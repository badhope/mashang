#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""重建 catalog.json，登记 content_packs 下的课程包，重算签名。

课程包会增删，catalog 里的哈希、讲数、体积都得现算。手改容易踩一个坑：
顶层自签名会把第一个包的哈希盖掉。流程固定：
1. 顶层 sha256 先用 64 个 0 占位；
2. 给每个包算真实哈希、讲数和体积；
3. 把顶层占位替换成空串后算签名，再回填。

用法：python tools/build_catalog.py
"""
import hashlib
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
CATALOG = ROOT / "catalog.json"
PACKS = ROOT / "content_packs"

PLACEHOLDER = "0" * 64
SHA_FIELD = re.compile(r'"sha256"\s*:\s*"[0-9a-f]{64}"')

# 包 id 对应展示名、简介、语言和层级
META = {
    "bonus-builtin": ("彩蛋课 · 内置函数巡礼", "Python 城市公共服务一次逛完：len/sorted/enumerate/zip/any/all…附随堂一问与判题实战。", "python", ""),
    "dsa-basics": ("增补包 · DSA 算法基础", "复杂度直觉 / 双指针 / 哈希表 / 栈与队列 / 递归记忆化：5 讲算法入门，每讲含判题。", "python", ""),
    "dsa-mastery": ("增补包 · DSA 算法进阶", "手写排序 / 二分查找 / 贪心找零 / 链表思维 / 最大子段和：5 讲算法进阶。", "python", ""),
    "js-basics": ("JavaScript · 基础篇", "10 讲：从 console.log 到函数与作用域，Rhino 运行时真跑真判题。", "javascript", "basics"),
    "js-core": ("JavaScript · 核心篇", "10 讲：数组方法 / JSON / 闭包 / 解构 / Map-Set / 正则，语言主体一次讲透。", "javascript", "core"),
    "js-advanced": ("JavaScript · 进阶篇", "10 讲：原型链 / this / 生成器 / Promise / 不可变数据 / 项目实战。", "javascript", "advanced"),
    "ts-basics": ("TypeScript · 基础篇", "10 讲：类型擦除 / 基础类型 / 数组元组 / 函数签名 / interface / 联合类型。", "typescript", "basics"),
    "ts-core": ("TypeScript · 核心篇", "10 讲：泛型入门 / 可选与 readonly / 索引签名 / 类型断言 / 从 JS 迁移。", "typescript", "core"),
    "ts-advanced": ("TypeScript · 进阶篇", "10 讲：类型守卫 / 工具类型 / 可辨识联合 / 声明文件 / tsconfig / 综合项目。", "typescript", "advanced"),
    "c-basics": ("C · 基础篇", "10 讲：main/printf 到字符串与作用域，设备端解释执行。", "c", "basics"),
    "c-core": ("C · 核心篇", "10 讲：指针 / 数组 / 结构体 / typedef / 二维数组 / 递归 / 位运算。", "c", "core"),
    "c-advanced": ("C · 进阶篇", "10 讲：字符串库实战 / 结构体嵌套 / 算法 / 复杂度 / 调试 / 综合项目。", "c", "advanced"),
    "lua-basics": ("Lua · 基础篇", "10 讲：print 到作用域，LuaJ 运行时真跑。", "lua", "basics"),
    "lua-core": ("Lua · 核心篇", "10 讲：多返回值 / 表库 / 闭包 / 递归 / 错误处理 / 迭代器。", "lua", "core"),
    "lua-advanced": ("Lua · 进阶篇", "10 讲：元表 / 继承 / 协程 / 模式匹配 / 函数式 / 项目实战。", "lua", "advanced"),
    "sql-basics": ("SQL · 基础篇", "10 讲：SELECT 到主键约束，SQLite 内存库真跑。", "sql", "basics"),
    "sql-core": ("SQL · 核心篇", "10 讲：多表 JOIN / 子查询 / CASE / 字符串与日期函数 / 视图。", "sql", "core"),
    "sql-advanced": ("SQL · 进阶篇", "10 讲：CTE / 窗口函数 / 事务 / 索引 / 触发器 / 查询优化 / 项目实战。", "sql", "advanced"),
    "js-expert": ("JavaScript · 专业篇", "8 讲：函数式 / 设计模式 / 模块化演进 / 性能 / 闭包陷阱 / 事件循环 / 测试 / 综合项目。", "javascript", "expert"),
    "ts-expert": ("TypeScript · 专业篇", "8 讲：高级泛型 / 类型级编程 / 模块增强 / 类型安全数据层 / 领域建模 / 迁移 / 工程化 / 项目。", "typescript", "expert"),
    "c-expert": ("C · 专业篇", "8 讲：内存布局 / 动态内存 / 文件 I/O / 预处理器 / 多文件链接 / 链表 / 栈队列 / 综合项目。", "c", "expert"),
    "lua-expert": ("Lua · 专业篇", "8 讲：表优化 / GC / 元表进阶 / 协程调度 / 序列化 / 游戏脚本模式 / 调试 / 综合项目。", "lua", "expert"),
    "sql-expert": ("SQL · 专业篇", "8 讲：执行计划 / 索引设计 / 事务隔离 / 并发锁 / JSON / 数据仓库 / 分析函数 / 留存漏斗。", "sql", "expert"),
    "sql-edge": ("SQL · 边界专题", "4 讲：NULL 分组 / 重复行去重 / 排序并列 / 事务与 SAVEPOINT，全部真实 sqlite3 判题。", "sql", "expert"),
}

# 包版本：内容更新但 id 不变时递增，客户端据此提示「更新到 vN」
VERSIONS = {}

# 只登记这些层级/前缀的包，避免把临时产物塞进目录
ALLOWED = set(META)


def build() -> tuple[str, list[str]]:
    packs = []
    missing = []
    for pid in sorted(ALLOWED):
        f = PACKS / f"{pid}.json"
        if not f.exists():
            missing.append(pid)
            continue
        data = json.loads(f.read_text(encoding="utf-8"))
        name, desc, lang, tier = META[pid]
        item = {
            "id": pid,
            "name": name,
            "version": VERSIONS.get(pid, 2),
            "description": desc,
            "sha256": hashlib.sha256(f.read_bytes()).hexdigest(),
            "kind": "course",
            "lang": lang,
            "lessons": len(data),
            "bytes": f.stat().st_size,
        }
        if tier:
            item["tier"] = tier
        packs.append(item)
    if missing:
        # C 课程三包由另一条内容线生成；缺包时只登记已存在的，并显式提示，
        # 避免整份 catalog 生成失败（发布前补跑一次即可补齐）。
        print(f"[warn] 以下课程包尚不存在，暂不登记：{missing}", file=sys.stderr)
        if not packs:
            raise SystemExit("没有任何可登记的课程包")

    catalog = {"updated_at": "2026-11-06", "sha256": PLACEHOLDER, "packs": packs}
    text = json.dumps(catalog, ensure_ascii=False, indent=2) + "\n"
    stripped = text.replace(f'"sha256": "{PLACEHOLDER}"', '"sha256": ""', 1)
    digest = hashlib.sha256(stripped.encode("utf-8")).hexdigest()
    return text.replace(f'"sha256": "{PLACEHOLDER}"', f'"sha256": "{digest}"', 1), missing


def main() -> int:
    text, missing = build()
    CATALOG.write_text(text, encoding="utf-8", newline="\n")
    data = json.loads(text)
    print(f"OK {CATALOG.name}: {len(data['packs'])} 包，自签名 {data['sha256'][:16]}…")
    for p in data["packs"]:
        print(f"  {p['id']:16} v{p['version']} {p['lessons']:2} 讲 {p['bytes'] // 1024:3}KB tier={p.get('tier', '-')}")
    if missing:
        print(f"[warn] 未登记（文件缺失）：{missing}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

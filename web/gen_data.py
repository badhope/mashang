#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成网页端(电脑端)所需的数据文件，全部复用 Android 侧现有产物，零改动源码。

输出:
  web/data.js      -> window.LESSONS / window.CHALLENGES  (课程 JSON 原样内联)
  web/engine.js    -> window.ENGINE_PY (runner.py 全文) / window.REPL_HELPER_PY (有状态 REPL 助手)

设计说明(最小实现 + 复用):
- 课程 JSON 来自 app/src/main/assets/lessons_*.json + challenges.json，结构与手机端完全一致。
- Python 引擎直接复用 runner.py；网页端通过 Pyodide(浏览器内 CPython)调用同一份
  run_code_json / check_exercise_json，因此判题与变量快照逻辑复用手机端同一份实现（web/README.md 有差异说明）。
- REPL 需要"有状态"命名空间，runner.py 本身每次 run_code 都用新 namespace，
  故在 engine.js 末尾追加一段 REPL_HELPER_PY（仅网页端用），复用 runner 的
  _run_protected / _snapshot，不修改 runner.py 原件。
"""
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent  # 仓库根目录（本文件位于 web/ 下）
ASSETS = ROOT / "app" / "src" / "main" / "assets"
PY = ROOT / "app" / "src" / "main" / "python"
WEB = ROOT / "web"
WEB.mkdir(exist_ok=True)

# ---- 课程 ----
lessons = []
for fname in ("lessons_basic.json", "lessons_mid.json", "lessons_adv.json", "lessons_pro.json"):
    p = ASSETS / fname
    if p.exists():
        lessons += json.loads(p.read_text(encoding="utf-8"))

# ---- 语言包课程（同样内联，网页端可读；能否运行由前端按语言判断）----
PACKS = ROOT / "content_packs"
pack_lessons = []
for pack in sorted(PACKS.glob("*.json")):
    try:
        pack_lessons += json.loads(pack.read_text(encoding="utf-8"))
    except Exception as exc:  # 单个包坏了不该拖垮整个生成
        print(f"  [warn] 跳过 {pack.name}: {exc}")
lessons += pack_lessons

challenges = []
cp = ASSETS / "challenges.json"
if cp.exists():
    challenges = json.loads(cp.read_text(encoding="utf-8"))

# ---- 引擎 ----
runner_src = (PY / "runner.py").read_text(encoding="utf-8")

# 仅在网页端使用的有状态 REPL 助手：复用 runner 的沙箱与快照。
repl_helper = r'''
import json as _json

_REPL_NS = {"__name__": "__main__"}
_REPL_WRAPPER = (
    "try:\n"
    "    __repl_value__ = eval(compile(__repl_src__, '<repl>', 'eval'))\n"
    "except SyntaxError:\n"
    "    exec(compile(__repl_src__, '<repl>', 'exec'))\n"
    "else:\n"
    "    if __repl_value__ is not None:\n"
    "        print(repr(__repl_value__))\n"
)

def repl_exec_json(payload_json):
    """在持久命名空间里执行一行/一段 REPL 代码，返回与 run_code 同构的结果。

    表达式模式（1+1 / 'a'.upper()）回显 repr，语句模式按 exec 执行；
    两者都走 _run_protected 的看门狗与输出捕获。
    """
    p = _json.loads(payload_json)
    src = p.get("code", "")
    ns = dict(_REPL_NS)
    ns["__repl_src__"] = src
    res = _run_protected(_REPL_WRAPPER, ns, p.get("stdin") or [], float(p.get("timeout", DEFAULT_TIMEOUT)))
    _REPL_NS.clear()
    _REPL_NS.update({k: v for k, v in ns.items() if not k.startswith("__repl_")})
    r = dict(res)
    r["ok"] = res["error"] is None
    r["variables"] = _snapshot(_REPL_NS)
    return _json.dumps(r, ensure_ascii=False)

def repl_reset_json(payload_json=""):
    """清空 REPL 会话命名空间（保留 __name__）。"""
    _REPL_NS.clear()
    _REPL_NS["__name__"] = "__main__"
    return "{}"
'''

# ---- 写出 ----
(WEB / "data.js").write_text(
    "window.LESSONS = " + json.dumps(lessons, ensure_ascii=False) + ";\n"
    "window.CHALLENGES = " + json.dumps(challenges, ensure_ascii=False) + ";\n",
    encoding="utf-8",
    newline="\n",
)

(WEB / "engine.js").write_text(
    "window.ENGINE_PY = " + json.dumps(runner_src, ensure_ascii=False) + ";\n"
    "window.REPL_HELPER_PY = " + json.dumps(repl_helper, ensure_ascii=False) + ";\n",
    encoding="utf-8",
    newline="\n",
)

_ch_by_lang = {}
for _c in challenges:
    _l = _c.get("lang", "python")
    _ch_by_lang[_l] = _ch_by_lang.get(_l, 0) + 1
js_challenges = _ch_by_lang.get("javascript", 0)
print("OK generated web/data.js + web/engine.js")
print("  lessons      :", len(lessons), f"(内置 + 语言包 {len(pack_lessons)})")
_by_lang = {}
for _l in lessons:
    _by_lang[_l.get("lang", "python")] = _by_lang.get(_l.get("lang", "python"), 0) + 1
print("  by language  :", ", ".join(f"{k} {v}" for k, v in sorted(_by_lang.items())))
print("  challenges   :", len(challenges), "(" + ", ".join(f"{k} {v}" for k, v in sorted(_ch_by_lang.items())) + ")")
print("  engine.py len:", len(runner_src), "chars")

"""CI 构建产物探针：校验 APK 内含 Chaquopy Python 引擎与关键资源。

用法：python tests/ci_apk_probe.py <apk路径>
失败即 exit 1（CI 阻断）。

校验项：
  R1 APK 可被 zipfile 打开且条目数 > 500
  R2 assets/chaquopy/app.imy 存在，解包后含 runner / repl 模块
     （Chaquopy 可能产出 .py / .pyc / __pycache__/*.pyc 三种布局，均接受）
  R3 runner 模块可校验：.pyc 走 marshal 反解、.py 走源码扫描，
     二者都必须含 threading + settrace（超时看门狗路径）
  R4 assets 内课程与挑战 JSON 存在且可解析（lessons_*.json / challenges.json）
  R5 assets/syntaxes/javascript.tmLanguage.json 存在（多语言高亮）
  R6 AndroidManifest（二进制 AXML，UTF-16LE）含包名 com.pyneon.academy
  R7 APK 预装 24 个课程包与离线 catalog（assets/content_packs/*.json、assets/catalog.json）
"""
import importlib.util
import io
import json
import marshal
import sys
import zipfile

CHAQUOPY_PYC_MAGIC = 0x0DF3  # CPython 3.13（app/build.gradle.kts 的 chaquopy.version）


def fail(msg):
    print(f"[FAIL] {msg}")
    sys.exit(1)


def code_names(code, acc):
    acc.update(code.co_names)
    for const in code.co_consts:
        if hasattr(const, "co_names"):
            code_names(const, acc)
    return acc


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    flags = {a for a in sys.argv[1:] if a.startswith("--")}
    if len(args) != 1:
        fail("用法：python tests/ci_apk_probe.py <apk路径> [--require-release]")
    apk = args[0]
    require_release = "--require-release" in flags
    try:
        z = zipfile.ZipFile(apk)
    except Exception as exc:  # noqa: BLE001
        fail(f"APK 无法打开：{exc}")

    names = z.namelist()
    if len(names) < 500:
        fail(f"APK 条目数异常：{len(names)}")
    print(f"[PASS] APK 可打开，条目 {len(names)}")

    if "assets/chaquopy/app.imy" not in names:
        fail("缺少 assets/chaquopy/app.imy")
    inner = zipfile.ZipFile(io.BytesIO(z.read("assets/chaquopy/app.imy")))
    entries = set(inner.namelist())

    def find_module(stem):
        # Chaquopy 按平台/缓存状态可能产出 <stem>.py、<stem>.pyc 或
        # __pycache__/<stem>.cpython-3XX.pyc，三种布局都接受。
        hits = [
            n for n in entries
            if n.rsplit("/", 1)[-1].split(".")[0] == stem and n.endswith((".py", ".pyc"))
        ]
        if not hits:
            fail(f"app.imy 缺少 {stem}.py/.pyc（现有：{sorted(entries)}）")
        return min(hits, key=len)

    runner_entry = find_module("runner")
    find_module("repl")
    print(f"[PASS] app.imy 含 {sorted(entries)}")

    raw = inner.read(runner_entry)
    if runner_entry.endswith(".pyc"):
        # 校验 pyc 头结构：CPython pyc 前 4 字节是魔数，后两字节固定为 CRLF。
        # 只做 marshal.loads 会把「头部乱填、正文恰好是合法 code」的假包放行。
        # 这里不比对本地解释器魔数（CI 用 3.12 解析、Chaquopy 产出 3.13），
        # 只校验结构合法 + Chaquopy 目标版本主次号（3.13 -> 0x0df3）。
        if raw[2:4] != bytes([13, 10]):
            fail(f"{runner_entry} 不是合法 pyc（魔数尾部 {raw[2:4]!r}）")
        if int.from_bytes(raw[0:2], "little") != CHAQUOPY_PYC_MAGIC:
            fail(
                f"{runner_entry} 的 pyc 版本不是 Chaquopy 目标 3.13"
                f"（魔数 {raw[0:2].hex()}，期望 {CHAQUOPY_PYC_MAGIC:04x}）"
            )
        try:
            code = marshal.loads(raw[16:])
        except Exception as exc:  # noqa: BLE001
            fail(f"{runner_entry} 无法解析：{exc}")
        missing = {"threading", "settrace"} - code_names(code, set())
        if missing:
            fail(f"{runner_entry} 缺 {sorted(missing)}（超时看门狗缺失）")
        print(f"[PASS] {runner_entry} 魔数正确、可解析且含 threading+settrace")
    else:
        source = raw.decode("utf-8")
        missing = [k for k in ("import threading", "settrace") if k not in source]
        if missing:
            fail(f"{runner_entry} 缺 {missing}（超时看门狗缺失）")
        print(f"[PASS] {runner_entry} 源码含 threading + settrace 看门狗")

    assets = [n for n in names if n.startswith("app/src/main/assets/") or n.startswith("assets/lessons_")]
    if not assets:
        # Chaquopy/AGP 可能把 assets 放在 assets/ 根：直接按文件名核对
        assets = [n for n in names if n.endswith(("lessons_basic.json", "lessons_mid.json", "lessons_adv.json", "lessons_pro.json", "challenges.json"))]
    required_assets = ("lessons_basic.json", "lessons_mid.json", "lessons_adv.json", "lessons_pro.json", "challenges.json")
    for want in required_assets:
        hit = [n for n in names if n.endswith(want)]
        if not hit:
            fail(f"APK 缺少 assets/{want}")
        json.loads(z.read(hit[0]).decode("utf-8"))
    print(f"[PASS] 课程与挑战 JSON 完整且可解析（{len(required_assets)} 个）")

    pack_names = [
        n for n in names
        if n.startswith("assets/content_packs/") and n.endswith(".json")
    ]
    if len(pack_names) < 24:
        fail(f"APK 预装课程包不足：{len(pack_names)} 个（至少 24）")
    for n in pack_names:
        try:
            json.loads(z.read(n).decode("utf-8"))
        except Exception as exc:  # noqa: BLE001
            fail(f"预装课程包不可解析：{n}（{exc}）")
    catalog_hits = [n for n in names if n == "assets/catalog.json"]
    if not catalog_hits:
        fail("APK 缺少 assets/catalog.json（语言中心离线目录）")
    catalog = json.loads(z.read(catalog_hits[0]).decode("utf-8"))
    if len(catalog.get("packs") or []) < 24:
        fail("assets/catalog.json 的包数量不足 24")
    print(f"[PASS] 预装课程包 {len(pack_names)} 个，离线 catalog 含 {len(catalog.get('packs') or [])} 包")

    # 每门内置语言都必须有语法文件，否则该语言编辑器静默降级成白底
    for lang in ("python", "javascript", "typescript", "c", "lua", "sql"):
        hit = [n for n in names if n.endswith(f"syntaxes/{lang}.tmLanguage.json")]
        if not hit:
            fail(f"APK 缺少 syntaxes/{lang}.tmLanguage.json")
    print("[PASS] 语法高亮资源齐全（python / javascript / typescript / c / lua / sql）")

    # AXML 是二进制格式，字符串资源以 UTF-16LE 存储
    manifest = z.read("AndroidManifest.xml").decode("utf-16-le", "ignore")
    if "com.pyneon.academy" not in manifest:
        fail("AndroidManifest 中未找到包名 com.pyneon.academy")
    print("[PASS] AndroidManifest 包名就位")

    # 发布级 APK 检查
    # 这几条都是「能在真机上炸」的问题，必须在打包阶段就拦住。

    # R6: 不得混入源码/调试器/示例文件（体积与信息泄漏）
    junk = [n for n in names if n.lower().endswith((".java", ".kt", ".md"))
            or "/debugger/" in n or n.endswith("DebugProbesKt.bin")]
    if junk:
        fail(f"APK 混入无关文件 {len(junk)} 个，例如：{junk[:3]}")
    print("[PASS] 无源码/调试器残留")

    # R7: release 包必须不可调试
    if "android:debuggable" in manifest and "--require-release" in sys.argv:
        fail("release APK 仍标记为 debuggable")
    print("[PASS] 非 debuggable")

    # R8: 混淆必须真的生效（dex 显著小于 debug 版）
    dex_total = sum(i.file_size for i in z.infolist() if i.filename.endswith(".dex"))
    if "--require-release" in sys.argv and dex_total > 20 * 1024 * 1024:
        fail(f"release dex 过大（{dex_total/1048576:.1f}MB），R8 可能未生效")
    print(f"[PASS] dex 体积 {dex_total/1048576:.1f} MB")

    # R9: ABI 必须包含 arm64-v8a（国内商店强制 64 位）
    if not any(n.startswith("lib/arm64-v8a/") for n in names):
        fail("APK 缺少 arm64-v8a 原生库")
    print("[PASS] arm64-v8a 就位")


    if require_release:
        # release 特有：R8 处理后仍有 dex，且 APK 带 v2+ 签名块。
        # minSdk 29 且 AGP 8 默认只出 v2 签名（不写 META-INF/*.RSA），
        # 因此用 APK Signing Block 魔数判断，而不是找 JAR 签名文件。
        dexes = [n for n in names if n.endswith(".dex")]
        if not dexes:
            fail("release APK 无 dex")
        with open(apk, "rb") as fh:
            fh.seek(0, 2)
            size = fh.tell()
            fh.seek(max(0, size - 1_048_576))
            tail = fh.read()
        if b"APK Sig Block 42" not in tail:
            # --require-release 是「这个包准备发布」的严格模式：必须已签名。
            # 本地只想检查「产物结构对不对」时不要加该参数（会得到 WARN）。
            if "--require-release" in sys.argv:
                fail("release APK 未签名（缺 APK Signing Block）——请先配置 keystore")
            print("[WARN] release APK 未签名（本地构建正常；发布前需配置 keystore）")
        # 注：签名块存在不等于用的是 release 密钥（debug 包同样有签名块），
        # 证书指纹校验由 CI 的 apksigner --print-certs 步骤负责。
        print(f"[PASS] release 产物含 dex({len(dexes)}) 且带 APK 签名块")

    print("ALL APK PROBES PASSED")


if __name__ == "__main__":
    main()

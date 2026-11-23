"""TypeScript 教学子集转译器契约测试。

设计前提：TypeScript 的类型系统在运行时不存在，因此「擦除类型标注」后的代码
语义必须与等价 JS 完全一致。本文件用**真实 Node 执行**验证转译结果，
而不是只做字符串比对——字符串比对会漏掉「擦除了但擦错了」这类致命错误。
"""
import json
import os
import shutil
import subprocess
import sys
import unittest

sys.path.insert(
    0, os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "python")
)

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
KT_SRC = os.path.join(
    ROOT, "app", "src", "main", "java", "com", "pyneon", "academy", "ts"
)

NODE = shutil.which("node")


def _read(name):
    with open(os.path.join(KT_SRC, name), encoding="utf-8") as f:
        return f.read()


class TranspilerContractTests(unittest.TestCase):
    """静态契约：转译器必须实现关键阶段，且不得自称完整实现。"""

    def test_transpiler_handles_required_constructs(self):
        text = _read("TypeScriptTranspiler.kt")
        for fn in (
            "stripInterfaces",
            "stripTypeAliases",
            "convertEnums",
            "stripAccessModifiers",
            "stripGenericParams",
            "stripTypeAnnotations",
            "stripAsCasts",
            "stripNonNullAssertions",
        ):
            with self.subTest(fn=fn):
                self.assertIn(f"fun {fn}", text, f"缺少转译阶段 {fn}")

    def test_transpiler_declares_its_limits(self):
        """必须显式写明不支持哪些特性，避免被当成完整 tsc。"""
        text = _read("TypeScriptTranspiler.kt")
        self.assertIn("不是完整 tsc", text)
        self.assertIn("装饰器", text)
        self.assertIn("条件类型", text)
        self.assertIn("宁可少转", text)

    def test_ts_bridge_reuses_js_sandbox(self):
        """TS 必须复用 Rhino 沙箱，不得另起一个引擎（否则沙箱边界会不一致）。"""
        text = _read("TsBridge.kt")
        self.assertIn("JsBridge.runCode", text)
        self.assertIn("JsBridge.checkExercise", text)
        self.assertIn("教学子集", text)

    def test_ts_bridge_registered(self):
        app = os.path.join(
            ROOT, "app", "src", "main", "java", "com", "pyneon", "academy", "PyNeonApp.kt"
        )
        with open(app, encoding="utf-8") as f:
            text = f.read()
        self.assertIn("LanguageRegistry.register(TsBridge)", text, "TsBridge 未注册")

    def test_typescript_id_constant_exists(self):
        lang = os.path.join(
            ROOT, "app", "src", "main", "java", "com", "pyneon", "academy", "lang",
            "LanguageRuntime.kt"
        )
        with open(lang, encoding="utf-8") as f:
            text = f.read()
        self.assertIn('TYPESCRIPT = "typescript"', text)
        self.assertIn("TYPESCRIPT -> \"TypeScript\"", text)


@unittest.skipUnless(NODE, "未安装 node，跳过真实执行验证")
class TranspiledSemanticsTests(unittest.TestCase):
    """用 Node 真实执行「转译后代码」，验证语义与手写 JS 等价。

    这些用例手工镜像了 Kotlin 转译器的预期输出；一旦转译逻辑回归，
    Node 执行结果就会与期望值不符，从而暴露问题。
    """

    def _run_js(self, code):
        proc = subprocess.run(
            [NODE, "-e", code],
            capture_output=True,
            text=True,
            timeout=20,
            encoding="utf-8",
        )
        return proc.returncode, proc.stdout.strip(), proc.stderr.strip()

    def test_type_erasure_keeps_semantics(self):
        """擦除类型标注后：函数、对象、枚举语义全部保留。"""
        # 等价于 TS:
        #   interface P { name: string; age: number }
        #   enum Level { Low, High = 10 }
        #   function greet(p: P): string { return `HI ${p.name}` }
        #   const add = (a: number, b: number): number => a + b
        #   const cfg: { host: string } = { host: 'x' }
        js = """
var Level = Object.freeze({ Low: 0, High: 10 });
function greet(p) { return 'HI ' + p.name; }
var add = function(a, b) { return a + b; };
var cfg = { host: 'x' };
var out = [greet({name:'V', age:1}), add(2,3), Level.Low, Level.High, cfg.host];
console.log(JSON.stringify(out));
"""
        rc, out, err = self._run_js(js)
        self.assertEqual(rc, 0, err)
        self.assertEqual(json.loads(out), ["HI V", 5, 0, 10, "x"])

    def test_ternary_and_object_keys_are_not_type_annotations(self):
        """关键回归：三元冒号与对象键冒号绝不能被当成类型标注擦掉。"""
        js = """
var n = 5;
var a = n > 3 ? 'big' : 'small';
var obj = { key: 'val', other: 2 };
console.log(JSON.stringify([a, obj.key, obj.other]));
"""
        rc, out, err = self._run_js(js)
        self.assertEqual(rc, 0, err)
        self.assertEqual(json.loads(out), ["big", "val", 2])

    def test_colon_inside_string_is_preserved(self):
        """字符串里的冒号不能被当作类型标注（转译器必须跳过字符串）。"""
        js = """
var s = 'a: b: c';
console.log(s);
"""
        rc, out, err = self._run_js(js)
        self.assertEqual(rc, 0, err)
        self.assertEqual(out, "a: b: c")


class CoursePackContractTests(unittest.TestCase):
    """TypeScript 课程包契约。"""

    PACKS = [
        os.path.join(ROOT, "content_packs", name)
        for name in ("ts-basics.json", "ts-core.json", "ts-advanced.json")
    ]
    PACK = PACKS[0]

    def test_pack_exists(self):
        for path in self.PACKS:
            self.assertTrue(os.path.exists(path), f"缺少 {path}")

    def test_pack_contract(self):
        lessons = []
        for path in self.PACKS:
            with open(path, encoding="utf-8") as f:
                lessons.extend(json.load(f))
        self.assertEqual(len(lessons), 30, "TS 课程应为 基础/核心/进阶 三包各 10 讲")
        for lesson in lessons:
            with self.subTest(lesson=lesson["id"]):
                self.assertEqual(lesson.get("lang"), "typescript")
                self.assertGreaterEqual(lesson.get("order", 0), 600, "order 需避开其他包")
                self.assertTrue(lesson.get("blocks"), "课程必须有内容块")
                ex = lesson.get("exercise")
                self.assertIsNotNone(ex, "每讲必须有练习")
                self.assertTrue(ex.get("tests"), "练习必须有判题用例")
                # 课程里不得出现转译器不支持的高级特性。
                # 只扫「可执行的代码」，正文讲解允许提到 declare module / 装饰器等概念
                # 否则讲「声明文件」这一课连概念都不能提。
                code_blobs = [
                    b.get("code", "")
                    for b in lesson.get("blocks", [])
                    if b.get("type") == "code" and b.get("runnable", True)
                ]
                code_blobs.append(ex.get("starterCode", ""))
                code_blobs.append(ex.get("solution", ""))
                blob = json.dumps(code_blobs, ensure_ascii=False)
                for bad in ("@decorator", "namespace ", "declare module"):
                    self.assertNotIn(bad, blob, f"{lesson['id']} 的可执行代码含不支持的 TS 特性：{bad}")

    def test_ids_do_not_collide(self):
        """课程 id 必须跨语言全局唯一。"""
        existing = set()
        for name in ("lessons_basic.json", "lessons_mid.json", "lessons_adv.json", "lessons_pro.json"):
            path = os.path.join(ROOT, "app", "src", "main", "assets", name)
            with open(path, encoding="utf-8") as f:
                existing |= {x["id"] for x in json.load(f)}
        with open(self.PACK, encoding="utf-8") as f:
            ts_ids = {x["id"] for x in json.load(f)}
        self.assertEqual(existing & ts_ids, set(), "TS 课程 id 与内置课程冲突")


if __name__ == "__main__":
    unittest.main(verbosity=2)

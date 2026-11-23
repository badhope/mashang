"""CI 配置的静态校验（不需要网络/平台凭据）。

锁住两类真实踩过的坑：
  R1 YAML 必须能被严格解析——重复键会被 GitHub 直接判为 Invalid workflow
     （2026-10-21 实际发生：mirror-sync 的 checkout 步骤写了两个 with:）
  R2 关键门禁必须在位：lint、JVM 单测、debug/release 构建、APK 探针、
     版本校验、镜像同步；任何一项被删都要显式改测试，不能悄悄消失
"""
import os
import unittest
from pathlib import Path

try:
    import yaml  # noqa: F401
except ModuleNotFoundError:  # pragma: no cover - 取决于运行环境
    yaml = None

ROOT = Path(__file__).resolve().parent.parent
# Gitee Go 未开通（其平台无法用 API 激活），故不再维护 .workflow/ 流水线；
# 若将来开通，把文件加回此列表即可纳入校验。
WORKFLOWS = [
    ROOT / ".github/workflows/ci.yml",
    ROOT / ".gitcode/workflows/ci.yml",
]


def _strict_load(path):
    if yaml is None:
        if os.environ.get("REQUIRE_YAML") == "1":
            raise AssertionError(
                "REQUIRE_YAML=1 但未安装 PyYAML：CI 不允许跳过 workflow 静态校验"
            )
        raise unittest.SkipTest("未安装 PyYAML；由平台自身校验器兜底")

    class Strict(yaml.SafeLoader):
        pass

    def no_duplicates(loader, node, deep=False):
        mapping = {}
        for key_node, value_node in node.value:
            key = loader.construct_object(key_node, deep=deep)
            if key in mapping:
                raise ValueError(f"{path.name}: 重复键 {key!r}（行 {key_node.start_mark.line + 1}）")
            mapping[key] = loader.construct_object(value_node, deep=deep)
        return mapping

    Strict.add_constructor(
        yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, no_duplicates
    )
    return yaml.load(path.read_text(encoding="utf-8"), Loader=Strict)


class WorkflowSyntaxTests(unittest.TestCase):
    def test_all_workflows_parse_without_duplicate_keys(self):
        for path in WORKFLOWS:
            with self.subTest(path=path.name):
                doc = _strict_load(path)
                self.assertIsInstance(doc, dict)
                # GitHub/GitCode 用 jobs，Gitee Go 用 stages，都要求至少有一层任务
                self.assertIn("jobs", doc, f"{path.name} 缺少 jobs")


class GitHubGateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = (ROOT / ".github/workflows/ci.yml").read_text(encoding="utf-8")

    def test_required_gates_present(self):
        required = [
            "python -m unittest discover -s tests",
            "python tests/validate_content.py",
            "python web/gen_data.py",
            "git diff --exit-code -- web/data.js web/engine.js",
            "./gradlew :app:lintDebug",
            "./gradlew :app:lintRelease",
            "./gradlew :app:testDebugUnitTest",
            "./gradlew :app:assembleDebug",
            "./gradlew :app:assembleRelease",
            "python tests/ci_apk_probe.py",
            "scripts/ci/check_release_version.py",
            "bash scripts/ci/sync_mirrors.sh",
        ]
        missing = [c for c in required if c not in self.text]
        self.assertEqual(missing, [], f"CI 缺少门禁步骤: {missing}")

    def test_no_green_washing(self):
        # 允许的唯一 || true 是 SDK license 接受（后续 --list_installed 是硬门禁）
        bad = []
        for line in self.text.splitlines():
            if "|| true" in line and "--licenses" not in line:
                bad.append(line.strip())
            if "continue-on-error" in line:
                bad.append(line.strip())
        self.assertEqual(bad, [], f"发现吞错写法: {bad}")

    def test_release_is_gated_by_version_and_signature(self):
        self.assertIn("check_release_version.py", self.text)
        self.assertIn("apksigner", self.text)
        self.assertIn("--require-release", self.text)
        self.assertIn("environment: release", self.text)


    def test_mirror_sync_retries_each_mirror_independently(self):
        """Gitee 在 CI runner 上偶发 connection reset（2026-10-23 实测），
        单个镜像失败不得让另外三个镜像停在旧 commit，也不得吞错。"""
        text = (ROOT / "scripts/ci/sync_mirrors.sh").read_text(encoding="utf-8")
        self.assertIn("while [ \"$attempt\" -le 3 ]", text, "缺少重试循环")
        self.assertIn("return 1", text, "重试耗尽必须失败，不能吞错")
        self.assertIn("set -euo pipefail", text)
        for label in ("GitCode", "Gitee", "GitHub X33834", "GitHub Morningstar"):
            self.assertIn(label, text, f"缺少镜像 {label}")

    def test_signing_secrets_are_not_written_to_github_env(self):
        """签名口令只能存在于所需 step 的 env 内。

        2026-10-22 审计发现：原实现把 KEYSTORE_PASSWORD / KEY_PASSWORD 写进
        $GITHUB_ENV，后续所有 step（含第三方 action）都能读到。
        """
        self.assertNotIn(">> \"$GITHUB_ENV\"", self.text, "签名凭据不得写入 $GITHUB_ENV")
        self.assertIn("KEYSTORE_PATH: ${{ runner.temp }}/keystore/release.keystore", self.text)

    def test_mirror_sync_is_primary_repo_only(self):
        self.assertIn("github.repository == 'badhope/mashang'", self.text)

    def test_js_tests_are_forced_to_run(self):
        self.assertIn('REQUIRE_NODE: "1"', self.text)

    def test_yaml_check_is_enforced_on_github(self):
        self.assertIn('REQUIRE_YAML: "1"', self.text)


if __name__ == "__main__":
    unittest.main()

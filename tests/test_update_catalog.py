"""R1/R2 回归测试：catalog 自签名剥离语义 + 包哈希一致性。

背景（2026-10-19 全量审计发现）：
- tools/update_catalog.py 的 strip_self_sha 用 re.sub 替换所有 sha256 字段，
  而 Kotlin 端 ContentCenter.stripSelfSha 只 replaceFirst 顶层字段。
  两者不一致会导致：运行工具后 packs[].sha256 被清空，内容中心拒绝下载。
- catalog.json 中 dsa-basics / dsa-mastery 的 sha256 与实际文件不符
  （commit 5004113 改了包内容但未同步目录），应用内下载必被完整性校验拦截。

本文件是上述两个缺陷的 Behavior Lock：先红后绿，修复后必须全绿。
"""
import hashlib
import importlib.util
import json
import os
import re
import unittest

ROOT = os.path.join(os.path.dirname(__file__), "..")
CATALOG_PATH = os.path.join(ROOT, "catalog.json")
PACKS_DIR = os.path.join(ROOT, "content_packs")

# ---- 载入 tools/update_catalog.py（非包目录，按路径导入）----
_spec = importlib.util.spec_from_file_location(
    "update_catalog", os.path.join(ROOT, "tools", "update_catalog.py")
)
update_catalog = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(update_catalog)


def read_catalog_text():
    with open(CATALOG_PATH, encoding="utf-8") as f:
        return f.read()


def sha256_text(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


class StripSelfShaTests(unittest.TestCase):
    """R1: strip_self_sha MUST 只剥离顶层 sha256，保留 packs[].sha256。"""

    def test_only_top_level_sha_is_stripped(self):
        text = read_catalog_text()
        stripped = update_catalog.strip_self_sha(text)
        # 顶层字段必须被置空
        self.assertTrue(re.search(r'"sha256"\s*:\s*""', stripped))
        # packs 内层的 sha256 必须保持 64 位 hex 原值（核心断言：之前那版会清空它们）。
        # 包数量随语言包扩充变化，按 catalog 实际 packs 数动态断言。
        import json

        with open(CATALOG_PATH, encoding="utf-8") as f:
            pack_count = len(json.load(f)["packs"])
        self.assertGreater(pack_count, 0)
        pack_hashes = re.findall(r'"sha256"\s*:\s*"([0-9a-f]{64})"', stripped)
        self.assertEqual(
            len(pack_hashes),
            pack_count,
            "剥离后应保留 %d 个 packs[].sha256 原值，实际剩 %d 个"
            % (pack_count, len(pack_hashes)),
        )

    def test_kotlin_semantics_equivalence(self):
        """与 Kotlin ContentCenter.stripSelfSha（replaceFirst）行为一致。"""
        text = read_catalog_text()
        kotlin_equivalent = re.sub(
            r'"sha256"\s*:\s*"[0-9a-f]{64}"', '"sha256": ""', text, count=1
        )
        self.assertEqual(update_catalog.strip_self_sha(text), kotlin_equivalent)


class CatalogIntegrityTests(unittest.TestCase):
    """R2: catalog.json 每个 pack 的 sha256 MUST 与文件实际哈希一致。"""

    def test_catalog_pack_hashes_match_files(self):
        with open(CATALOG_PATH, encoding="utf-8") as f:
            catalog = json.load(f)
        for pack in catalog["packs"]:
            pack_path = os.path.join(PACKS_DIR, "%s.json" % pack["id"])
            self.assertTrue(
                os.path.exists(pack_path), "缺少课程包文件: %s" % pack_path
            )
            # 必须按原始字节算：App 下载后用收到的文本（HTTP body 原样）做 sha256，
            # 用 text 模式读取会把 CRLF 归一成 LF，从而掩盖「仓库文件是 CRLF、
            # catalog 按 LF 签名」这类真实故障（2026-10-21 js-basics 实际踩过）。
            with open(pack_path, "rb") as f:
                actual = hashlib.sha256(f.read()).hexdigest()
            self.assertEqual(
                pack["sha256"],
                actual,
                "pack %s 的 catalog 哈希与文件不符（内容中心会拒绝下载）" % pack["id"],
            )
            # 附带说明：包文件必须用 LF，否则不同平台的 git 行为会让哈希漂移
            with open(pack_path, "rb") as f:
                self.assertNotIn(
                    (chr(13) + chr(10)).encode("ascii"), f.read(),


                    "pack %s 含 CRLF 换行，会导致下载校验失败（统一用 LF）" % pack["id"],
                )

    def test_catalog_self_signature_valid(self):
        """顶层自签名 MUST 按「剥离顶层 sha256 后的原文」计算且匹配。"""
        text = read_catalog_text()
        catalog = json.loads(text)
        stripped = re.sub(
            r'"sha256"\s*:\s*"[0-9a-f]{64}"', '"sha256": ""', text, count=1
        )
        self.assertEqual(catalog["sha256"], sha256_text(stripped))


if __name__ == "__main__":
    unittest.main()

#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""生成软著申请的源程序册。

按中国版权保护中心的受理要求：交前 30 页和后 30 页，共 60 页；不足 60 页就全交。
每页至少 50 行，页眉写软件名称和版本号。

默认写到 dist/softcopy/：
源程序.html，浏览器打开后打印成 PDF
源程序.txt，分页符分隔的纯文本
源程序清单.csv，文件名、行数和所在页区间

用法：

    python tools/softcopy_source.py
    python tools/softcopy_source.py --name "码上多语言编程学习软件" --version "V1.0" --out dist/softcopy

只收项目自己的源码，Gradle 依赖和 sora-editor 这些第三方不进册。
文件按路径字典序排，保证每次生成的前后 30 页都一样。
"""

from __future__ import annotations

import argparse
import csv
import html
import os
import sys
from pathlib import Path
from typing import Iterable, List, NamedTuple, Tuple

ROOT = Path(__file__).resolve().parent.parent

LINES_PER_PAGE = 50
HEAD_PAGES = 30
TAIL_PAGES = 30

DEFAULT_NAME = "码上多语言编程学习软件"
DEFAULT_VERSION = "V1.0"

# 需要收录的源文件根目录，顺序固定：主程序、引擎、数据
SOURCE_ROOTS: Tuple[str, ...] = (
    "app/src/main/java",
    "app/src/main/python",
)

# 收录的扩展名
SOURCE_EXTS = (".kt", ".py")

# 明确排除：构建产物 / 第三方 / 测试桩
EXCLUDE_PARTS = ("/build/", "/test/", "/androidTest/", "/__pycache__/")


class SourceFile(NamedTuple):
    rel: str
    abs: Path
    lines: List[str]


def collect_sources() -> List[SourceFile]:
    """按包名字典序收集原创源文件，保证多次运行顺序完全一致。"""
    found: List[SourceFile] = []
    for root in SOURCE_ROOTS:
        base = ROOT / root
        if not base.exists():
            print(f"[WARN] 目录不存在，跳过：{root}", file=sys.stderr)
            continue
        for path in base.rglob("*"):
            if not path.is_file() or path.suffix not in SOURCE_EXTS:
                continue
            rel = path.relative_to(ROOT).as_posix()
            if any(p in f"/{rel}" for p in EXCLUDE_PARTS):
                continue
            # 行尾统一为 \n，去掉 BOM；保留空行（官方要求"连续"源程序）
            text = path.read_text(encoding="utf-8", errors="replace").lstrip("\ufeff")
            lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n")
            if lines and lines[-1] == "":
                lines.pop()
            found.append(SourceFile(rel, path, lines))

    found.sort(key=lambda f: f.rel)
    return found


def paginate(files: Iterable[SourceFile], per_page: int) -> List[Tuple[str, int, List[str]]]:
    """
    把文件流切成固定行数的页。返回 [(文件路径, 该页起始行号(1-based), 行列表)]。
    单个文件跨页时，后续页的"文件路径"仍标注该文件，起始行号连续。
    """
    pages: List[Tuple[str, int, List[str]]] = []
    cur_file = ""
    cur_start = 1
    buf: List[str] = []

    for f in files:
        for i, line in enumerate(f.lines, start=1):
            if not buf:
                cur_file = f.rel
                cur_start = i
            buf.append(line)
            if len(buf) >= per_page:
                pages.append((cur_file, cur_start, buf))
                buf = []
    if buf:
        pages.append((cur_file, cur_start, buf))
    return pages


def select_pages(pages: List[Tuple[str, int, List[str]]]) -> Tuple[List[Tuple[str, int, List[str]]], str]:
    """前 30 页 + 后 30 页；总页数不足 60 页则全部提交。"""
    total = len(pages)
    need = HEAD_PAGES + TAIL_PAGES
    if total <= need:
        return pages, f"总页数 {total} ≤ {need}，全部提交"

    head = pages[:HEAD_PAGES]
    tail = pages[-TAIL_PAGES:]
    note = f"总页数 {total} ＞ {need}，提交前 {HEAD_PAGES} 页 + 后 {TAIL_PAGES} 页"
    return head + tail, note


def build_html(
    name: str,
    version: str,
    pages: List[Tuple[str, int, List[str]]],
    total_pages: int,
) -> str:
    """生成可打印为 A4 PDF 的 HTML。每页 50 行，页眉 = 软件名 + 版本号。"""
    header = f"{name} {version}"
    parts: List[str] = []
    for idx, (rel, start, lines) in enumerate(pages, start=1):
        # 末页（或最后一页）不足 50 行时用空行补齐，保证每页版式统一为 50 行
        pad = LINES_PER_PAGE - len(lines)
        body_lines = [html.escape(l).replace(" ", "&nbsp;") for l in lines]
        body_lines += ["&nbsp;"] * pad
        body = "\n".join(body_lines)
        parts.append(
            f'<section class="page">\n'
            f'  <div class="hd"><span>{html.escape(header)}</span>'
            f'<span class="pg">{idx} / {total_pages}</span></div>\n'
            f'  <div class="src">{body}</div>\n'
            f'</section>'
        )
    pages_html = "\n".join(parts)
    return f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>{html.escape(header)} 源程序</title>
<style>
  @page {{ size: A4; margin: 14mm 12mm; }}
  * {{ box-sizing: border-box; }}
  body {{ margin: 0; background: #eee; font-family: "Consolas", "Courier New", monospace; }}
  .page {{
    width: 210mm; min-height: 297mm; margin: 8mm auto; padding: 14mm 12mm;
    background: #fff; position: relative; page-break-after: always;
  }}
  .page:last-child {{ page-break-after: auto; }}
  .hd {{
    display: flex; justify-content: space-between; align-items: baseline;
    border-bottom: 1px solid #333; padding-bottom: 3px; margin-bottom: 6px;
    font-size: 10pt; font-family: "Microsoft YaHei", sans-serif;
  }}
  .pg {{ font-size: 9pt; color: #555; }}
  .src {{ white-space: pre; font-size: 7.6pt; line-height: 1.28; }}
  @media print {{
    body {{ background: #fff; }}
    .page {{ margin: 0; width: auto; min-height: auto; height: 267mm; page-break-after: always; }}
  }}
</style>
</head>
<body>
{pages_html}
</body>
</html>
"""


def build_txt(
    name: str,
    version: str,
    pages: List[Tuple[str, int, List[str]]],
) -> str:
    header = f"{name} {version}"
    out: List[str] = []
    for idx, (rel, start, lines) in enumerate(pages, start=1):
        out.append(f"{header} 第 {idx} 页 [{rel} 自第 {start} 行起]")
        out.extend(lines)
        out.append("\f")
    return "\n".join(out)


def build_manifest(files: List[SourceFile], pages: List[Tuple[str, int, List[str]]]) -> List[dict]:
    """生成文件清单：每个文件的总行数，以及它在入选页中出现的页号区间。"""
    page_of: dict = {}
    for idx, (rel, _start, _lines) in enumerate(pages, start=1):
        page_of.setdefault(rel, []).append(idx)

    rows = []
    for f in files:
        hit = page_of.get(f.rel)
        rows.append(
            {
                "文件": f.rel,
                "行数": len(f.lines),
                "入选页码": (f"{hit[0]}-{hit[-1]}" if hit else "未入选"),
            }
        )
    return rows


def main() -> int:
    ap = argparse.ArgumentParser(description="软著源程序册生成器")
    ap.add_argument("--name", default=DEFAULT_NAME, help="软件名称（页眉用）")
    ap.add_argument("--version", default=DEFAULT_VERSION, help="版本号（页眉用）")
    ap.add_argument("--out", default="dist/softcopy", help="输出目录")
    ap.add_argument("--per-page", type=int, default=LINES_PER_PAGE, help="每页行数（默认 50）")
    args = ap.parse_args()

    out_dir = (ROOT / args.out) if not os.path.isabs(args.out) else Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    if args.per_page < 50:
        print("[WARN] 每页行数低于 50，可能不符合受理要求", file=sys.stderr)

    files = collect_sources()
    if not files:
        print("[ERROR] 未收集到任何源文件", file=sys.stderr)
        return 1

    all_pages = paginate(files, args.per_page)
    selected, note = select_pages(all_pages)

    (out_dir / "源程序.html").write_text(
        build_html(args.name, args.version, selected, len(selected)), encoding="utf-8"
    )
    (out_dir / "源程序.txt").write_text(
        build_txt(args.name, args.version, selected), encoding="utf-8"
    )

    with open(out_dir / "源程序清单.csv", "w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["文件", "行数", "入选页码"])
        w.writeheader()
        w.writerows(build_manifest(files, selected))

    total_lines = sum(len(f.lines) for f in files)
    print(f"[OK] 软件名称 : {args.name} {args.version}")
    print(f"[OK] 收录文件 : {len(files)} 个，共 {total_lines} 行")
    print(f"[OK] 总页数   : {len(all_pages)} 页（每页 {args.per_page} 行）")
    print(f"[OK] 提交策略 : {note}")
    print(f"[OK] 输出目录 : {out_dir}")
    for n in ("源程序.html", "源程序.txt", "源程序清单.csv"):
        p = out_dir / n
        print(f"     - {n}  ({p.stat().st_size / 1024:.1f} KB)")
    print("\n[提示] 用 Chrome/Edge 打开 源程序.html，按 Ctrl+P，目标另存为 PDF，"
          "纸张 A4、边距「默认」、勾选「背景图形」关闭，即得送审 PDF。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

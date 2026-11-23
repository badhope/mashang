#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成对外展示的品牌资产（banner / 竖版海报 / OG 分享图 / 商店横幅）。

品牌口径（2026-11 品牌重塑）：
  CodeLearning（英文，一个词，C 与 L 大写）/ 中文名「码上」保留
  课程口径：6 门语言，共 245 讲

产物：
  docs/banner.png                         2560x800  仓库头图（README 顶部）
  demo/promo-poster.jpg                   1080x1920 宣传片封面 / 海报
  docs/assets/og.jpg                      1200x630  Open Graph 分享图
  docs/assets/feature-graphic-1024x500.png 1024x500 商店特色图片

所有素材从仓库内的真实截图与数据生成，可重复执行、结果稳定（无随机数）。
缺截图时跳过对应画面并在 stdout 明确说明，不伪造图像。

用法：python tools/make_brand_assets.py
"""
import pathlib

from PIL import Image, ImageDraw, ImageFont

ROOT = pathlib.Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
DEMO = ROOT / "demo"

BRAND = "CodeLearning"
BRAND_CN = "码上"
BRAND_FULL = "CodeLearning · 码上"
TAGLINE = "码上 · 离线编程学习终端"
COURSE_LINE = "6 门语言，共 245 讲"
COURSE_LINE_EN = "6 LANGUAGES, 245 LESSONS"

CYAN = (0, 229, 255)
GREEN = (0, 255, 156)
MAGENTA = (255, 45, 120)
YELLOW = (247, 255, 0)
BG0 = (10, 14, 23)
BG1 = (18, 26, 42)
TEXT_HI = (249, 250, 251)
TEXT_MID = (147, 163, 184)

FONT_CN = "C:/Windows/Fonts/msyhbd.ttc"
FONT_CN_REG = "C:/Windows/Fonts/msyh.ttc"
FONT_LATIN = "C:/Windows/Fonts/segoeuib.ttf"
FONT_LATIN_REG = "C:/Windows/Fonts/segoeui.ttf"
FONT_MONO = "C:/Windows/Fonts/consolab.ttf"
FONT_MONO_REG = "C:/Windows/Fonts/consola.ttf"

# Logo 几何（108×108 网格）：代码括号 + 上升阶梯
MARK_LEFT = [(41, 32), (27, 54), (41, 76)]
MARK_RIGHT = [(67, 32), (81, 54), (67, 76)]
MARK_STAIR = [(40, 74), (40, 64), (51, 64), (51, 54), (62, 54), (62, 44)]


def font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except OSError:
        return ImageFont.load_default()


def draw_mark(draw, x, y, size, stroke=None):
    """按 108 网格绘制 CodeLearning 标记，size 为标记外框边长。"""
    scale = size / 108.0
    width = max(2, int(round(8 * scale)))
    if stroke is None:
        stroke = width

    def pts(seq):
        return [(x + px * scale, y + py * scale) for px, py in seq]

    draw.line(pts(MARK_LEFT), fill=CYAN, width=stroke, joint="curve")
    draw.line(pts(MARK_RIGHT), fill=CYAN, width=stroke, joint="curve")
    draw.line(pts(MARK_STAIR), fill=GREEN, width=stroke, joint="curve")


def grid(draw, w, h, step=64, color=(0, 229, 255, 18)):
    for x in range(0, w, step):
        draw.line([(x, 0), (x, h)], fill=color, width=1)
    for y in range(0, h, step):
        draw.line([(0, y), (w, y)], fill=color, width=1)


def corners(draw, w, h, pad=28, length=72, color=CYAN, width=3):
    for x, y, dx, dy in (
        (pad, pad, 1, 1), (w - pad, pad, -1, 1),
        (pad, h - pad, 1, -1), (w - pad, h - pad, -1, -1),
    ):
        draw.line([(x, y), (x + dx * length, y)], fill=color, width=width)
        draw.line([(x, y), (x, y + dy * length)], fill=color, width=width)


def vertical_gradient(w, h, top=BG1, bottom=BG0):
    img = Image.new("RGB", (w, h), top)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img


def spaced(draw, xy, text, fnt, fill, spacing=8):
    """逐字加字距绘制（PIL 没有 letter-spacing）。"""
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=fnt, fill=fill)
        x += draw.textlength(ch, font=fnt) + spacing
    return x


def banner():
    w, h = 2560, 800
    img = vertical_gradient(w, h)
    d = ImageDraw.Draw(img, "RGBA")
    grid(d, w, h, 64, (0, 229, 255, 20))
    corners(d, w, h, pad=34, length=90)

    d.text((112, 92), "CodeLearning   ·   " + COURSE_LINE_EN + "   ·   OFFLINE",
           font=font(FONT_MONO_REG, 30), fill=GREEN)

    draw_mark(d, 108, 172, 200)

    d.text((350, 168), BRAND, font=font(FONT_LATIN, 132), fill=TEXT_HI)
    d.text((356, 318), TAGLINE, font=font(FONT_CN_REG, 40), fill=TEXT_MID)

    d.text((112, 430), "六门语言，真跑，全离线", font=font(FONT_CN, 62), fill=TEXT_HI)
    cn_font = font(FONT_CN_REG, 30)
    d.text((112, 522), COURSE_LINE, font=cn_font, fill=TEXT_MID)
    d.text((112 + d.textlength(COURSE_LINE, font=cn_font) + 18, 526),
           "·  Python · JavaScript · TypeScript · C · Lua · SQL",
           font=font(FONT_MONO_REG, 27), fill=TEXT_MID)

    chips = [("六门语言真实运行", CYAN), ("245 讲闯关", GREEN), ("assert 自动判题", CYAN), ("MIT 开源", YELLOW)]
    x = 112
    for label, color in chips:
        f = font(FONT_CN_REG, 30)
        tw = d.textlength(label, font=f)
        box = (x, 598, x + tw + 52, 668)
        d.rounded_rectangle(box, radius=35, outline=color, width=2, fill=(color[0], color[1], color[2], 20))
        d.text((x + 26, 614), label, font=f, fill=color)
        x = box[2] + 22

    d.text((2448, 700), "245 LESSONS · 22 CHALLENGES · 6 LANGUAGES",
           font=font(FONT_MONO_REG, 24), fill=(120, 136, 160), anchor="ra")
    img.save(DOCS / "banner.png", optimize=True)
    print("docs/banner.png", img.size)


def phone_paste(canvas, shot_path, box):
    shot = Image.open(shot_path).convert("RGB")
    tw, th = box[2] - box[0], box[3] - box[1]
    scale = min(tw / shot.width, th / shot.height)
    resized = shot.resize((int(shot.width * scale), int(shot.height * scale)), Image.LANCZOS)
    x = box[0] + (tw - resized.width) // 2
    y = box[1] + (th - resized.height) // 2
    canvas.paste(resized, (x, y))


def promo_poster():
    w, h = 1080, 1920
    img = vertical_gradient(w, h)
    d = ImageDraw.Draw(img, "RGBA")
    grid(d, w, h, 72, (0, 229, 255, 16))
    corners(d, w, h, pad=30, length=80)

    draw_mark(d, 74, 74, 96)
    d.text((190, 92), BRAND, font=font(FONT_LATIN, 44), fill=TEXT_HI)
    d.text((192, 146), BRAND_CN + " · 离线编程学习终端", font=font(FONT_CN_REG, 24), fill=GREEN)

    d.text((72, 226), "码上，", font=font(FONT_CN, 116), fill=TEXT_HI)
    d.text((72, 356), "就是马上。", font=font(FONT_CN, 116), fill=CYAN)
    d.text((76, 512), "装进口袋的赛博朋克编程学习终端",
           font=font(FONT_CN_REG, 40), fill=TEXT_MID)
    d.text((76, 574), "6 门语言 · 共 245 讲 · assert 判题 · 全离线",
           font=font(FONT_CN_REG, 27), fill=GREEN)

    home = DEMO / "app" / "home.png"
    if home.exists():
        phone_paste(img, home, (250, 646, 830, 1566))
        d.rounded_rectangle((250, 646, 830, 1566), radius=34, outline=CYAN, width=3)
    else:
        print("  跳过（缺文件）：demo/app/home.png，海报空出手机位")

    d.text((540, 1650), "离线写码 · 变量快照 · 六段位成长", font=font(FONT_CN_REG, 38),
           fill=TEXT_HI, anchor="ma")
    d.text((540, 1712), "github.com/badhope/mashang", font=font(FONT_MONO_REG, 30),
           fill=MAGENTA, anchor="ma")
    d.text((540, 1812), "MIT LICENSE · ANDROID 10.0+ · FREE", font=font(FONT_CN_REG, 22),
           fill=(120, 136, 160), anchor="ma")
    img.convert("RGB").save(DEMO / "promo-poster.jpg", quality=88, optimize=True, progressive=True)
    print("demo/promo-poster.jpg", img.size)


def og_image():
    w, h = 1200, 630
    img = vertical_gradient(w, h)
    d = ImageDraw.Draw(img, "RGBA")
    grid(d, w, h, 56, (0, 229, 255, 18))

    draw_mark(d, 60, 48, 92)
    d.text((172, 60), BRAND, font=font(FONT_LATIN, 52), fill=TEXT_HI)
    d.text((174, 118), BRAND_CN + " · 离线编程学习终端", font=font(FONT_CN_REG, 24), fill=GREEN)

    d.text((56, 186), "码上，就是马上。", font=font(FONT_CN, 74), fill=TEXT_HI)
    d.text((58, 292), "6 门语言 · 共 245 讲", font=font(FONT_CN_REG, 29), fill=TEXT_MID)
    d.text((58, 336), "assert 判题 · 变量快照 · 全离线", font=font(FONT_CN_REG, 29), fill=TEXT_MID)

    shot = DEMO / "app" / "lesson_exercise.png"
    box = (790, 60, 1160, 600)
    if shot.exists():
        phone_paste(img, shot, box)
        d.rounded_rectangle(box, radius=26, outline=CYAN, width=2)
    else:
        print("  跳过（缺文件）：demo/app/lesson_exercise.png，OG 图空出手机位")

    d.text((60, 512), "ANDROID 10.0+  ·  MIT  ·  GITHUB.COM/BADHOPE/MASHANG",
           font=font(FONT_MONO_REG, 22), fill=MAGENTA)
    img.save(DOCS / "assets" / "og.jpg", quality=88, optimize=True, progressive=True)
    print("docs/assets/og.jpg", img.size)


def feature_graphic():
    """Google Play 特色图片 1024x500（多数市场的通用横幅尺寸）。

    Play 规范要点：文字要在缩略图下仍可辨认、不得有大面积留白、
    不得出现与应用无关的误导性内容。

    布局：
      - 左侧：标记 + 品牌名 + 主标题 + 六语言标签，宽度限制在 660px 内
      - 右侧：独立数据卡片（x=690 起），避免与主标题重叠
      - 主标题字号按实测宽度收敛，宁小不溢出
    """
    w, h = 1024, 500
    img = vertical_gradient(w, h)
    d = ImageDraw.Draw(img, "RGBA")
    grid(d, w, h, 48, (0, 229, 255, 16))
    corners(d, w, h, pad=22, length=56)

    LEFT_RIGHT = 656  # 左栏可用右边界

    draw_mark(d, 60, 62, 74)

    title_font_size = 62
    while title_font_size > 36:
        f = font(FONT_LATIN, title_font_size)
        if d.textlength(BRAND, font=f) <= LEFT_RIGHT - 150:
            break
        title_font_size -= 2
    d.text((152, 62), BRAND, font=font(FONT_LATIN, title_font_size), fill=TEXT_HI)
    d.text((154, 132), BRAND_CN + " · 离线编程学习终端", font=font(FONT_CN_REG, 22), fill=GREEN)

    d.text((62, 194), "六门语言，真跑，全离线", font=font(FONT_CN, 42), fill=TEXT_HI)
    d.text((62, 256), "断网也能写代码、判题、升级", font=font(FONT_CN_REG, 25), fill=TEXT_MID)

    langs = [("Python", CYAN), ("JavaScript", YELLOW), ("TypeScript", CYAN),
             ("C", GREEN), ("Lua", GREEN), ("SQL", MAGENTA)]
    x, y = 62, 322
    for name, color in langs:
        f = font(FONT_MONO, 20)
        tw = d.textlength(name, font=f)
        box_w = tw + 26
        if x + box_w > LEFT_RIGHT:
            x, y = 62, y + 46
        box = (x, y, x + box_w, y + 36)
        d.rounded_rectangle(box, radius=18, outline=color, width=2,
                            fill=(color[0], color[1], color[2], 26))
        d.text((x + 13, y + 7), name, font=f, fill=color)
        x = box[2] + 10

    bx0, by0, bx1, by1 = 690, 96, 984, 404
    d.rounded_rectangle((bx0, by0, bx1, by1), radius=18,
                        outline=(0, 229, 255, 90), width=2, fill=(11, 18, 32, 210))

    stats = [("245", "讲课程"), ("6", "门语言"), ("0", "广告 · 账号")]
    sy = by0 + 28
    for num, label in stats:
        d.text((bx0 + 30, sy), num, font=font(FONT_MONO, 42), fill=CYAN)
        d.text((bx0 + 148, sy + 14), label, font=font(FONT_CN_REG, 22), fill=TEXT_MID)
        sy += 78

    d.text((bx0 + 30, by1 - 40), "MIT 开源 · 数据不出手机",
           font=font(FONT_CN_REG, 18), fill=GREEN)

    out = DOCS / "assets" / "feature-graphic-1024x500.png"
    img.save(out, optimize=True)
    print("docs/assets/feature-graphic-1024x500.png", img.size)


def main():
    (DOCS / "assets").mkdir(parents=True, exist_ok=True)
    banner()
    feature_graphic()
    promo_poster()
    og_image()


if __name__ == "__main__":
    main()


# ===== 商店截图（带标题标注） =====
# Play / 各市场要求截图能一眼看懂卖点；纯界面截图信息密度太高。
# 做法：把原截图等比缩放放进画布，顶部留出标题条，底部留白。
STORE_SHOTS = [
    ("home.png", "六门语言，一个终端", "Six languages, one terminal"),
    ("lessons.png", "245 讲课程，从入门到实战", "245 lessons, beginner to capstone"),
    ("lesson_detail.png", "每讲都有讲解、图解、随堂问", "Explanations, diagrams, inline quizzes"),
    ("lesson_exercise.png", "写完就跑，跑完就判", "Write, run, get judged"),
    ("judge_pass.png", "用例全过才放行", "All tests must pass"),
    ("judge_fail.png", "失败时告诉你哪条断言没过", "Told exactly which assertion failed"),
    ("terminal.png", "内置终端，随时试验", "Built-in terminal, experiment freely"),
    ("tracks.png", "真实课程路线，不掺水分", "Real curriculum, no filler"),
    ("contenthub.png", "语言中心，按需下载", "Language Center, on-demand packs"),
    ("settings.png", "外观、字号、诊断都能调", "Appearance, font size, diagnostics"),
]


def store_screenshots():
    """生成商店用截图：1080x1920 画布 + 顶部标题条。

    输出到 store/screenshots/，文件名带序号便于按顺序上传。
    """
    out_dir = ROOT / "store" / "screenshots"
    out_dir.mkdir(parents=True, exist_ok=True)

    CW, CH = 1080, 1920
    TOP_H = 210          # 标题条高度
    PAD = 40             # 内容左右留白

    for idx, (fname, zh, en) in enumerate(STORE_SHOTS, start=1):
        src_path = DEMO / "app" / fname
        if not src_path.exists():
            print(f"  跳过（缺文件）：{fname}")
            continue

        canvas = vertical_gradient(CW, CH)
        d = ImageDraw.Draw(canvas, "RGBA")
        grid(d, CW, CH, 60, (0, 229, 255, 12))

        draw_mark(d, PAD, 40, 64)
        d.text((PAD + 84, 46), BRAND, font=font(FONT_LATIN, 30), fill=CYAN)
        d.text((PAD, 116), zh, font=font(FONT_CN, 46), fill=TEXT_HI)
        d.text((PAD, 176), en, font=font(FONT_MONO_REG, 20), fill=TEXT_MID)

        shot = Image.open(src_path).convert("RGB")
        avail_h = CH - TOP_H - PAD
        avail_w = CW - PAD * 2
        scale = min(avail_w / shot.width, avail_h / shot.height)
        nw, nh = int(shot.width * scale), int(shot.height * scale)
        shot = shot.resize((nw, nh), Image.LANCZOS)

        x = (CW - nw) // 2
        y = TOP_H
        # 截图加细边框，避免与背景糊在一起
        d.rounded_rectangle((x - 3, y - 3, x + nw + 3, y + nh + 3),
                            radius=18, outline=(0, 229, 255, 70), width=2)
        canvas.paste(shot, (x, y))

        out = out_dir / f"{idx:02d}-{fname.replace('.png', '')}.png"
        canvas.convert("RGB").save(out, optimize=True, quality=92)
        print(f"  store/screenshots/{out.name}")


if __name__ == "__main__":
    store_screenshots()

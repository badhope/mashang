#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 Python 综合项目课 l39（命令行记账本），并合并进 lessons_pro.json。

运行：python tools/gen_python_capstone.py
"""
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
TARGET = ROOT / "app" / "src" / "main" / "assets" / "lessons_pro.json"

SOLUTION = '''def add_expense(book, category, amount):
    if amount <= 0:
        raise ValueError("金额必须为正数")
    book.setdefault(category, []).append(amount)
    return book


def total_by_category(book):
    return {name: sum(items) for name, items in book.items()}


def top_category(book):
    totals = total_by_category(book)
    if not totals:
        return None
    return max(totals, key=lambda name: (totals[name], name))
'''

DEMO = SOLUTION + '''
book = {}
add_expense(book, "food", 35)
add_expense(book, "transport", 12)
add_expense(book, "food", 18)
print(total_by_category(book))
print(top_category(book))
'''

LESSON = {
    "id": "l39",
    "order": 39,
    "chapter": 5,
    "chapterName": "第五幕 · 生产实战",
    "title": "综合项目：命令行记账本",
    "subtitle": "把输入、数据模型、聚合与异常串成一条线",
    "xp": 220,
    "blocks": [
        {"type": "heading", "text": "从「会写函数」到「能交付一个小工具」"},
        {
            "type": "text",
            "text": "前面 38 讲拆开练了变量、容器、函数、异常、文件。真正写项目时，难点不是某个语法，"
                    "而是决定「数据长什么样、函数各负责什么、错误在哪里拦住」。这讲用一个记账本"
                    "把这些决定串起来：输入一笔支出，按分类汇总，找出花得最多的分类。",
        },
        {
            "type": "diagram",
            "text": "输入（分类, 金额）\n      │\n      ▼\n add_expense ──► book = {'food': [35, 18], ...}\n      │\n      ▼\n total_by_category ──► {'food': 53, 'transport': 12}\n      │\n      ▼\n top_category ──► 'food'",
        },
        {
            "type": "text",
            "text": "数据模型选 dict[str, list[float]]：键是分类，值是这个分类下的每笔金额。"
                    "为什么不直接存总额？因为保留明细才能回答「这个月有几笔午餐」「最大单笔是多少」。"
                    "聚合函数只读不写，新增函数只写不改结构，职责分开后测试也好写。",
        },
        {"type": "code", "runnable": True, "code": DEMO},
        {"type": "output", "text": "{'food': 53, 'transport': 12}\nfood"},
        {
            "type": "table",
            "headers": ["函数", "输入", "输出", "错误边界"],
            "rows": [
                ["add_expense", "book, 分类, 金额", "原 book（可链式调用）", "金额 <= 0 抛 ValueError"],
                ["total_by_category", "book", "分类到总额的 dict", "空 book 返回 {}"],
                ["top_category", "book", "总额最大的分类名", "空 book 返回 None"],
            ],
        },
        {
            "type": "tip",
            "text": "先写契约再写实现：每个函数接收什么、返回什么、什么情况报错。契约稳定后，"
                    "换存储方式（dict → SQLite）也不用改调用方。",
        },
        {
            "type": "warn",
            "text": "不要用可变默认参数 `def add(book={})`。默认值只在函数定义时创建一次，"
                    "多次调用会共享同一个 dict，账目会串。必须由调用方传入 book。",
        },
        {
            "type": "quiz",
            "question": "为什么 top_category 在空账本时返回 None，而不是抛异常？",
            "options": [
                "因为 Python 不允许抛异常",
                "空账本是正常业务状态，不是错误；调用方可以显示「暂无记录」",
                "返回 None 比抛异常快",
                "None 会自动变成 0",
            ],
            "answer": 1,
            "explain": "空数据是合法状态，用 None 表达「没有结果」；金额非法才是错误，用 ValueError。",
        },
        {
            "type": "practice",
            "title": "先算分类总额",
            "code": "book = {'food': [35, 18], 'transport': [12]}\nprint({k: sum(v) for k, v in book.items()})",
            "output": "{'food': 53, 'transport': 12}",
            "hint": "字典推导式里对每个 value 调 sum。",
        },
    ],
    "exercise": {
        "title": "补全记账本三个函数",
        "brief": "实现 add_expense、total_by_category、top_category。金额必须为正；空账本 top 返回 None；"
                 "并列时返回字典序更大的分类名（max 的 key 用 (总额, 分类名)）。",
        "starterCode": '''def add_expense(book, category, amount):
    # 金额 <= 0 抛 ValueError；把金额追加到 book[category]
    pass


def total_by_category(book):
    # 返回 {分类: 总额}
    return {}


def top_category(book):
    # 空账本返回 None；否则返回总额最大的分类
    return None

book = {}
add_expense(book, "food", 35)
add_expense(book, "food", 18)
print(total_by_category(book))
''',
        "hint": "add_expense 用 book.setdefault(category, []).append(amount)；top_category 用 max(totals, key=lambda k: (totals[k], k))。",
        "tests": [
            "book = {}; add_expense(book, 'food', 35); add_expense(book, 'food', 18); assert total_by_category(book) == {'food': 53}",
            "book = {}; add_expense(book, 'a', 10); add_expense(book, 'b', 20); assert top_category(book) == 'b'",
            "book = {}; assert top_category(book) is None",
            "book = {}\ntry:\n    add_expense(book, 'x', 0)\n    assert False, '金额 0 应抛 ValueError'\nexcept ValueError:\n    pass",
        ],
        "xp": 220,
        "solution": SOLUTION,
    },
}


def main():
    lessons = json.loads(TARGET.read_text(encoding="utf-8"))
    lessons = [x for x in lessons if x["id"] != "l39"]
    lessons.append(LESSON)
    ns = {}
    exec(SOLUTION, ns)  # noqa: S102 - 生成期验证参考答案
    book = {}
    ns["add_expense"](book, "food", 35)
    ns["add_expense"](book, "food", 18)
    assert ns["total_by_category"](book) == {"food": 53}
    assert ns["top_category"](book) == "food"
    assert ns["top_category"]({}) is None
    TARGET.write_text(json.dumps(lessons, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(f"l39 -> {TARGET.name}, {len(lessons)} 讲")


if __name__ == "__main__":
    main()

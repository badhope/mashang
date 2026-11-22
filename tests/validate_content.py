import json
import os
import pathlib
import sys

sys.path.insert(
    0, os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "python")
)

import runner

APP_ASSETS = os.path.join(
    os.path.dirname(__file__), "..", "app", "src", "main", "assets"
)

LESSON_FILES = ["lessons_basic.json", "lessons_mid.json", "lessons_adv.json", "lessons_pro.json"]

SOLUTIONS = {
    "l01": "hacker_name = 'V'\nonline = True\nprint(hacker_name, online)",
    "l02": "energy = 9000\nshield = 62.5\n\ntotal = energy + shield\nprint(total)",
    "l03": "base = 'CYBER'\n\nfull_id = base + '-' + base[2:]\nprint(full_id)",
    "l04": (
        "def damage(attack, defense):\n"
        "    return int(attack ** 0.5) - defense // 2\n"
        "\n"
        "print(damage(25, 6))\n"
        "print(damage(100, 10))"
    ),
    "l05": (
        "def echo_band(band):\n"
        "    return 'BAND:' + band.upper()\n"
        "\n"
        "print(echo_band('neon'))"
    ),
    "l06": (
        "def access(level):\n"
        "    if level >= 100:\n"
        "        return 'ROOT'\n"
        "    elif level >= 50:\n"
        "        return 'OPERATOR'\n"
        "    else:\n"
        "        return 'GUEST'\n"
        "\n"
        "print(access(120))\n"
        "print(access(66))\n"
        "print(access(3))"
    ),
    "l07": (
        "def harmonic(limit):\n"
        "    total = 0\n"
        "    for n in range(2, limit + 1, 2):\n"
        "        total += n\n"
        "    return total\n"
        "\n"
        "print(harmonic(10))"
    ),
    "l08": (
        "def peak(values):\n"
        "    if not values:\n"
        "        return None\n"
        "    best = values[0]\n"
        "    for v in values:\n"
        "        if v > best:\n"
        "            best = v\n"
        "    return best\n"
        "\n"
        "print(peak([88, 92, 79]))\n"
        "print(peak([]))"
    ),
    "l09": (
        "def freq(text):\n"
        "    counts = {}\n"
        "    for ch in text:\n"
        "        counts[ch] = counts.get(ch, 0) + 1\n"
        "    return counts\n"
        "\n"
        "print(freq('aab'))"
    ),
    "l10": (
        "def street_credit(stats):\n"
        "    total = sum(stats.values())\n"
        "    if total >= 100:\n"
        "        return 'LEGEND'\n"
        "    elif total >= 60:\n"
        "        return 'PRO'\n"
        "    else:\n"
        "        return 'ROOKIE'\n"
        "\n"
        "s = {'atk': 40, 'def': 35, 'hack': 30}\n"
        "print(street_credit(s))"
    ),
    "l11": (
        "def slugify(title):\n"
        "    return title.strip().lower().replace(' ', '-')\n"
        "\n"
        "print(slugify('Hello Neon City'))\n"
        "print(slugify('  PY  '))"
    ),
    "l12": (
        "def dedupe(seq):\n"
        "    result = []\n"
        "    seen = set()\n"
        "    for item in seq:\n"
        "        if item not in seen:\n"
        "            seen.add(item)\n"
        "            result.append(item)\n"
        "    return result\n"
        "\n"
        "print(dedupe([3, 1, 3, 2, 1]))"
    ),
    "l13": (
        "def flex_total(*nums, start=0):\n"
        "    return sum(nums) + start\n"
        "\n"
        "print(flex_total(1, 2, 3))\n"
        "print(flex_total(1, 2, start=10))"
    ),
    "l14": (
        "def even_squares(n):\n"
        "    return [i * i for i in range(1, n + 1) if i % 2 == 0]\n"
        "\n"
        "print(even_squares(5))\n"
        "print(even_squares(1))"
    ),
    "l15": (
        "def safe_div(a, b):\n"
        "    try:\n"
        "        return a / b\n"
        "    except ZeroDivisionError:\n"
        "        return None\n"
        "\n"
        "def parse_age(s):\n"
        "    try:\n"
        "        return int(s)\n"
        "    except ValueError:\n"
        "        return -1\n"
        "\n"
        "print(safe_div(6, 3), safe_div(1, 0))\n"
        "print(parse_age('20'), parse_age('abc'))"
    ),
    "l16": (
        "import json\n"
        "\n"
        "def restore_agent(json_str):\n"
        "    data = json.loads(json_str)\n"
        "    data.setdefault('level', 1)\n"
        "    return data\n"
        "\n"
        'print(restore_agent(\'{"name": "V"}\'))\n'
        'print(restore_agent(\'{"name": "J", "level": 5}\'))'
    ),
    "l17": (
        "import random\n"
        "\n"
        "def roll(sides, times):\n"
        "    random.seed(7)\n"
        "    return [random.randint(1, sides) for _ in range(times)]\n"
        "\n"
        "print(roll(6, 4))"
    ),
    "l18": (
        "class NeonBot:\n"
        "    def __init__(self, name, hp=100):\n"
        "        self.name = name\n"
        "        self.hp = hp\n"
        "\n"
        "    def hit(self, dmg):\n"
        "        self.hp = max(0, self.hp - dmg)\n"
        "\n"
        "    def is_alive(self):\n"
        "        return self.hp > 0\n"
        "\n"
        "b = NeonBot('K', 50)\n"
        "b.hit(30)\n"
        "print(b.hp, b.is_alive())\n"
        "b.hit(999)\n"
        "print(b.is_alive())"
    ),
    "l19": (
        "class NeonBot:\n"
        "    def __init__(self, name, hp=100):\n"
        "        self.name = name\n"
        "        self.hp = hp\n"
        "\n"
        "    def hit(self, dmg):\n"
        "        self.hp = max(0, self.hp - dmg)\n"
        "\n"
        "class Ninja(NeonBot):\n"
        "    def __init__(self, name, hp=100, weapon='刀'):\n"
        "        super().__init__(name, hp)\n"
        "        self.weapon = weapon\n"
        "\n"
        "    def hit(self, dmg):\n"
        "        super().hit(dmg // 2)\n"
        "\n"
        "    def __str__(self):\n"
        "        return f'Ninja:{self.name}'\n"
        "\n"
        "k = Ninja('K', weapon='振动刀')\n"
        "k.hit(30)\n"
        "print(k.hp, k.weapon)\n"
        "print(str(k))"
    ),
    "l20": (
        "class Wallet:\n"
        "    def __init__(self, balance=0):\n"
        "        self.balance = balance\n"
        "\n"
        "    def deposit(self, amount):\n"
        "        self.balance += amount\n"
        "        return self.balance\n"
        "\n"
        "    def withdraw(self, amount):\n"
        "        if amount > self.balance:\n"
        "            raise ValueError('insufficient')\n"
        "        self.balance -= amount\n"
        "        return self.balance\n"
        "\n"
        "w = Wallet(100)\n"
        "print(w.withdraw(30))\n"
        "try:\n"
        "    w.withdraw(999)\n"
        "except ValueError as e:\n"
        "    print('拦截:', e)"
    ),
    "l21": (
        "def fibonacci(k):\n"
        "    a, b = 0, 1\n"
        "    for _ in range(k):\n"
        "        yield a\n"
        "        a, b = b, a + b\n"
        "\n"
        "print(list(fibonacci(6)))"
    ),
    "l22": (
        "def make_multiplier(factor):\n"
        "    def apply(x):\n"
        "        return x * factor\n"
        "    return apply\n"
        "\n"
        "def double_result(fn):\n"
        "    def inner(a, b):\n"
        "        return fn(a, b) * 2\n"
        "    return inner\n"
        "\n"
        "@double_result\n"
        "def add(a, b):\n"
        "    return a + b\n"
        "\n"
        "twice = make_multiplier(2)\n"
        "print(twice(5), add(1, 2))"
    ),
    "l23": (
        "def process(nums, mode):\n"
        "    ops = {\n"
        "        'sq': lambda x: x * x,\n"
        "        'dbl': lambda x: x * 2\n"
        "    }\n"
        "    return list(map(ops[mode], nums))\n"
        "\n"
        "print(process([1, 2, 3], 'sq'))\n"
        "print(process([1, 2, 3], 'dbl'))"
    ),
    "l24": (
        "from collections import Counter\n"
        "\n"
        "def top_word(sentence):\n"
        "    words = sentence.lower().split()\n"
        "    return Counter(words).most_common(1)[0][0]\n"
        "\n"
        "print(top_word('Neon city NEON rain'))"
    ),
    "l25": (
        "from datetime import date\n"
        "import random\n"
        "\n"
        "def weekday_of(y, m, d):\n"
        "    return date(y, m, d).strftime('%a')\n"
        "\n"
        "def shuffle_copy(lst, seed):\n"
        "    copy = list(lst)\n"
        "    random.Random(seed).shuffle(copy)\n"
        "    return copy\n"
        "\n"
        "print(weekday_of(2026, 8, 26))\n"
        "print(shuffle_copy([1, 2, 3, 4, 5], 99))"
    ),
    "l26": (
        "def analyze(lines):\n"
        "    result = {}\n"
        "    for line in lines:\n"
        "        tag = line[1:line.index(']')]\n"
        "        result[tag] = result.get(tag, 0) + 1\n"
        "    result['total'] = len(lines)\n"
        "    return result\n"
        "\n"
        "sample = ['[ERROR] a', '[INFO] b', '[ERROR] c']\n"
        "print(analyze(sample))"
    ),
    "l27": (
        "class Gate:\n"
        "    def __init__(self):\n"
        "        self.inside = False\n"
        "    def __enter__(self):\n"
        "        self.inside = True\n"
        "        return self\n"
        "    def __exit__(self, exc_type, exc, tb):\n"
        "        self.inside = False\n"
        "\n"
        "gate = Gate()\n"
        "with gate as g:\n"
        "    print('进入:', g.inside)"
    ),
    "bonus-27": (
        "def safe_len(x):\n"
        "    return len(x) if hasattr(x, '__len__') else None\n"
        "\n"
        "print(safe_len('neon'))\n"
        "print(safe_len(42))"
    ),
    "l28": (
        "import os, tempfile\n"
        "def write_note(path, text):\n"
        "    with open(path, 'w', encoding='utf-8') as f:\n"
        "        f.write(text)\n"
        "    return len(text)\n"
        "\n"
        "p = os.path.join(tempfile.gettempdir(), 'note.txt')\n"
        "print(write_note(p, 'hello'))\n"
        "print(open(p, encoding='utf-8').read())"
    ),
    "l29": (
        "class HackError(Exception):\n"
        "    def __init__(self, msg='非法入侵'):\n"
        "        super().__init__(msg)\n"
        "\n"
        "def guard(n):\n"
        "    if n < 0:\n"
        "        raise HackError()\n"
        "    return n * 2\n"
        "\n"
        "print(guard(5))\n"
        "try:\n"
        "    guard(-1)\n"
        "except HackError as e:\n"
        "    print('已拦截:', e)"
    ),
    "l30": (
        "import math\n"
        "\n"
        "def area_circle(r):\n"
        "    return round(math.pi * r * r, 2)\n"
        "\n"
        "print(area_circle(1))\n"
        "print(area_circle(2))"
    ),
    "l31": (
        "import re\n"
        "\n"
        "def is_phone(s):\n"
        "    return bool(re.fullmatch(r'1[3-9]\\d{9}', s))\n"
        "\n"
        "print(is_phone('13800138000'))\n"
        "print(is_phone('12345'))"
    ),
    "l32": (
        "from dataclasses import dataclass\n"
        "\n"
        "@dataclass\n"
        "class Student:\n"
        "    name: str\n"
        "    score: int\n"
        "\n"
        "    def level(self) -> str:\n"
        "        if self.score >= 90:\n"
        "            return '优秀'\n"
        "        elif self.score >= 60:\n"
        "            return '及格'\n"
        "        else:\n"
        "            return '加油'\n"
        "\n"
        "s = Student('小明', 92)\n"
        "print(s, s.level())"
    ),
    "l33": (
        "from functools import partial\n"
        "from operator import mul\n"
        "\n"
        "double = partial(mul, 2)\n"
        "\n"
        "print(double(10))\n"
        "print(double(-3))"
    ),
    "l34": (
        "import threading\n"
        "\n"
        "results = []\n"
        "\n"
        "def collect(n):\n"
        "    results.append(n)\n"
        "\n"
        "threads = [threading.Thread(target=collect, args=(i,)) for i in range(2)]\n"
        "for t in threads:\n"
        "    t.start()\n"
        "for t in threads:\n"
        "    t.join()\n"
        "print(sorted(results))"
    ),
    "l35": (
        "import asyncio\n"
        "\n"
        "done = []\n"
        "\n"
        "async def greet(name: str, delay: float):\n"
        "    await asyncio.sleep(delay)\n"
        "    done.append(name)\n"
        "    print(f'你好，{name}')\n"
        "\n"
        "async def main():\n"
        "    await asyncio.gather(greet('A', 0.05), greet('B', 0.05))\n"
        "\n"
        "asyncio.run(main())"
    ),
    "l36": (
        "def parse_config(args):\n"
        "    cfg = {}\n"
        "    i = 0\n"
        "    while i < len(args):\n"
        "        key = args[i]\n"
        "        if key == '--name' and i + 1 < len(args):\n"
        "            cfg['name'] = args[i + 1]\n"
        "            i += 2\n"
        "        elif key == '--times' and i + 1 < len(args):\n"
        "            cfg['times'] = int(args[i + 1])\n"
        "            i += 2\n"
        "        else:\n"
        "            i += 1\n"
        "    return cfg\n"
        "\n"
        "print(parse_config(['--name', 'Python', '--times', '3']))"
    ),
    "l37": (
        "import unittest\n"
        "\n"
        "def max3(a, b, c):\n"
        "    return max(a, b, c)\n"
        "\n"
        "class TestMax3(unittest.TestCase):\n"
        "    def test_max_basic(self):\n"
        "        self.assertEqual(max3(1, 9, 5), 9)\n"
        "\n"
        "    def test_max_all_equal(self):\n"
        "        self.assertEqual(max3(4, 4, 4), 4)\n"
        "\n"
        "suite = unittest.TestLoader().loadTestsFromTestCase(TestMax3)\n"
        "result = unittest.TextTestRunner(verbosity=0).run(suite)\n"
        "print('通过', result.testsRun, '个，失败', len(result.failures) + len(result.errors))"
    ),
    "l38": (
        "def parse_requirements(text):\n"
        "    result = []\n"
        "    for ln in text.strip().splitlines():\n"
        "        ln = ln.strip()\n"
        "        if not ln or ln.startswith('#'):\n"
        "            continue\n"
        "        for sep in ('==', '>='):\n"
        "            if sep in ln:\n"
        "                name, ver = ln.split(sep, 1)\n"
        "                result.append((name.strip(), sep + ver.strip()))\n"
        "                break\n"
        "        else:\n"
        "            result.append((ln, ''))\n"
        "    return result\n"
        "\n"
        "print(parse_requirements('requests==2.32.3\\nflask>=3.0\\npytest'))"
    ),
    "c01": (
        "def neon_fb(n):\n"
        "    result = []\n"
        "    for i in range(1, n + 1):\n"
        "        if i % 15 == 0:\n"
        "            result.append('NEONCITY')\n"
        "        elif i % 3 == 0:\n"
        "            result.append('NEON')\n"
        "        elif i % 5 == 0:\n"
        "            result.append('CITY')\n"
        "        else:\n"
        "            result.append(str(i))\n"
        "    return result\n"
        "\n"
        "print(neon_fb(5))"
    ),
    "c02": (
        "def is_palindrome(s):\n"
        "    clean = ''.join(ch.lower() for ch in s if ch.isalnum())\n"
        "    return clean == clean[::-1]\n"
        "\n"
        "print(is_palindrome('No lemon, no melon'))\n"
        "print(is_palindrome('cyber'))"
    ),
    "c03": (
        "def strength(pw):\n"
        "    score = 0\n"
        "    if len(pw) >= 8:\n"
        "        score += 1\n"
        "    if any(ch.isupper() for ch in pw):\n"
        "        score += 1\n"
        "    if any(ch.islower() for ch in pw):\n"
        "        score += 1\n"
        "    if any(ch.isdigit() for ch in pw):\n"
        "        score += 1\n"
        "    if any((not ch.isalnum()) and (not ch.isspace()) for ch in pw):\n"
        "        score += 1\n"
        "    if score >= 4:\n"
        "        return 'STRONG'\n"
        "    elif score == 3:\n"
        "        return 'MEDIUM'\n"
        "    else:\n"
        "        return 'WEAK'\n"
        "\n"
        "print(strength('Abc123!@'))\n"
        "print(strength('abc'))"
    ),
    "c04": (
        "def is_balanced(s):\n"
        "    pairs = {')': '(', ']': '[', '}': '{'}\n"
        "    stack = []\n"
        "    for ch in s:\n"
        "        if ch in '([{':\n"
        "            stack.append(ch)\n"
        "        elif ch in pairs:\n"
        "            if not stack or stack.pop() != pairs[ch]:\n"
        "                return False\n"
        "    return not stack\n"
        "\n"
        "print(is_balanced('{[()]}'))\n"
        "print(is_balanced('([)]'))"
    ),
    "c05": (
        "def encode_rle(s):\n"
        "    if not s:\n"
        "        return ''\n"
        "    result = []\n"
        "    current = s[0]\n"
        "    count = 1\n"
        "    for ch in s[1:]:\n"
        "        if ch == current:\n"
        "            count += 1\n"
        "        else:\n"
        "            result.append(current + str(count))\n"
        "            current = ch\n"
        "            count = 1\n"
        "    result.append(current + str(count))\n"
        "    return ''.join(result)\n"
        "\n"
        "print(encode_rle('AAABBC'))"
    ),
    "c06": (
        "class Inventory:\n"
        "    def __init__(self):\n"
        "        self.goods = {}\n"
        "\n"
        "    def add(self, item, n):\n"
        "        if not isinstance(n, int) or n <= 0 or isinstance(n, bool):\n"
        "            raise ValueError('数量必须为正整数')\n"
        "        self.goods[item] = self.goods.get(item, 0) + n\n"
        "\n"
        "    def remove(self, item, n):\n"
        "        if self.stock(item) < n:\n"
        "            raise ValueError('库存不足')\n"
        "        self.goods[item] -= n\n"
        "\n"
        "    def stock(self, item):\n"
        "        return self.goods.get(item, 0)\n"
        "\n"
        "    def total(self):\n"
        "        return sum(self.goods.values())\n"
        "\n"
        "inv = Inventory()\n"
        "inv.add('chip', 5)\n"
        "inv.add('cell', 3)\n"
        "inv.remove('chip', 2)\n"
        "print(inv.stock('chip'), inv.total())\n"
        "try:\n"
        "    inv.remove('chip', 99)\n"
        "except ValueError as e:\n"
        "    print('拦截:', e)"
    ),
}


VALID_LANGS = {"python", "javascript", "typescript", "c", "lua", "sql"}


# 2026-11-22 综合项目课与综合挑战的参考答案
SOLUTIONS.update({
    "l39": """def add_expense(book, category, amount):
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
""",
    "py-cap": """def summarize(records):
    total = 0
    by_category = {}
    for category, amount in records:
        if amount <= 0:
            raise ValueError("金额必须为正数")
        total += amount
        by_category[category] = by_category.get(category, 0) + amount
    top = max(by_category, key=lambda k: (by_category[k], k)) if by_category else None
    return {"total": total, "by_category": by_category, "top": top}


def top_n(records, n):
    totals = summarize(records)["by_category"]
    ordered = sorted(totals, key=lambda k: (-totals[k], k))
    return ordered[:max(0, n)]
""",
})

def structural_checks(lessons):
    problems = []
    for lesson in lessons:
        lid = lesson["id"]
        has_runnable = any(
            b.get("type") == "code" and b.get("runnable") for b in lesson["blocks"]
        )
        if not has_runnable:
            problems.append(f"{lid}: 缺少可运行代码块")
        ex = lesson.get("exercise")
        if not ex:
            problems.append(f"{lid}: 缺少练习")
        elif lid not in SOLUTIONS and not ex.get("solution"):
            # 多语言：非 Python 课程的参考答案直接放在 exercise.solution 里，
            # 由对应语言的判题器（如 JS → node）使用
            problems.append(f"{lid}: 缺少参考答案")
        for b in lesson["blocks"]:
            if b.get("type") == "quiz":
                if not (0 <= b["answer"] < len(b["options"])):
                    problems.append(f"{lid}: quiz answer 越界")
            if b.get("type") == "table":
                width = len(b["headers"])
                for row in b["rows"]:
                    if len(row) != width:
                        problems.append(f"{lid}: 表格列数不一致")
            if b.get("type") == "compare":
                cols = b.get("columns") or []
                if len(cols) < 2:
                    problems.append(f"{lid}: compare 至少需要 2 栏")
                for c in cols:
                    if not c.get("code"):
                        problems.append(f"{lid}: compare 栏缺少 code")
                    if c.get("lang") not in VALID_LANGS:
                        problems.append(f"{lid}: compare 栏语言非法 {c.get('lang')!r}")
            if b.get("type") == "visualize":
                frames = b.get("frames") or []
                if len(frames) < 2:
                    problems.append(f"{lid}: visualize 至少需要 2 帧（否则没有「过程」）")
                for f in frames:
                    if not f.get("label"):
                        problems.append(f"{lid}: visualize 帧缺少 label")
                    names = {i.get("name") for i in (f.get("items") or [])}
                    for h in (f.get("highlight") or []):
                        if h not in names:
                            problems.append(
                                f"{lid}: visualize 高亮项 {h!r} 不在该帧 items 里"
                            )
    return problems


# DSA 增补包参考答案（与 content_packs/dsa-*.json 的 exercise 一一对应）
SOLUTIONS.update(
    {
        "dsa01": "def sum_to_n(n):\n    return n * (n + 1) // 2\n\nprint(sum_to_n(5))\nprint(sum_to_n(100))",
        "dsa02": (
            "def two_sum_sorted(nums, target):\n"
            "    i, j = 0, len(nums) - 1\n"
            "    while i < j:\n"
            "        s = nums[i] + nums[j]\n"
            "        if s == target:\n"
            "            return (i, j)\n"
            "        elif s < target:\n"
            "            i += 1\n"
            "        else:\n"
            "            j -= 1\n"
            "    return (-1, -1)\n"
            "\n"
            "print(two_sum_sorted([2, 7, 11, 15], 9))"
        ),
        "dsa03": (
            "def first_unique(s):\n"
            "    counts = {}\n"
            "    for ch in s:\n"
            "        counts[ch] = counts.get(ch, 0) + 1\n"
            "    for i, ch in enumerate(s):\n"
            "        if counts[ch] == 1:\n"
            "            return i\n"
            "    return -1\n"
            "\n"
            "print(first_unique('pyneon'))\n"
            "print(first_unique('aabb'))"
        ),
        "dsa04": (
            "def bracket_depth(s):\n"
            "    depth = 0\n"
            "    best = 0\n"
            "    for ch in s:\n"
            "        if ch == '(':\n"
            "            depth += 1\n"
            "            best = max(best, depth)\n"
            "        else:\n"
            "            depth -= 1\n"
            "            if depth < 0:\n"
            "                return -1\n"
            "    return -1 if depth != 0 else best\n"
            "\n"
            "print(bracket_depth('(()(()))'))\n"
            "print(bracket_depth('(()'))"
        ),
        "dsa05": (
            "def fib_memo(n, memo=None):\n"
            "    if memo is None:\n"
            "        memo = {}\n"
            "    if n in memo:\n"
            "        return memo[n]\n"
            "    if n < 2:\n"
            "        return n\n"
            "    memo[n] = fib_memo(n - 1, memo) + fib_memo(n - 2, memo)\n"
            "    return memo[n]\n"
            "\n"
            "print(fib_memo(10))\n"
            "print(fib_memo(30))"
        ),
        "dsa06": (
            "def choose_sort(nums):\n"
            "    data = list(nums)\n"
            "    for i in range(len(data)):\n"
            "        mi = i\n"
            "        for j in range(i + 1, len(data)):\n"
            "            if data[j] < data[mi]:\n"
            "                mi = j\n"
            "        data[i], data[mi] = data[mi], data[i]\n"
            "    return data\n"
            "\n"
            "print(choose_sort([3, 1, 2]))"
        ),
        "dsa07": (
            "def binary_search(nums, target):\n"
            "    lo, hi = 0, len(nums) - 1\n"
            "    while lo <= hi:\n"
            "        mid = (lo + hi) // 2\n"
            "        if nums[mid] == target:\n"
            "            return mid\n"
            "        elif nums[mid] < target:\n"
            "            lo = mid + 1\n"
            "        else:\n"
            "            hi = mid - 1\n"
            "    return -1\n"
            "\n"
            "print(binary_search([1, 2, 3, 4, 5], 3))\n"
            "print(binary_search([1, 2, 3, 4, 5], 9))"
        ),
        "dsa08": (
            "def min_coins(amount):\n"
            "    count = 0\n"
            "    for c in [25, 10, 5, 1]:\n"
            "        count += amount // c\n"
            "        amount %= c\n"
            "    return count\n"
            "\n"
            "print(min_coins(41))\n"
            "print(min_coins(30))"
        ),
        "dsa09": (
            "class Node:\n"
            "    def __init__(self, value):\n"
            "        self.value = value\n"
            "        self.next = None\n"
            "\n"
            "def ll_length(head):\n"
            "    cnt = 0\n"
            "    cur = head\n"
            "    while cur is not None:\n"
            "        cnt += 1\n"
            "        cur = cur.next\n"
            "    return cnt\n"
            "\n"
            "n1 = Node(1); n2 = Node(2); n3 = Node(3)\n"
            "n1.next = n2; n2.next = n3\n"
            "print(ll_length(n1))\n"
            "print(ll_length(None))"
        ),
        "dsa10": (
            "def max_subarray(nums):\n"
            "    best = float('-inf')\n"
            "    cur = 0\n"
            "    for x in nums:\n"
            "        cur = max(x, cur + x)\n"
            "        best = max(best, cur)\n"
            "    return best\n"
            "\n"
            "print(max_subarray([-2, 1, -3, 4, -1, 2, 1, -5, 4]))\n"
            "print(max_subarray([-1, -2]))"
        ),
    }
)


def main():
    lessons = []
    for name in LESSON_FILES:
        with open(os.path.join(APP_ASSETS, name), encoding="utf-8") as f:
            lessons.extend(json.load(f))
    # 内容包目录整体扫描：新增语言包/分层包不必再回来改这份清单
    for pack in sorted(
        p.name
        for p in (
            pathlib.Path(os.path.dirname(__file__)).resolve().parent / "content_packs"
        ).glob("*.json")
    ):
        pack_path = os.path.join(os.path.dirname(__file__), "..", "content_packs", pack)
        with open(pack_path, encoding="utf-8") as f:
            lessons.extend(json.load(f))
    with open(os.path.join(APP_ASSETS, "challenges.json"), encoding="utf-8") as f:
        challenges = json.load(f)

    problems = structural_checks(lessons)
    if not os.path.exists(
        os.path.join(os.path.dirname(__file__), "..", "catalog.json")
    ):
        problems.append("catalog.json 缺失（内容中心远端目录）")
    # 多语言：JS 课程的判题在 tests/test_js_lessons.py 里用真实 JS 引擎（node）
    # 执行；Python runner 无法执行 JS，这里只做结构检查并单独计数。
    js_lessons = [l for l in lessons if l.get("lang", "python") == "javascript"]
    c_lessons = [l for l in lessons if l.get("lang", "python") == "c"]
    ts_lessons = [l for l in lessons if l.get("lang", "python") == "typescript"]
    lua_lessons = [l for l in lessons if l.get("lang", "python") == "lua"]
    sql_lessons = [l for l in lessons if l.get("lang", "python") == "sql"]
    py_lessons = [
        l for l in lessons if l.get("lang", "python") not in ("javascript", "typescript", "c", "lua", "sql")
    ]
    items = []
    for lesson in sorted(py_lessons, key=lambda x: x["order"]):
        ex = lesson.get("exercise")
        if ex:
            items.append(
                (
                    lesson["id"],
                    ex["tests"],
                    ex.get("stdin") or [],
                    SOLUTIONS[lesson["id"]],
                )
            )
    # 只有 Python 挑战能交给 CPython 跑；JS 走 node，C/Lua/TS 走各自
    # Kotlin 单测，SQL 走 sqlite3，避免把非 Python 代码误当 Python 执行。
    py_challenges = [c for c in challenges if c.get("lang", "python") == "python"]
    for ch in py_challenges:
        items.append((ch["id"], ch["tests"], [], SOLUTIONS[ch["id"]]))

    failures = []
    checked = 0
    total_cases = 0
    for key, tests, stdin_lines, solution in items:
        result = runner.check_exercise(solution, tests, stdin_lines)
        checked += 1
        total_cases += len(tests)
        status = "PASS" if result["passed"] else "FAIL"
        print(f"[{status}] {key} ({len(tests)} cases)")
        if not result["passed"]:
            failures.append((key, result["error"], result["stdout"]))
        elif not result["ok"]:
            failures.append(
                (key, {"type": "crash", "message": str(result["error"])}, "")
            )

    print(
        f"\n课程数={len(lessons)}（Python {len(py_lessons)} + JavaScript {len(js_lessons)}"
        f" + TypeScript {len(ts_lessons)} + C {len(c_lessons)} + Lua {len(lua_lessons)} + SQL {len(sql_lessons)}；"
        f"JS / TS / C / Lua / SQL 判题分别由 test_js_lessons / TypeScriptCoursePacksTest / CCoursePacksTest"
        f" / LuaCoursePacksTest / test_sql_lessons 执行）"
        f" 挑战数={len(challenges)}（Python {len(py_challenges)} 道，其余语言由各自测试覆盖）"
        f" 判题项={checked} 用例总数={total_cases}"
    )
    print(f"结构问题={len(problems)} 判题失败={len(failures)}")
    for p in problems:
        print("STRUCT:", p)
    for key, err, out in failures:
        print("---", key, err, "\nstdout:", out)
    sys.exit(1 if (failures or problems) else 0)


if __name__ == "__main__":
    main()

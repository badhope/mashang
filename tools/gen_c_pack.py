import json
import pathlib


def lesson(id_, order, chapter, chapter_name, title, subtitle, xp, blocks, exercise):
    return {
        "id": id_,
        "order": order,
        "chapter": chapter,
        "chapterName": chapter_name,
        "lang": "c",
        "title": title,
        "subtitle": subtitle,
        "xp": xp,
        "blocks": blocks,
        "exercise": exercise,
    }


C = "语言包 · C 启程"

c_lessons = [
    lesson("c01", 700, 34, C, "第一次握手", "printf 与 main", 60, [
        {"type": "heading", "text": "每个 C 程序都从 main 开始"},
        {"type": "text", "text": "C 是编译型语言：你写的代码要先变成机器指令才能跑。但程序入口永远只有一个——main 函数。操作系统启动你的程序时，第一件事就是找到 main 然后跳进去。return 0 表示「正常结束」。"},
        {"type": "diagram", "text": "你写的源码            编译器处理后\n───────────          ──────────────\nint main() { ... }  →  入口点 main\nreturn 0;           →  退出码 0（成功）"},
        {"type": "code", "code": "int main() {\n    printf(\"hello, night city\\n\");\n    return 0;\n}", "runnable": True},
        {"type": "output", "text": "hello, night city"},
        {"type": "tip", "text": "printf 里的 \\n 是换行符。C 不会自动换行——这正是它比 print 更需要小心的原因。"},
        {"type": "table", "headers": ["转义符", "含义", "示例输出"], "rows": [["\\n", "换行", "第二行另起"], ["\\t", "制表符", "对齐用"], ["\\\\", "反斜杠本身", "\\"], ["\\\"", "双引号", "\""]]},
        {"type": "quiz", "question": "C 程序的入口函数是？", "options": ["start()", "main()", "run()", "init()"], "answer": 1, "explain": "C 运行时固定从 main 开始执行，这是语言规范，不是习惯。"},
        {"type": "task", "text": "把问候语换成你自己的代号，重新运行。"},
    ], {
        "title": "打印入网代号",
        "brief": "在 main 里打印 'HELLO, V'（不带换行）。",
        "starterCode": "int main() {\n    // 打印 HELLO, V\n\n    return 0;\n}",
        "hint": "printf(\"HELLO, V\");",
        "tests": [
            # 判题断言的是「用户 main 的真实输出」，不是让测试自己打印。
            # 否则用户什么都没写也能通过（之前踩过的坑）。
            "int main() { assertOutput(\"HELLO, V\", \"应打印 HELLO, V\"); return 0; }",
        ],
        "stdin": [],
        "xp": 80,
        "solution": "int main() {\n    printf(\"HELLO, V\");\n    return 0;\n}\n",
    }),

    lesson("c02", 701, 34, C, "变量与类型", "int / double / char", 70, [
        {"type": "heading", "text": "C 要求你明说类型"},
        {"type": "text", "text": "Python 里 x = 5 就够了，C 必须写 int x = 5;。这不是啰嗦——编译器要据此决定分配几个字节、怎么解释这段内存。类型一旦定下就不能改。"},
        {"type": "code", "code": "int main() {\n    int level = 66;\n    double rate = 1.5;\n    char tag = 'V';\n\n    printf(\"level=%d\\n\", level);\n    printf(\"rate=%.1f\\n\", rate);\n    printf(\"tag=%c\\n\", tag);\n    return 0;\n}", "runnable": True},
        {"type": "output", "text": "level=66\nrate=1.5\ntag=V"},
        {"type": "table", "headers": ["类型", "装什么", "printf 占位符"], "rows": [["int", "整数", "%d"], ["double", "小数", "%f / %.2f"], ["char", "单个字符", "%c"], ["void", "无", "—"]]},
        {"type": "warn", "text": "整数除法会截断：7 / 2 得到 3，不是 3.5。想要小数必须让其中一个是 double，例如 7.0 / 2。"},
        {"type": "quiz", "question": "`int a = 7 / 2;` 里 a 的值是？", "options": ["3.5", "3", "4", "报错"], "answer": 1, "explain": "两个 int 相除，结果还是 int，小数部分直接丢掉。想得到 3.5 要写 7.0 / 2。"},
    ], {
        "title": "平均分计算",
        "brief": "实现 average(a, b)：返回两个整数的平均值（用 double）。",
        "starterCode": "double average(int a, int b) {\n    // 返回平均值，注意不要整数截断\n    return 0;\n}",
        "hint": "把其中一个转成 double：(a + b) / 2.0，或 (double)(a + b) / 2。",
        "tests": [
            "int main() { assert(average(4, 6) == 5.0, \"average(4,6) 应为 5.0\"); return 0; }",
            "int main() { assert(average(3, 4) == 3.5, \"average(3,4) 应为 3.5，别整数截断\"); return 0; }",
        ],
        "stdin": [],
        "xp": 90,
        "solution": "double average(int a, int b) {\n    return (a + b) / 2.0;\n}\n",
    }),

    lesson("c03", 702, 34, C, "条件与循环", "if / while / for", 70, [
        {"type": "heading", "text": "流程控制三件套"},
        {"type": "text", "text": "if 判断、while 不知道次数时用、for 知道次数时用。C 的 for 把「初始化、条件、步进」三件事写在一行里，是它最典型的紧凑风格。"},
        {"type": "code", "code": "int main() {\n    int sum = 0;\n    for (int i = 1; i <= 5; i++) {\n        sum = sum + i;\n    }\n    printf(\"sum=%d\\n\", sum);\n\n    int n = 3;\n    while (n > 0) {\n        printf(\"%d\", n);\n        n--;\n    }\n    printf(\"\\n\");\n    return 0;\n}", "runnable": True},
        {"type": "output", "text": "sum=15\n321"},
        {"type": "diagram", "text": "for (int i = 1; i <= 5; i++)\n     │        │       │\n     │        │       └─ 每轮结束执行\n     │        └───────── 每轮开始检查\n     └────────────────── 只执行一次"},
        {"type": "tip", "text": "i++ 是 i = i + 1 的简写。放在变量后面叫「后置自增」，返回的是加之前的值。"},
        {"type": "quiz", "question": "`for (int i = 0; i < 3; i++)` 循环几次？", "options": ["2 次", "3 次", "4 次", "无限"], "answer": 1, "explain": "i 取 0、1、2 时条件成立，共 3 次；i=3 时 3<3 为假，退出。"},
    ], {
        "title": "累加器",
        "brief": "实现 sum_to(n)：返回 1 到 n 的和（n < 1 返回 0）。",
        "starterCode": "int sum_to(int n) {\n    // 返回 1+2+...+n\n    return 0;\n}",
        "hint": "先判断 n < 1 直接返回 0，否则用 for 累加。",
        "tests": [
            "int main() { assert(sum_to(5) == 15, \"sum_to(5) 应为 15\"); return 0; }",
            "int main() { assert(sum_to(1) == 1, \"sum_to(1) 应为 1\"); return 0; }",
            "int main() { assert(sum_to(0) == 0, \"sum_to(0) 应为 0\"); return 0; }",
        ],
        "stdin": [],
        "xp": 90,
        "solution": "int sum_to(int n) {\n    if (n < 1) return 0;\n    int s = 0;\n    for (int i = 1; i <= n; i++) s = s + i;\n    return s;\n}\n",
    }),

    lesson("c04", 703, 34, C, "数组", "连续内存与下标", 80, [
        {"type": "heading", "text": "数组就是一块连续内存"},
        {"type": "text", "text": "int a[5] 向系统要 5 个 int 的空间，它们紧挨着排在一起。下标从 0 开始——a[0] 是第一个，a[4] 是最后一个。C 不做越界检查，越界会写到别人的内存里，这是 C 最危险的特性之一。"},
        {"type": "code", "code": "int main() {\n    int a[4];\n    a[0] = 10;\n    a[1] = 20;\n    a[2] = 30;\n    a[3] = 40;\n\n    int sum = 0;\n    for (int i = 0; i < 4; i++) {\n        sum = sum + a[i];\n    }\n    printf(\"sum=%d\\n\", sum);\n    return 0;\n}", "runnable": True},
        {"type": "output", "text": "sum=100"},
        {"type": "diagram", "text": "int a[4]\n┌────┬────┬────┬────┐\n│ 10 │ 20 │ 30 │ 40 │\n└────┴────┴────┴────┘\na[0]  a[1]  a[2]  a[3]   ← 下标从 0 开始"},
        {"type": "warn", "text": "访问 a[4] 或 a[-1] 不会报错，而是读写到数组外面的内存。本课程的解释器会拦下越界，但真实 C 不会——写 C 必须自己数清楚。"},
        {"type": "quiz", "question": "`int a[5]` 里最后一个元素是？", "options": ["a[5]", "a[4]", "a[6]", "a[0]"], "answer": 1, "explain": "长度为 5 的数组下标是 0..4，a[4] 是最后一个。a[5] 已经越界。"},
    ], {
        "title": "数组求和",
        "brief": "实现 sum(int a[], int n) 的等价逻辑：本课程不支持数组参数，改用固定长度求和函数。",
        "starterCode": "// 计算 4 个整数的和（参数按值传递，本课程不支持数组参数）\nint sum4(int a, int b, int c, int d) {\n    return 0;\n}",
        "hint": "直接 return a + b + c + d;",
        "tests": [
            "int main() { assert(sum4(1,2,3,4) == 10, \"sum4(1,2,3,4) 应为 10\"); return 0; }",
            "int main() { assert(sum4(0,0,0,0) == 0, \"全 0 应为 0\"); return 0; }",
            "int main() { assert(sum4(-1,1,0,5) == 5, \"含负数应正确\"); return 0; }",
        ],
        "stdin": [],
        "xp": 100,
        "solution": "int sum4(int a, int b, int c, int d) {\n    return a + b + c + d;\n}\n",
    }),

    lesson("c05", 704, 34, C, "函数与递归", "值传递与调用栈", 80, [
        {"type": "heading", "text": "函数是 C 的组织单位"},
        {"type": "text", "text": "C 的参数是「按值传递」：函数拿到的是副本，改副本不影响外面的变量。这跟 Python 里改列表会影响外面完全不同，是初学 C 最容易困惑的地方。"},
        {"type": "code", "code": "int add(int a, int b) {\n    return a + b;\n}\n\nint fact(int n) {\n    if (n <= 1) return 1;\n    return n * fact(n - 1);\n}\n\nint main() {\n    printf(\"add=%d\\n\", add(3, 4));\n    printf(\"fact=%d\\n\", fact(5));\n    return 0;\n}", "runnable": True},
        {"type": "output", "text": "add=7\nfact=120"},
        {"type": "diagram", "text": "fact(3)\n  └─ 3 * fact(2)\n        └─ 2 * fact(1)\n              └─ 1  ← 触底\n     结果逐层返回：1 → 2 → 6"},
        {"type": "warn", "text": "递归必须有「触底条件」（这里是 n <= 1），否则会无限调用直到栈溢出。"},
        {"type": "quiz", "question": "C 的函数参数默认是什么传递方式？", "options": ["引用传递", "按值传递（副本）", "按需传递", "指针传递"], "answer": 1, "explain": "C 只有按值传递。想改外面的变量必须传指针——而指针不在本课程范围内。"},
    ], {
        "title": "阶乘",
        "brief": "实现 factorial(n)：返回 n!（n < 0 返回 0）。",
        "starterCode": "int factorial(int n) {\n    // n! = n * (n-1) * ... * 1\n    return 0;\n}",
        "hint": "用递归：n <= 1 时返回 1，否则 return n * factorial(n - 1)。注意先处理 n < 0。",
        "tests": [
            "int main() { assert(factorial(5) == 120, \"factorial(5) 应为 120\"); return 0; }",
            "int main() { assert(factorial(0) == 1, \"0! 应为 1\"); return 0; }",
            "int main() { assert(factorial(-1) == 0, \"负数应返回 0\"); return 0; }",
        ],
        "stdin": [],
        "xp": 110,
        "solution": "int factorial(int n) {\n    if (n < 0) return 0;\n    if (n <= 1) return 1;\n    return n * factorial(n - 1);\n}\n",
    }),

    lesson("c06", 705, 34, C, "C 的边界", "为什么不支持指针与结构体", 90, [
        {"type": "heading", "text": "知道边界，才知道自己学到哪"},
        {"type": "text", "text": "本课程是「C 教学子集」：用真实解释器跑你的代码，但只覆盖入门部分。指针、结构体、预处理、switch 都不支持——不是偷懒，而是这些特性要么依赖真实内存模型（指针需要地址），要么需要完整编译器前端。"},
        {"type": "table", "headers": ["特性", "本课程", "为什么"], "rows": [["int / double / char", "支持", "基础类型，直接映射"], ["数组", "支持（一维）", "连续内存可模拟"], ["函数与递归", "支持", "调用栈可模拟"], ["指针", "不支持", "需要真实内存地址"], ["结构体", "不支持", "需要内存布局计算"], ["#include / #define", "不支持", "需要预处理器"], ["switch / goto", "不支持", "课程未覆盖，可用 if 替代"]]},
        {"type": "diagram", "text": "你在这里 ──► 入门语法 · 数组 · 函数 · 递归\n                    │\n                    ▼\n            指针 / 结构体 / 内存管理\n            （需要真实编译器 + 内存模型）"},
        {"type": "code", "code": "// 不支持的写法会得到明确提示，而不是晦涩的语法错误\nint main() {\n    int a = 1;\n    int b = 2;\n    int bigger = (a > b) ? a : b;   // 三元运算符是支持的\n    printf(\"bigger=%d\\n\", bigger);\n    return 0;\n}", "runnable": True},
        {"type": "output", "text": "bigger=2"},
        {"type": "tip", "text": "遇到「暂不支持」的提示不要当成 bug——那是课程在告诉你：这个特性需要真实编译器，等你上手 PC 上的 gcc 时再学。"},
        {"type": "quiz", "question": "为什么本课程不支持指针？", "options": ["指针太难", "需要真实内存地址模型", "性能问题", "没必要"], "answer": 1, "explain": "指针的值是内存地址，而解释器没有真实内存地址可给——这是实现层面的硬约束，不是难度取舍。"},
    ], {
        "title": "三者取最大",
        "brief": "实现 max3(a, b, c)：返回三个整数中的最大值。",
        "starterCode": "int max3(int a, int b, int c) {\n    // 返回三者最大\n    return 0;\n}",
        "hint": "先用 if 比较 a 和 b 得到较大值，再和 c 比较；也可以用三元运算符。",
        "tests": [
            "int main() { assert(max3(1, 5, 3) == 5, \"max3(1,5,3) 应为 5\"); return 0; }",
            "int main() { assert(max3(9, 2, 4) == 9, \"max3(9,2,4) 应为 9\"); return 0; }",
            "int main() { assert(max3(-1, -5, -3) == -1, \"负数也要正确\"); return 0; }",
        ],
        "stdin": [],
        "xp": 110,
        "solution": "int max3(int a, int b, int c) {\n    int m = a;\n    if (b > m) m = b;\n    if (c > m) m = c;\n    return m;\n}\n",
    }),
]

pathlib.Path("content_packs/c-basics.json").write_text(
    json.dumps(c_lessons, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n"
)
print("c", len(c_lessons), "lessons,", sum(len(x["exercise"]["tests"]) for x in c_lessons), "cases")

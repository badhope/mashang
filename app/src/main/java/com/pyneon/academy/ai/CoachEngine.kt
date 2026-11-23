package com.pyneon.academy.ai

/**
 * 离线规则教练。不联网也不用模型，只看错误类型和消息，
 * 给出问题判断、几步提示和一小段示例。
 *
 * 原则是只提示不代写：语言报错给通用修法，判题不过只给检查清单，不直接给答案。
 */
data class CoachTip(
    val title: String,
    val summary: String,
    val steps: List<String>,
    val example: String?,
    val lineRef: Int?
)

object CoachEngine {

    private val LINE_PATTERN = Regex("""[Ll]ine (\d+)""")

    fun analyze(
        errorType: String?,
        errorMessage: String?,
        traceback: String?,
        code: String
    ): CoachTip {
        val type = (errorType ?: "").trim()
        val message = (errorMessage ?: "").trim()
        // 抓第一个 Line N，通常就是用户代码那一帧。取最后一个容易抓到库内部。
        val lineRef = LINE_PATTERN.findAll(traceback ?: "").firstOrNull()
            ?.groupValues?.get(1)?.toIntOrNull()
        // 行号只在这里算一次，下面统一由 tip 带上。
        fun tip(title: String, summary: String, steps: List<String>, example: String?): CoachTip =
            CoachTip(title, summary, steps, example, lineRef)

        // 判题不过就列自查项，不给答案。
        if (type == "TestFailed" || message.contains("用例未通过") || type == "AssertionError") {
            return tip(
                "判题没过：先别改代码，先对答案",
                "判题器用断言逐条检查你的函数。失败信息里是第一个没通过的断言，它就是你现在的目标。",
                listOf(
                    "读出最后一条断言期望什么（例如 assert xxx == 5），用 print() 打印你的函数实际返回值",
                    "把函数能想到的边界值单独试一遍：0、1、负数、空列表、重复元素",
                    "检查是不是只覆盖了题目说的那一条路径，漏掉了 return 分支",
                    "对照题目 brief 里的样例输入输出，是否一一对应"
                ),
                null
            )
        }
        // 代码超时
        if (type.contains("Timeout")) {
            return tip(
                "代码超时：几乎总是死循环",
                "运行超过时限被强制中断，最常见的凶手是 while/for 的退出条件永远达不到。",
                listOf(
                    "找到正在执行的循环，问自己：它的退出条件一定会变成 False 吗？",
                    "循环体里有没有让条件逐步靠近 False 的语句（计数器 + 1、索引 + 1、更新变量）",
                    "把数据规模先换小（如输入 5）跑一遍，确认小数据能正常结束",
                    "别急着重写，先在循环里 print(当前进度)，看它到底有没有推进"
                ),
                "n = 5\nwhile n > 0:\n    print(n)\n    n -= 1   # 少了这行就是死循环"
            )
        }
        // 运行台不支持 input
        if (type.contains("InputError")) {
            return tip(
                "运行台不支持 input()",
                "课堂判题环境预置了测试数据，交互式 input() 在这里不可用。",
                listOf(
                    "把测试用数据直接写死在代码里（赋值给变量），再跑判题",
                    "想练习交互输入，请去「终端」里运行",
                    "判题只看你的输出是否匹配断言，固定数据不影响结果"
                ),
                "name = 'neo'   # 代替 name = input()"
            )
        }
        // 缩进
        if (type.contains("IndentationError")) {
            return tip(
                "缩进错误：Python 用缩进表示代码块",
                "if / for / def / while 后面冒号结束的那一行，下一行必须多缩进一层。",
                listOf(
                    "看报错行和它上面一行：上面是不是以冒号结尾？是，则这行要缩进",
                    "同一个代码块内统一用空格（别混 Tab 和空格）",
                    "多套一层就多缩进 4 空格，层数别多也别少"
                ),
                "if x > 0:\n    print(x)   # 必须缩进"
            )
        }
        // 语法
        if (type.contains("SyntaxError")) {
            return tip(
                "语法错误：编译器读不懂这一行",
                "多半是少了配对的括号、冒号或引号，或中英文符号混用。",
                listOf(
                    "看报错行和它上一行，哪个括号/引号/冒号没配对",
                    "中文输入法打出的括号（）逗号，都不是合法 Python 符号",
                    "字符串两端的引号必须一模一样"
                ),
                "print('hello')   # 半角括号与引号"
            )
        }
        // 名字没定义
        if (type.contains("NameError")) {
            val name = Regex("'([^']+)'").find(message)?.groupValues?.get(1) ?: "这个变量"
            return tip(
                "名字未定义：Python 不认识这条名字",
                "NameError 表示这个名字还没被赋值过，或拼写与大小写不一致。",
                listOf(
                    "检查是不是多了空格/大小写错字（score 和 scroe 是两个名字）",
                    "确认它在使用之前被赋值；函数内要用外部变量，请作为参数传入或 return 返回",
                    "没有定义就补上定义，或换用已存在的变量"
                ),
                "score = 100\nprint(score)   # 别把 score 写成 scroe"
            )
        }
        // 类型对不上
        if (type.contains("TypeError")) {
            return tip(
                "类型错误：不同类型的值不能直接运算",
                "Python 对类型很严格：字符串与数字不能 +、len() 只接受容器、下标只接受整数。",
                listOf(
                    "看报错的操作两边的类型是什么（print(type(x)) 最快）",
                    "数字与字符串要转换：int() / str()，再运算",
                    "报 not subscriptable 说明你对不可以下标的类型用了 [i]"
                ),
                "'1' + 1 改成 int('1') + 1，或者 '1' + '1'"
            )
        }
        // 下标越界
        if (type == "IndexError") {
            return tip(
                "下标越界：容器没那么多位置",
                "列表/字符串下标从 0 开始，最大可用下标是 len(x) - 1。",
                listOf(
                    "看一下用到的下标是不是大于等于 len(容器)",
                    "循环里 range(len(x)) 安全；range(len(x) + 1) 就会越界",
                    "取末尾可以用负数下标 x[-1]"
                ),
                "x = [1, 2, 3]\nprint(x[2])   # 合法；x[3] 越界"
            )
        }
        // 键不存在
        if (type == "KeyError") {
            val key = Regex("'([^']+)'").find(message)?.groupValues?.get(1) ?: "这个键"
            return tip(
                "键不存在：字典里没有这条键",
                "KeyError 表示你用 [key] 去取一个并不存在的键。",
                listOf(
                    "检查键名拼写（含大小写）是否与写入时一致",
                    "用 dict.get(key, 默认值) 代替 [key]，不存在时返回默认值而不是报错",
                    "不确定就先 if key in d: 再取"
                ),
                "d = {'a': 1}\nprint(d.get('b', 0))   # 返回 0 而不是报错"
            )
        }
        // 除以零
        if (type == "ZeroDivisionError") {
            return tip(
                "除数为 0：数学上不允许",
                "某处做了 x / y，而 y 此刻为 0。",
                listOf(
                    "定位分母变量，想清楚它什么情况下会等于 0",
                    "除之前先判断：if y == 0: 给个默认结果",
                    "或者用 try / except ZeroDivisionError 接住再给提示"
                ),
                "if y != 0:\n    result = x / y\nelse:\n    result = None"
            )
        }
        // 属性不存在
        if (type.contains("AttributeError")) {
            return tip(
                "对象上没有这个成员",
                "AttributeError 表示你访问的方法/属性在这类对象上不存在。",
                listOf(
                    "确认拼写是否正确（strp 不是 strip、appen 不是 append）",
                    "方法要加括号调用，属性不加",
                    "确认对象类型是你以为的那个（print(type(x))）"
                ),
                "s = 'neon'\ns.strip()   # 方法记得加 ()"
            )
        }
        // 值不合法
        if (type == "ValueError") {
            return tip(
                "值不合法：转换或拆包失败",
                "ValueError 常见于 int('abc')、解包数量不匹配、容器查找/索引值不在范围。",
                listOf(
                    "int()/float() 不可以的写法：先确保内容能转换",
                    "if 判断边界后再转换，避免对空串/特殊字符转换",
                    "解包 a, b = ... 时确认右侧正好 2 个元素"
                ),
                "if s.isdigit():\n    n = int(s)"
            )
        }
        // 文件找不到
        if (type == "FileNotFoundError") {
            return tip(
                "文件不存在：路径写错了或文件还没创建",
                "open() 的路径指向一个不存在的文件。课堂环境里文件系统是受限的。",
                listOf(
                    "确认文件名拼写与扩展名（.txt / .csv）完全一致",
                    "路径是相对路径时，确认文件真的在当前目录；先写出文件再读取",
                    "课堂环境不建议依赖外部文件，练习时可用字符串代替文件内容"
                ),
                "with open('data.txt', 'w') as f:\n    f.write('hello')   # 先写后读"
            )
        }
        // 导入失败
        if (type == "ImportError" || type == "ModuleNotFoundError") {
            return tip(
                "导入失败：模块不存在或拼写错误",
                "import 的模块名拼错了、未安装，或把文件名当成了模块。",
                listOf(
                    "核对模块名拼写（random、math、json 都是内置的）",
                    "自己写的文件不要起名 random.py / math.py，会顶掉内置模块",
                    "不要 import 不存在或课堂未提供的第三方包"
                ),
                "import random   # 内置模块，直接可用"
            )
        }
        // 递归太深
        if (type == "RecursionError") {
            return tip(
                "递归太深：函数无限调用自己",
                "RecursionError 表示函数调用自己一直没有终止条件，栈被耗尽。",
                listOf(
                    "找函数的递归出口：什么时候不再调用自己？",
                    "递归参数每次都要向出口靠近（如 n-1、列表变短）",
                    "出口条件写太晚/太宽都会触发；先想清楚最小规模（如 n=0）会发生什么"
                ),
                "def f(n):\n    if n <= 0: return 0   # 出口\n    return f(n - 1)   # 向出口靠近"
            )
        }
        // 其他未知错误
        return if (message.isNotBlank()) {
            tip(
                "报错信息定位",
                "错误类型（$type）。信息原文就是最好的线索，先读最后一行。",
                listOf(
                    "把报错信息里提到的行号和代码对应起来，重点看那一行",
                    "用 print(type(...)) 检查变量的实际类型",
                    "把问题简化成最小示例单独跑，缩小范围"
                ),
                null
            )
        } else {
            tip(
                "暂无可用诊断",
                "这次没有任何报错信息，说明问题在结果本身。",
                listOf(
                    "把实际输出和题目期望输出逐字符对比（包括空格与换行）",
                    "对照 brief 的示例输入输出完整复核一遍",
                    "重新读一遍题目，确认没有遗漏的格式要求"
                ),
                null
            )
        }
    }

}
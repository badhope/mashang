#!/usr/bin/env python3
"""生成 JavaScript「专业篇」课程包（8 讲，js31 - js38）。

运行：python tools/gen_js_expert.py
产物：content_packs/js-expert.json

课程面向 PyNeon 内置的 Mozilla Rhino 1.7.15（解释模式，ES6 子集）。
生成器内置结构自检：id/order/chapter、块数量与类型、正文汉字量、
quiz 答案范围、练习字段与用例数量、禁用语法扫描。任一不满足直接抛错，
不会写出半成品。

设计说明：
- 既有三包（js-basics / js-core / js-advanced）已讲完语言主体与常见算法，
  本包只讲工程能力，每讲一个明确技能点，内容不与前 30 讲重复。
- 所有代码按 Rhino 1.7.15 的真实能力编写：无 class / 展开 / 默认参数 /
  async-await / 可选链 / 空值合并 / import / require / setTimeout / DOM。
- Rhino 的 let 在 for 循环中不产生每次迭代的新绑定（实测），因此闭包捕获
  一讲统一用工厂函数演示，不教「用 let 就好」。
"""

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "content_packs" / "js-expert.json"

CHAPTER = 24
CHAPTER_NAME = "JavaScript · 专业篇"
FIRST_ID = 31
FIRST_ORDER = 330


# ---------------------------------------------------------------------------
# 块构造器（与 gen_js_curriculum.py 保持同一风格）
# ---------------------------------------------------------------------------

def heading(text):
    return {"type": "heading", "text": text}


def paragraph(text):
    return {"type": "text", "text": _flow(text)}


def _flow(text):
    """把长段落按句号切成 3 个子段，贴近既有课程的排版（不改变汉字总量）。"""
    if "\n\n" in text or len(text) <= 300:
        return text
    sentences = re.findall(r"[^。！？]*[。！？]", text)
    if "".join(sentences) != text:
        return text
    if len(sentences) < 3:
        return text
    target = len(text) / 3.0
    chunks = []
    current = ""
    for sentence in sentences:
        current += sentence
        if len(current) >= target and len(chunks) < 2:
            chunks.append(current)
            current = ""
    chunks.append(current)
    return "\n\n".join(chunks)


def code(src):
    return {"type": "code", "code": src.strip("\n"), "runnable": True}


def output(text):
    return {"type": "output", "text": text.strip("\n")}


def table(headers, rows):
    return {"type": "table", "headers": headers, "rows": rows}


def diagram(text):
    return {"type": "diagram", "text": text.strip("\n")}


def tip(text):
    return {"type": "tip", "text": text}


def warn(text):
    return {"type": "warn", "text": text}


def quiz(question, options, answer, explain):
    return {
        "type": "quiz",
        "question": question,
        "options": options,
        "answer": answer,
        "explain": explain,
    }


def practice(title, src, expected, hint):
    return {
        "type": "practice",
        "title": title,
        "code": src.strip("\n"),
        "output": expected.strip("\n"),
        "hint": hint,
    }


def steps(items):
    return {"type": "steps", "items": items}


def pair(src, expected):
    """一个可运行代码块 + 紧随其后的真实输出块。"""
    return [code(src), output(expected)]


def exercise(title, brief, starter, hint, tests, solution, xp=120):
    return {
        "title": title,
        "brief": brief,
        "starterCode": starter.strip("\n"),
        "hint": hint,
        "tests": tests,
        "stdin": [],
        "xp": xp,
        "solution": solution.strip("\n"),
    }


def lesson(id_, order, title, subtitle, xp, blocks, ex):
    return {
        "id": id_,
        "order": order,
        "chapter": CHAPTER,
        "chapterName": CHAPTER_NAME,
        "lang": "javascript",
        "title": title,
        "subtitle": subtitle,
        "xp": xp,
        "blocks": blocks,
        "exercise": ex,
    }


# ---------------------------------------------------------------------------
# js31 函数式编程：纯函数、不可变、组合 compose / pipe
# ---------------------------------------------------------------------------

def js31():
    return lesson(
        "js31", 330,
        "函数式编程：组合的艺术",
        "纯函数 · 不可变 · compose / pipe",
        130,
        [
            heading("把小函数串成流水线"),
            paragraph(
                "函数式的核心不是术语，而是把计算拆成可独立验证的小步骤。纯函数是地基：同样的输入永远同样的输出，"
                "不读写外部状态；js27 已经把纯函数与不可变的判断标准讲透，js17 的 compose2 也只能接两个函数；这一讲往前一步，解决任意长度的组合、方向选择与管道调试。组合的前提是"
                "一元函数——只接收一个参数的函数最容易串联。多参数函数先用闭包固定一部分参数，变成一元函数，再放进"
                "流水线。compose 从右往左执行，pipe 从左往右执行；数学表达式习惯用 compose，业务代码为了阅读顺序通常"
                "用 pipe。组合的结果仍然是一个函数，可以继续参与组合，这就是结合律：先组合哪两个不影响最终结果。"
                "方向固定之后，调试也有了抓手：可以在任意两步之间插入观察点，看数据长什么样。反过来，如果每个函数都"
                "依赖外部变量，组合就失去意义，因为每一步的输出都不可预测。引用透明还有一个实际好处：纯函数的结果可以安全缓存，参数相同就直接复用，不必担心缓存过期。柯里化与偏应用常被混为一谈：柯里化把 f(a, b, c) 拆成 f(a)(b)(c)，偏应用只固定前几个参数，用 bind 或闭包就能实现。管道里通常只需要偏应用，不必追求严格的柯里化；point-free 风格写起来像一句话，但过度使用会让栈追踪变难。"
            ),
            paragraph(
                "管道有两种实现。循环版取出 arguments 里的函数列表，按方向逐个调用，代码直白，适合任意数量；"
                "reduce 版把函数数组折叠成一个复合函数，compose 用 reduceRight，pipe 用 reduce，初始值必须是一个"
                "「原样返回输入」的函数，否则空管道会得到 undefined。两种写法等价，选可读性更好的那种。组合的边界是"
                "副作用：日志、随机数、当前时间、网络请求都不应该藏在管道中间，否则同一段管道每次结果不同，也无法"
                "测试。需要观察中间值时用 tap：它执行一个副作用，然后把原值原样传下去，既不改变数据流，又能在关键"
                "位置打印。另一个边界是参数数量，组合只适合一元函数；需要两个参数时，用闭包先固定一个，或者把参数"
                "打包成一个对象再拆开。用 reduce 实现 compose 时顺序容易写反：reduceRight 从最后一个函数开始，acc 是已经组合好的部分，fn 是更靠前的函数，所以返回 fn(acc(value))。写完先用两个函数验证方向，再扩展到多个。管道里每一步都该做参数校验，把非法输入挡在入口，而不是让它在第三步才炸出一个难以定位的 undefined。函数命名也要具体：transform 不如 normalizePrice 清楚，组合的可读性一半来自函数名。"
            ),
            paragraph(
                "组合不是越多越好。三个以上的步骤应该给整条管道起个名字，否则调用处是一长串函数名，读起来比命令式"
                "循环更累。管道适合数据转换：解析、校验、归一化、格式化，每一步都能单独测试；不适合有复杂分支、需要"
                "提前退出、或者要维护中间状态的流程。性能上每次组合多一层函数调用，但相对业务开销可以忽略；真正要"
                "避免的是在管道里反复遍历大数组。判断标准很简单：每一步能否单独命名、单独断言。能，就适合组合；"
                "不能，就老老实实写循环。如果一段逻辑需要用 try/catch 包住中间某一步，它更适合写成普通函数，而不是硬塞进管道；需要索引或相邻元素的滑动窗口也不适合。函数式是一种组织手段，不是信仰；团队里有人不熟悉时，先用注释标出每一步的输入输出，再逐步替换。性能上组合产生的闭包很小，但每次调用都会新建作用域，极端热路径可以手动展开成顺序调用。"
            ),
            *pair(
                r"""
function pipe() {
  var fns = Array.prototype.slice.call(arguments);
  return function (value) {
    var out = value;
    for (var i = 0; i < fns.length; i++) {
      out = fns[i](out);
    }
    return out;
  };
}

function compose() {
  var fns = Array.prototype.slice.call(arguments);
  return function (value) {
    var out = value;
    for (var i = fns.length - 1; i >= 0; i--) {
      out = fns[i](out);
    }
    return out;
  };
}

function trim(s) { return s.trim(); }
function lower(s) { return s.toLowerCase(); }
function dash(s) { return s.replace(/ /g, '-'); }

var slug = pipe(trim, lower, dash);
var slug2 = compose(dash, lower, trim);
console.log(slug('  Hello Neon City  '));
console.log(slug2('  Hello Neon City  '));
""",
                "hello-neon-city\nhello-neon-city",
            ),
            table(
                ["概念", "含义", "本讲用法"],
                [
                    ["纯函数", "同输入同输出、无副作用", "管道里的每一步"],
                    ["组合", "把多个一元函数接成新函数", "compose / pipe"],
                    ["方向", "compose 右到左，pipe 左到右", "按阅读习惯选"],
                    ["结合律", "分组方式不影响结果", "可以分段命名"],
                    ["tap", "观察中间值但不改变数据", "调试管道"],
                ],
            ),
            *pair(
                r"""
function compose2() {
  var fns = Array.prototype.slice.call(arguments);
  return fns.reduceRight(function (acc, fn) {
    return function (value) { return fn(acc(value)); };
  }, function (value) { return value; });
}

function tap(label, fn) {
  return function (value) {
    fn(label, value);
    return value;
  };
}

function addOne(n) { return n + 1; }
function double(n) { return n * 2; }

var run = compose2(String, addOne, double);
console.log(run(5));

var traced = compose2(
  tap('after double', function (label, v) { console.log(label, v); }),
  double
);
console.log(traced(7));
""",
                "11\nafter double 14\n14",
            ),
            tip("空管道要返回原值：reduce 的初始值就是 identity 函数。没有它，pipe()(x) 会得到 undefined。"),
            quiz(
                "compose(f, g)(x) 等价于下面哪一个？",
                ["f(g(x))", "g(f(x))", "f(x) + g(x)", "g(x) + f(x)"],
                0,
                "compose 从右往左，先执行 g，再把结果交给 f。",
            ),
            practice(
                "管道化字符串",
                r"""
function pipe() {
  var fns = Array.prototype.slice.call(arguments);
  return function (value) {
    var out = value;
    for (var i = 0; i < fns.length; i++) out = fns[i](out);
    return out;
  };
}

var shout = pipe(
  function (s) { return s.trim(); },
  function (s) { return s.toUpperCase(); },
  function (s) { return s + '!'; }
);
console.log(shout('  hi  '));
""",
                "HI!",
                "trim 去空白，toUpperCase 转大写，最后补感叹号。",
            ),
            steps(
                [
                    "先写纯函数，每个只做一次转换",
                    "多参数用闭包固定成一元函数",
                    "选方向：数据流用 pipe，数学式用 compose",
                    "空管道返回 identity，避免 undefined",
                    "用 tap 在关键步骤观察，不改变数据",
                ]
            ),
        ],
        exercise(
            "管道工具",
            "实现 pipe() 与 compose()：都接收任意数量的一元函数，pipe 从左到右、compose 从右到左依次调用；"
            "没有参数时返回原值。再实现 tap(fn)：返回一个函数，收到值后先调用 fn(value)，再原样返回该值。",
            r"""
function pipe() {
  // 取出函数列表，从左到右调用

}

function compose() {
  // 取出函数列表，从右到左调用

}

function tap(fn) {
  // 返回一个包装函数

}

var inc = function (n) { return n + 1; };
var dbl = function (n) { return n * 2; };
console.log(pipe(inc, dbl)(3));
""",
            "用 arguments 取参数，Array.prototype.slice.call(arguments) 转成数组；compose 从末尾往前循环。",
            [
                "var inc1 = function (n) { return n + 1; };\nvar dbl1 = function (n) { return n * 2; };\nassert(pipe(inc1, dbl1)(3) === 8, '从左到右：先加后乘')",
                "assert(compose(inc1, dbl1)(3) === 7, '从右到左：先乘后加')",
                "assert(pipe()(42) === 42 && compose()(42) === 42, '空管道返回原值')",
                "var seen = [];\nvar f = tap(function (v) { seen.push(v); });\nassert(f(9) === 9 && seen.join(',') === '9', 'tap 原样返回')",
                "var trim = function (s) { return s.trim(); };\nvar upper = function (s) { return s.toUpperCase(); };\nvar mark = function (s) { return s + '!'; };\nassert(pipe(trim, upper, mark)('  hi  ') === 'HI!', '三步管道')",
            ],
            r"""
function pipe() {
  var fns = Array.prototype.slice.call(arguments);
  return function (value) {
    var out = value;
    for (var i = 0; i < fns.length; i++) out = fns[i](out);
    return out;
  };
}

function compose() {
  var fns = Array.prototype.slice.call(arguments);
  return function (value) {
    var out = value;
    for (var i = fns.length - 1; i >= 0; i--) out = fns[i](out);
    return out;
  };
}

function tap(fn) {
  return function (value) {
    fn(value);
    return value;
  };
}

var inc = function (n) { return n + 1; };
var dbl = function (n) { return n * 2; };
console.log(pipe(inc, dbl)(3));
""",
        ),
    )


# ---------------------------------------------------------------------------
# js32 设计模式：工厂、单例、观察者、策略（函数与对象）
# ---------------------------------------------------------------------------

def js32():
    return lesson(
        "js32", 331,
        "设计模式",
        "工厂 · 单例 · 观察者 · 策略",
        130,
        [
            heading("模式是重复出现的结构，不是模板代码"),
            paragraph(
                "设计模式的价值首先在于命名：当你说「这里用观察者」，同事立刻知道谁持有订阅列表、谁负责通知、退订"
                "怎么处理。模式描述的是对象之间的关系，不是语法，所以没有 class 也能把经典模式全部实现出来。工厂把"
                "创建细节收进一个函数，调用方只关心参数和返回值；需要多种产品时，工厂内部用一张表按名字选择构造函数，"
                "新增产品只加表项。单例把唯一实例放进闭包，第一次访问时创建，之后复用；外部拿不到构造过程，也就无法"
                "造出第二份状态。工厂和单例经常一起出现：工厂负责创建，缓存负责唯一。两者都不需要 new，也不依赖原型链。工厂的返回值可以是对象、函数或数组，只要满足调用方契约即可；需要缓存实例时，把创建函数换成带 Map 的查找函数，键由参数组合而成。单例要注意初始化顺序：依赖必须在第一次 get 之前准备好，否则会拿到半成品。测试单例最麻烦，因为它跨用例共享状态，通常要提供 reset 方法，或者改成工厂加显式注入。"
            ),
            paragraph(
                "观察者维护一张事件名到回调数组的表，提供三个动作：on 订阅、off 退订、emit 通知。关键细节在退订："
                "回调必须能被精确移除，否则监听器只增不减，最后变成内存泄漏。做法是 on 返回一个取消函数，内部用 "
                "indexOf 找到下标再 splice；或者要求调用方传入同一个函数引用。emit 遍历前要先复制数组：如果回调里"
                "退订了自己，直接遍历原数组会跳过下一个监听器，这个 bug 只在特定顺序下出现，很难复现。策略模式把可"
                "替换的算法抽成函数，放进按名字索引的对象；调用方只依赖名字，不依赖实现。新增策略只加一个键，调用点"
                "不动，长 if 链自然消失。观察者还要考虑异常隔离：一个监听器抛错不应该阻断其他监听器，可以在 emit 里用 try/catch 包住每次调用，把错误收集起来最后统一报告。策略表可以带默认项，未知名字回落到默认策略，调用方就不必到处判空。策略函数最好保持纯：只依赖传入参数，不读外部状态，这样每条策略都能单独测试。策略与工厂也能组合：工厂按配置挑策略，调用方只看到统一接口。"
            ),
            paragraph(
                "四种模式的共同点是把变化点隔离出来：工厂隔离创建，单例隔离全局状态，观察者隔离通知关系，策略隔离"
                "算法选择。它们也都有代价：单例是隐式的全局状态，测试之间会互相污染；观察者让调用链变长，出错时难以"
                "定位；策略表用字符串做键，拼错不会报错，只会在运行时返回 undefined。选择之前先问「这里真正会变的"
                "是什么」：创建方式会变用工厂，实例数量必须唯一用单例，通知对象会增减用观察者，算法会替换用策略。"
                "三行 if 能解决的分支不值得上策略表，模式是工具，不是目标。还要警惕过早抽象：只有一个实现的策略表、只有一个订阅者的观察者，都是负债。模式应该从第二次重复中长出来，而不是第一次就设计好。读别人的代码时，先找模式名对应的结构，再看它解决了什么问题；只记住结构不理解动机，抄出来的代码往往多一层没必要的间接。模式之间可以互相嵌套，但要控制层数，三层以上的间接通常意味着该拆模块了。"
            ),
            *pair(
                r"""
function createLogger(prefix) {
  var lines = [];
  return {
    log: function (msg) {
      lines.push(prefix + ': ' + msg);
      return lines.length;
    },
    all: function () { return lines.slice(); }
  };
}

var a = createLogger('A');
var b = createLogger('B');
a.log('boot');
b.log('idle');
console.log(a.all().join('|'));
console.log(b.all().join('|'));

var config = (function () {
  var instance = null;
  function make() {
    return { mode: 'offline', level: 1 };
  }
  return {
    get: function () {
      if (instance === null) instance = make();
      return instance;
    }
  };
})();

console.log(config.get() === config.get());
console.log(config.get().mode);
""",
                "A: boot\nB: idle\ntrue\noffline",
            ),
            table(
                ["模式", "解决的问题", "JS 实现要点"],
                [
                    ["工厂", "创建逻辑复杂、需要统一入口", "返回对象的函数"],
                    ["单例", "全局只应有一份状态", "闭包 + 惰性初始化"],
                    ["观察者", "一方变化要通知多方", "事件表 + on/off/emit"],
                    ["策略", "算法可替换、避免长 if", "名字到函数的映射表"],
                ],
            ),
            *pair(
                r"""
function createEmitter() {
  var listeners = {};
  return {
    on: function (event, fn) {
      if (!listeners[event]) listeners[event] = [];
      listeners[event].push(fn);
      return function () {
        var list = listeners[event] || [];
        var idx = list.indexOf(fn);
        if (idx !== -1) list.splice(idx, 1);
      };
    },
    emit: function (event, payload) {
      var list = (listeners[event] || []).slice();
      for (var i = 0; i < list.length; i++) {
        list[i](payload);
      }
      return list.length;
    }
  };
}

var bus = createEmitter();
var seen = [];
var off = bus.on('hit', function (n) { seen.push('first:' + n); });
bus.on('hit', function (n) { seen.push('second:' + n); });
console.log(bus.emit('hit', 7));
off();
bus.emit('hit', 8);
console.log(seen.join(','));

var strategies = {
  normal: function (price) { return price; },
  vip: function (price) { return price * 0.8; },
  staff: function (price) { return price * 0.5; }
};

function quote(kind, price) {
  var fn = strategies[kind];
  if (!fn) return null;
  return fn(price);
}
console.log(quote('vip', 100));
console.log(quote('staff', 100));
console.log(quote('ghost', 100));
""",
                "2\nfirst:7,second:7,second:8\n80\n50\nnull",
            ),
            warn("emit 遍历前先 slice 复制，否则回调里退订会让循环跳过下一个监听器；这个 bug 只在「退订自己」时出现，很难复现。"),
            quiz(
                "观察者模式里 on 返回取消函数的主要目的是什么？",
                ["提高 emit 的速度", "让监听器可以被精确移除，避免泄漏", "让事件变成同步", "替代策略模式"],
                1,
                "精确移除需要拿到同一个函数引用；返回取消函数把这件事封装在模块内部。",
            ),
            practice(
                "策略查表",
                r"""
var ops = {
  add: function (a, b) { return a + b; },
  mul: function (a, b) { return a * b; }
};

function calc(kind, a, b) {
  var fn = ops[kind];
  return fn ? fn(a, b) : null;
}

console.log(calc('add', 2, 3), calc('mul', 2, 3), calc('sub', 2, 3));
""",
                "5 6 null",
                "查不到策略时返回 null，而不是抛错，调用方可以决定怎么处理。",
            ),
            steps(
                [
                    "先判断变化点：创建、实例数、通知、算法",
                    "工厂返回对象，内部藏创建细节",
                    "单例用闭包加惰性初始化",
                    "观察者的 on 返回取消函数",
                    "策略用名字到函数的表，未知名字返回空",
                ]
            ),
        ],
        exercise(
            "事件总线",
            "实现 createEmitter()，返回 { on, off, emit, listeners }。on(event, fn) 追加监听器并返回该事件的监听器数量；"
            "off(event, fn) 移除一个匹配的回调，成功返回 true，找不到返回 false；emit(event, payload) 依次调用监听器"
            "并返回调用数量；listeners(event) 返回当前数量。emit 期间回调内部退订不应影响本轮其他监听器。",
            r"""
function createEmitter() {
  // 事件表与四个方法

}

var bus = createEmitter();
bus.on('hit', function (n) { console.log('hit', n); });
console.log(bus.emit('hit', 1));
""",
            "用对象保存事件到数组的映射；off 用 indexOf 找下标再 splice；emit 遍历前用 slice 复制数组。",
            [
                "var bus1 = createEmitter();\nvar f1 = function () {};\nassert(bus1.on('a', f1) === 1, '第一次订阅返回 1');\nassert(bus1.on('a', function () {}) === 2, '第二次订阅返回 2');\nassert(bus1.emit('a', null) === 2, 'emit 返回调用数')",
                "var got = [];\nvar bus2 = createEmitter();\nvar f2 = function (v) { got.push(v); };\nbus2.on('x', f2);\nassert(bus2.off('x', f2) === true && bus2.listeners('x') === 0, '退订成功');\nassert(bus2.off('x', f2) === false, '重复退订返回 false');\nbus2.emit('x', 1);\nassert(got.length === 0, '退订后不再收到')",
                "var sum = 0;\nvar bus3 = createEmitter();\nbus3.on('n', function (v) { sum = sum + v; });\nbus3.emit('n', 3);\nbus3.emit('n', 4);\nassert(sum === 7, 'payload 正确传递')",
                "var calls = [];\nvar bus4 = createEmitter();\nvar first = function () { calls.push('first'); bus4.off('e', first); };\nvar second = function () { calls.push('second'); };\nbus4.on('e', first);\nbus4.on('e', second);\nbus4.emit('e', null);\nassert(calls.join(',') === 'first,second', '回调内退订不影响本轮其他监听器')",
                "var bus5 = createEmitter();\nassert(bus5.listeners('none') === 0 && bus5.emit('none', 1) === 0, '未知事件安全')",
            ],
            r"""
function createEmitter() {
  var listeners = {};

  function listOf(event) {
    if (!listeners[event]) listeners[event] = [];
    return listeners[event];
  }

  return {
    on: function (event, fn) {
      listOf(event).push(fn);
      return listOf(event).length;
    },
    off: function (event, fn) {
      var list = listeners[event] || [];
      var idx = list.indexOf(fn);
      if (idx === -1) return false;
      list.splice(idx, 1);
      return true;
    },
    emit: function (event, payload) {
      var list = (listeners[event] || []).slice();
      for (var i = 0; i < list.length; i++) list[i](payload);
      return list.length;
    },
    listeners: function (event) {
      return (listeners[event] || []).length;
    }
  };
}

var bus = createEmitter();
bus.on('hit', function (n) { console.log('hit', n); });
console.log(bus.emit('hit', 1));
""",
        ),
    )


# ---------------------------------------------------------------------------
# js33 模块化演进：从 IIFE 到 CommonJS 再到 ES Module
# ---------------------------------------------------------------------------

def js33():
    return lesson(
        "js33", 332,
        "模块化演进",
        "IIFE → CommonJS → ES Module",
        130,
        [
            heading("模块的本质是隔离加导出"),
            paragraph(
                "模块要解决两个问题：内部实现不污染外部，外部只能通过公开接口使用。js26 的 IIFE 解决了作用域隔离，"
                "但模块之间的依赖关系仍靠全局变量传递，脚本顺序一错就崩。CommonJS 把每个文件包进一个函数，"
                "注入 exports、module、require 三个参数；文件执行完，module.exports 就是模块的返回值，require 负责"
                "查找、执行、缓存。缓存的副作用是单例：同一个模块被加载两次，拿到的是同一个对象，模块体只执行一次。"
                "这个设计简单有效，代价是同步加载，不适合浏览器里按需拉取。CommonJS 的查找规则值得了解：先看是否核心模块，再沿 node_modules 逐级向上找，最后查缓存；路径解析发生在运行时，所以条件加载是可能的。module.exports 与 exports 起初指向同一个对象，但重新赋值 module.exports 之后 exports 就失效了，这是新手最常踩的坑。循环依赖时，CommonJS 返回的是尚未完成的 exports，调用方可能拿到一个空对象。CommonJS 的缓存键是解析后的绝对路径，同一文件用不同相对路径引用也只加载一次；删掉缓存项才能强制重新执行，这就是测试里 mock 模块的常用手法。"
            ),
            paragraph(
                "ES Module 是语言层面的模块系统：import 与 export 是静态语法，打包工具在编译期就能分析依赖图，因此"
                "可以删掉没有被引用的导出，也就是 tree shaking。它还有实时绑定：导入的是变量的引用而不是快照，导出方"
                "更新后导入方能读到新值；CommonJS 导出的是值拷贝，对象虽然共享引用，但重新赋值不会同步过去。ES Module "
                "默认严格模式，顶层 this 是 undefined，模块只执行一次，循环依赖的处理方式也与 CommonJS 不同。本运行时"
                "没有 require 和 import，但概念必须懂：读打包产物、排查循环依赖、理解单例缓存，都靠这套模型。ES Module 的导出是静态的：不能在 if 里写 export，也不能动态拼模块名；需要动态加载时用 import()，它返回 Promise。静态结构换来的是工具链能力：打包器可以分析依赖、做 tree shaking、检测循环引用。两种模块系统的互操作也有坑：CommonJS 模块被 import 时，默认导出是整个 module.exports 对象；ES Module 被 require 时，通常需要先转译。"
            ),
            paragraph(
                "没有模块系统时，用命名空间对象模拟：一个全局变量挂所有公开方法，内部细节仍放在 IIFE 里。命名空间的"
                "问题是冲突：两个库都叫 utils 就互相覆盖。更稳的做法是注册表：每个模块用唯一名字登记，加载时按名字"
                "取，第一次执行后把导出缓存起来。注册表就是 CommonJS 缓存的手写版，也是本讲代码演示的重点。模块化的"
                "演进方向始终是更强的隔离、更显式的依赖、更早的静态分析；理解这条主线，比记住语法更有用。真实项目里，"
                "模块边界划得好，改一个文件不会牵动十个文件。划分模块的经验法则是按变化频率和变化原因分组：经常一起改的放一起，从不同时改的分开。模块的公开接口越窄越好，内部实现可以自由重构。循环依赖通常说明职责划错了，先找共同依赖抽成第三个模块，而不是用延迟加载掩盖。命名空间对象适合小脚本和演示，长期维护的项目应该用真正的模块系统。模块边界还可以用「公开 API 只有一个入口文件」来强制，内部文件不对外暴露；这样重构时只改入口，调用方不受影响。本课程用注册表演示，是为了让你在没有 import 的环境里也能体会缓存与依赖注入。"
            ),
            *pair(
                r"""
var NeonMath = (function () {
  function square(n) { return n * n; }
  function cube(n) { return n * n * n; }
  return { square: square, cube: cube };
})();

console.log(NeonMath.square(6));
console.log(NeonMath.cube(3));
console.log(typeof NeonMath.square);

var registry = (function () {
  var modules = {};
  function define(name, factory) {
    modules[name] = { factory: factory, exports: null };
  }
  function load(name) {
    var mod = modules[name];
    if (!mod) return null;
    if (mod.exports === null) {
      mod.exports = {};
      mod.factory(mod.exports);
    }
    return mod.exports;
  }
  return { define: define, load: load };
})();

registry.define('counter', function (exports) {
  var count = 0;
  exports.next = function () { count = count + 1; return count; };
});

var c1 = registry.load('counter');
var c2 = registry.load('counter');
console.log(c1 === c2);
console.log(c1.next(), c2.next());
""",
                "36\n27\nfunction\ntrue\n1 2",
            ),
            table(
                ["阶段", "隔离方式", "依赖声明", "加载时机"],
                [
                    ["IIFE", "函数作用域", "全局变量或参数注入", "脚本顺序"],
                    ["命名空间", "单个全局对象", "属性访问", "脚本顺序"],
                    ["CommonJS", "文件级函数包装", "require 同步调用", "运行时首次加载"],
                    ["ES Module", "文件级模块作用域", "import 静态声明", "编译期分析 + 运行时链接"],
                ],
            ),
            *pair(
                r"""
var modules = {};

function define(name, factory) {
  modules[name] = { factory: factory, exports: null, loaded: false };
}

function load(name) {
  var mod = modules[name];
  if (!mod) throw new Error('missing module: ' + name);
  if (!mod.loaded) {
    mod.loaded = true;
    mod.exports = {};
    mod.factory(mod.exports, load);
  }
  return mod.exports;
}

define('math', function (exports) {
  exports.double = function (n) { return n * 2; };
});

define('report', function (exports, requireModule) {
  var math = requireModule('math');
  exports.render = function (n) {
    return 'value=' + math.double(n);
  };
});

var report = load('report');
console.log(report.render(21));
console.log(load('math') === load('math'));
""",
                "value=42\ntrue",
            ),
            tip("模块缓存是单例的根源：同一个模块只执行一次，导出对象被复用。想让每次拿到新状态，导出工厂函数而不是对象。"),
            quiz(
                "CommonJS 里对同一模块第二次 require，会发生什么？",
                ["再执行一次模块体", "返回缓存里的导出对象", "抛出循环依赖错误", "返回 undefined"],
                1,
                "模块首次加载后导出被缓存，后续 require 直接返回同一份对象，模块体不再执行。",
            ),
            practice(
                "惰性注册表",
                r"""
var count = 0;
var reg = {};

function define(name, fn) { reg[name] = { fn: fn, out: null }; }
function load(name) {
  var m = reg[name];
  if (m.out === null) {
    count = count + 1;
    m.out = m.fn();
  }
  return m.out;
}

define('x', function () { return { v: 1 }; });
console.log(load('x').v, load('x') === load('x'), count);
""",
                "1 true 1",
                "define 只登记，第一次 load 才执行工厂，并把结果缓存。",
            ),
            steps(
                [
                    "用 IIFE 包住私有实现",
                    "只返回公开 API，不暴露内部变量",
                    "依赖通过参数注入，不读全局",
                    "用注册表登记模块名，首次加载后缓存",
                    "对照 CommonJS 与 ES Module 的差异",
                ]
            ),
        ],
        exercise(
            "模块注册表",
            "实现 createRegistry()，返回 { define, load, has }。define(name, factory) 只登记不执行；"
            "load(name) 第一次执行 factory(exports, load) 并缓存 exports，之后返回同一对象；未知模块返回 null；"
            "has(name) 判断是否登记过。factory 可以通过第二个参数加载依赖。",
            r"""
function createRegistry() {
  // 模块表、define、load、has

}

var registry = createRegistry();
registry.define('math', function (exports) {
  exports.double = function (n) { return n * 2; };
});
console.log(registry.load('math').double(4));
""",
            "模块表用对象保存 { factory, exports, loaded }；load 里先查缓存，再执行工厂，最后返回 exports。",
            [
                "var r1 = createRegistry();\nr1.define('m', function (exports) { exports.v = 2; });\nassert(r1.load('m').v === 2, 'load 返回导出对象')",
                "var runs = 0;\nvar r2 = createRegistry();\nr2.define('once', function (exports) { runs = runs + 1; exports.id = runs; });\nvar a2 = r2.load('once');\nvar b2 = r2.load('once');\nassert(a2 === b2 && runs === 1 && a2.id === 1, '缓存单例，工厂只执行一次')",
                "var r3 = createRegistry();\nr3.define('base', function (exports) { exports.value = 2; });\nr3.define('use', function (exports, load) { var base = load('base'); exports.value = base.value * 10; });\nassert(r3.load('use').value === 20, '依赖注入可用')",
                "assert(r3.has('base') === true && r3.has('missing') === false, 'has 判断登记');\nassert(r3.load('missing') === null, '未知模块返回 null')",
                "var r5 = createRegistry();\nvar count5 = 0;\nr5.define('lazy', function (exports) { count5 = count5 + 1; });\nassert(count5 === 0, 'define 不执行工厂');\nr5.load('lazy');\nassert(count5 === 1, '首次 load 才执行')",
            ],
            r"""
function createRegistry() {
  var modules = {};

  function define(name, factory) {
    modules[name] = { factory: factory, exports: null, loaded: false };
  }

  function load(name) {
    var mod = modules[name];
    if (!mod) return null;
    if (!mod.loaded) {
      mod.loaded = true;
      mod.exports = {};
      mod.factory(mod.exports, load);
    }
    return mod.exports;
  }

  function has(name) {
    return Object.prototype.hasOwnProperty.call(modules, name);
  }

  return { define: define, load: load, has: has };
}

var registry = createRegistry();
registry.define('math', function (exports) {
  exports.double = function (n) { return n * 2; };
});
console.log(registry.load('math').double(4));
""",
        ),
    )


# ---------------------------------------------------------------------------
# js34 性能与复杂度：数组操作代价、字符串拼接、避免 O(n^2)
# ---------------------------------------------------------------------------

def js34():
    return lesson(
        "js34", 333,
        "性能与复杂度实战",
        "数组代价 · 字符串拼接 · 避免 O(n²)",
        140,
        [
            heading("慢，往往是因为在循环里做整表操作"),
            paragraph(
                "js29 讲过复杂度的读法与大方向，这一讲拆到具体 API：每个数组操作背后搬了多少元素，字符串拼接为什么会退化。数组尾部 push 和 pop 是 O(1) 摊还，头部 unshift "
                "和 shift 是 O(n)，因为要移动所有元素；splice 在中间插入或删除同样是 O(n)；concat 和 slice 会复制"
                "元素，成本与长度成正比。把这些操作放进循环，复杂度立刻乘一层：循环 n 次、每次操作 n 个元素，就是 "
                "O(n²)。字符串在 JS 里不可变，每次 s += x 都可能创建新串；现代引擎做了优化，但最稳的写法仍是数组"
                "收集、最后 join。理解这些常数背后的结构，才能判断哪一行值得改。还要注意数组方法的隐藏成本：map 和 filter 各生成一个新数组，链式调用三次就是三次分配，大数据量时可以合并成一次 reduce。sort 是 O(n log n)，但在循环里对同一份数据反复排序就是灾难，应该排一次、查多次。对象属性访问通常很快，但频繁增删属性会让引擎退化为字典模式，批量构造时先定好形状更划算。数组的 length 是属性读取，但每次循环都重新求值仍是多余操作，缓存到局部变量既清晰又稳定；这类改动收益很小，只有在热点路径上才值得做。"
            ),
            paragraph(
                "第二类 O(n²) 来自循环里查表：对每个元素都 indexOf 或 includes 一次，等于每个元素扫一遍数组。换成 "
                "Set 或 Map 的 has 和 get，平均 O(1)，整体降到 O(n)。第三类来自循环里拼接：把结果数组反复 concat，"
                "每次复制已有元素；应该 push 到同一个数组。第四类来自循环里做不变的计算：排序、深拷贝、正则编译，"
                "都能提到循环外。识别方法很机械：看循环体内有没有另一个随规模增长的遍历或复制；有，就想想能不能提前"
                "建索引。空间换时间是常用手段，代价是内存，数据量大时要权衡。另一个常见来源是字符串匹配：对每个元素跑一次正则，等于把模式编译和扫描都塞进循环；把正则提到循环外，或者先用简单条件粗筛。嵌套结构里反复查找也可以先建索引，比如把用户列表转成 id 到用户的 Map。判断是否值得优化，看这段代码在请求路径上被调用多少次、数据规模上限是多少；两个数字都不大时，可读性优先。如果必须保留 O(n²) 的实现，至少把内层循环的边界写对，避免多做无谓的比较；复杂度分析里的常数差异，往往就藏在这些边界里。"
            ),
            paragraph(
                "优化前先量。Date.now() 取毫秒，跑多次取平均，数据量接近真实规模；十个元素的测试说明不了问题。测量"
                "不是为了追求数字，而是确认瓶颈真的在这里。多数业务代码的瓶颈在 IO 和渲染，不在一个 for 循环；把 "
                "O(n²) 改成 O(n) 的收益，通常要到数据量上万才明显。原则是先写对，再写清楚，最后才为热点写快。微优化"
                "只有在你确认它是热点后才值得做，否则只是给代码增加噪音；如果优化让代码难以理解而收益只有几个百分点，"
                "通常不值得。测量工具有限时，可以数操作次数而不是数毫秒：把循环里的比较、复制、分配各计一次，对比两个实现的计数差异，结论比计时稳定得多，本讲示例就是这么做的。优化完成后必须重跑正确性测试，性能提升不能以行为改变为代价；两者冲突时，先保住正确性。"
            ),
            *pair(
                r"""
function countChecksSlow(list) {
  var checks = 0;
  for (var i = 0; i < list.length; i++) {
    for (var j = i + 1; j < list.length; j++) {
      checks = checks + 1;
      if (list[i] === list[j]) return checks;
    }
  }
  return checks;
}

function countChecksFast(list) {
  var seen = new Set();
  var checks = 0;
  for (var i = 0; i < list.length; i++) {
    checks = checks + 1;
    if (seen.has(list[i])) return checks;
    seen.add(list[i]);
  }
  return checks;
}

var data = [1, 2, 3, 4, 5, 6, 7, 8, 9, 9];
console.log(countChecksSlow(data));
console.log(countChecksFast(data));
""",
                "45\n10",
            ),
            table(
                ["操作", "代价", "替代方案"],
                [
                    ["push / pop 尾部", "O(1) 摊还", "首选"],
                    ["unshift / shift 头部", "O(n) 移动元素", "反向 push 后 reverse"],
                    ["splice 中间", "O(n)", "先标记，最后统一过滤"],
                    ["循环里 concat", "O(n²) 复制", "push 到同一数组"],
                    ["循环里 indexOf", "O(n²)", "Set / Map 建索引"],
                    ["循环里字符串 +=", "可能反复复制", "数组收集后 join"],
                ],
            ),
            *pair(
                r"""
function concatSlow(parts) {
  var out = [];
  var copies = 0;
  for (var i = 0; i < parts.length; i++) {
    copies = copies + out.length + 1;
    out = out.concat([parts[i]]);
  }
  return { list: out, copies: copies };
}

function pushFast(parts) {
  var out = [];
  var copies = 0;
  for (var i = 0; i < parts.length; i++) {
    out.push(parts[i]);
    copies = copies + 1;
  }
  return { list: out, copies: copies };
}

var parts = ['a', 'b', 'c', 'd', 'e'];
var slow = concatSlow(parts);
var fast = pushFast(parts);
console.log(slow.list.join(''), slow.copies);
console.log(fast.list.join(''), fast.copies);
""",
                "abcde 15\nabcde 5",
            ),
            warn("用 Date.now() 计时受机器和预热影响，单次结果不可信。比较两个实现要跑多次、同数据、同环境，并先确认行为一致。"),
            quiz(
                "在长度 n 的数组里循环调用 unshift，总复杂度大约是多少？",
                ["O(1)", "O(n)", "O(n²)", "O(log n)"],
                2,
                "每次 unshift 都要移动 O(n) 个元素，循环 n 次就是 O(n²)。",
            ),
            practice(
                "建索引",
                r"""
var users = [{ id: 3, name: 'V' }, { id: 7, name: 'Judy' }];
var byId = new Map();
for (var i = 0; i < users.length; i++) {
  byId.set(users[i].id, users[i].name);
}
console.log(byId.get(7));
console.log(byId.has(9));
""",
                "Judy\nfalse",
                "一次遍历建 Map，之后每次查找平均 O(1)。",
            ),
            steps(
                [
                    "找出循环体内的整表操作",
                    "把 indexOf 换成 Set 或 Map",
                    "把 concat 累积换成 push",
                    "字符串用数组收集后 join",
                    "用真实规模的数据测量前后",
                ]
            ),
        ],
        exercise(
            "摊平与索引",
            "实现 flattenOnce(chunks)：把二维数组按顺序摊平成一维，只遍历一次、只 push，不用 concat 累积，且不修改入参。"
            "实现 indexBy(records, key)：返回 Map，键是 record[key]，值是 record；重复键由后者覆盖前者。",
            r"""
function flattenOnce(chunks) {
  // 只遍历一次，push 到结果数组

}

function indexBy(records, key) {
  // 返回 Map

}

console.log(flattenOnce([[1, 2], [], [3]]).join(','));
""",
            "flattenOnce 用两层 for 循环；indexBy 用 new Map()，遍历一次 set(key, record)。",
            [
                "assert(JSON.stringify(flattenOnce([[1, 2], [], [3]])) === '[1,2,3]', '按顺序摊平')",
                "assert(JSON.stringify(flattenOnce([])) === '[]', '空输入返回空数组')",
                "var input = [[1], [2, 3]];\nvar copy = JSON.stringify(input);\nflattenOnce(input);\nassert(JSON.stringify(input) === copy, '不修改入参')",
                "var users = [{ id: 'a', n: 1 }, { id: 'b', n: 2 }];\nvar map = indexBy(users, 'id');\nassert(map.get('a').n === 1 && map.get('b').n === 2 && map.size === 2, 'Map 索引正确')",
                "var dup = [{ id: 'x', v: 1 }, { id: 'x', v: 2 }];\nvar m2 = indexBy(dup, 'id');\nassert(m2.size === 1 && m2.get('x').v === 2, '重复键后者覆盖')",
            ],
            r"""
function flattenOnce(chunks) {
  var out = [];
  for (var i = 0; i < chunks.length; i++) {
    var chunk = chunks[i];
    for (var j = 0; j < chunk.length; j++) {
      out.push(chunk[j]);
    }
  }
  return out;
}

function indexBy(records, key) {
  var map = new Map();
  for (var i = 0; i < records.length; i++) {
    map.set(records[i][key], records[i]);
  }
  return map;
}

console.log(flattenOnce([[1, 2], [], [3]]).join(','));
""",
        ),
    )


# ---------------------------------------------------------------------------
# js35 内存与闭包陷阱
# ---------------------------------------------------------------------------

def js35():
    return lesson(
        "js35", 334,
        "内存与闭包陷阱",
        "全局泄漏 · 持有引用 · 循环捕获",
        140,
        [
            heading("闭包延长生命周期，也延长了麻烦"),
            paragraph(
                "js16 讲的是闭包怎么用，这一讲讲它什么时候反咬一口。闭包让函数记住定义时的变量环境，这是能力也是负担：只要闭包还活着，它引用的变量就不会被回收。常见"
                "泄漏是顺手捕获了大对象：回调里只用 config.mode，却把整个 config 闭包进去；事件监听器注册后从不移除，"
                "监听器又引用着页面数据。排查方法不是猜，而是问两个问题：这个函数还会活多久，它抓住了谁。能只传需要"
                "的字段，就不要把整个对象带进闭包；能用完就置 null 的引用，就别留着。内存问题通常不会立刻报错，而是"
                "表现为越用越慢。闭包持有引用的方向也要注意：外层持有内层没关系，内层持有外层的大对象才会延长生命周期。缓存是最容易失控的场景：用对象做记忆化，键只增不减，等于把所有参数和结果都留在内存里；需要限制条目数或提供清理接口。定时器和监听器是另外两个常见来源，注册时就要想好什么时候注销。"
            ),
            paragraph(
                "意外全局变量是另一种泄漏：赋值时忘了声明，非严格模式下会挂到全局对象上，永远不释放，还可能覆盖"
                "别人的变量。解决办法是每个函数开头写 'use strict'，让这种赋值直接抛 ReferenceError；或者在文件级"
                "开启严格模式。循环变量捕获是第三个坑：var 只有一个函数级绑定，循环里创建的函数共享同一个变量，最后"
                "全部读到终值。经典修法是工厂函数：每次迭代调用一个函数，把当前值作为参数传进去，参数在每次调用时都是"
                "新的绑定。IIFE 也能达到同样效果，本质都是「用函数调用创建新作用域」。工厂函数的写法有固定套路：外层接收当前值，返回内层函数，内层只读参数不读循环变量；IIFE 是它的即时版本，适合一次性创建。另一个相关陷阱是在循环里给界面元素绑定事件：处理函数捕获循环变量，导致每个元素都响应同一个值，修法同样是传参。严格模式除了拦住意外全局，还会让删除不可删除属性、给只读属性赋值直接报错，早暴露问题。"
            ),
            paragraph(
                "本运行时的 let 在 for 循环里不提供每次迭代的新绑定：实测三个闭包都返回 3。因此不要照搬「用 let 就好"
                "了」的结论，必须用工厂函数或 IIFE 显式捕获。判断闭包是否持有引用，可以看它内部出现了哪些外层变量名；"
                "减少捕获范围、及时解除引用、避免在循环里注册匿名函数，是最实用的三条规则。写代码时养成习惯：先声明"
                "再赋值，循环里创建函数就传参，长生命周期的回调只捕获必要字段。做到这三点，大部分内存问题在写的时候就"
                "被避开了。排查内存问题可以借助变量快照：运行一段代码后看哪些对象还在、谁引用了它。更实用的办法是压测：反复执行同一段逻辑，观察内存曲线是否持续上升；平稳说明没有泄漏，锯齿上升说明有对象没释放。写库代码时，公开的订阅接口一律返回取消函数，这是对调用方最友好的约定。闭包本身不是问题，问题是不受控的引用范围。"
            ),
            *pair(
                r"""
function leaky(value) {
  total = value;
  return total;
}

console.log(leaky(42));
console.log(typeof total, total);

function safe(value) {
  'use strict';
  try {
    hidden = value;
  } catch (e) {
    return e.name;
  }
  return 'no error';
}

console.log(safe(7));
console.log(typeof hidden);
""",
                "42\nnumber 42\nReferenceError\nundefined",
            ),
            table(
                ["陷阱", "现象", "修复"],
                [
                    ["意外全局", "赋值未声明，挂到全局", "严格模式 + 先声明"],
                    ["闭包持有大对象", "内存不释放", "只捕获需要的字段"],
                    ["监听器不移除", "回调与数据一起泄漏", "保存取消函数并调用"],
                    ["循环 var 捕获", "所有函数读到终值", "工厂函数或 IIFE 传参"],
                    ["循环 let（本运行时）", "仍共享同一个绑定", "不要依赖，改用工厂"],
                ],
            ),
            *pair(
                r"""
var fns = [];
for (var i = 0; i < 3; i++) {
  fns.push(function () { return i; });
}
console.log(fns[0](), fns[1](), fns[2]());

function makeGetter(value) {
  return function () { return value; };
}

var fixed = [];
for (var j = 0; j < 3; j++) {
  fixed.push(makeGetter(j));
}
console.log(fixed[0](), fixed[1](), fixed[2]());

function makeBox(value) {
  var held = value;
  return {
    get: function () { return held; },
    clear: function () { held = null; }
  };
}
var box = makeBox({ size: 10 });
console.log(box.get().size);
box.clear();
console.log(box.get());
""",
                "3 3 3\n0 1 2\n10\nnull",
            ),
            warn("本运行时 Rhino 1.7.15 的 let 在 for 循环中不产生每次迭代的新绑定，三个闭包会都返回 3。课程统一用工厂函数捕获，这是跨引擎都成立的做法。"),
            quiz(
                "for (var i = 0; i < 3; i++) 里 push 三个读取 i 的函数，它们分别返回什么？",
                ["0 1 2", "3 3 3", "undefined undefined undefined", "直接报错"],
                1,
                "var 只有一个函数级绑定，循环结束后 i 是 3，三个闭包读到同一个变量。",
            ),
            practice(
                "IIFE 捕获",
                r"""
var out = [];
for (var i = 0; i < 3; i++) {
  out.push((function (n) {
    return function () { return n * 10; };
  })(i));
}
console.log(out[0](), out[1](), out[2]());
""",
                "0 10 20",
                "每次循环立即调用外层函数，把当前的 i 作为参数传进去，形成独立绑定。",
            ),
            steps(
                [
                    "函数开头写 'use strict'",
                    "变量先声明再赋值",
                    "循环里创建函数用工厂传参",
                    "回调只捕获必要字段",
                    "长生命周期对象用完置 null",
                ]
            ),
        ],
        exercise(
            "捕获与释放",
            "实现 makeAdders(nums)：返回函数数组，第 i 个函数接收 x 并返回 x + nums[i]，每个函数必须捕获自己的值。"
            "实现 makeBox(value)：返回 { get, clear }，get 返回当前持有的值，clear 之后 get 返回 null。",
            r"""
function makeAdders(nums) {
  // 每次迭代捕获自己的值

}

function makeBox(value) {
  // 用闭包保存 held

}

var adders = makeAdders([1, 2, 3]);
console.log(adders[0](10), adders[1](10), adders[2](10));
""",
            "makeAdders 每次循环调用一个立即执行的函数，把 nums[i] 作为参数；makeBox 里 held = null 即可释放。",
            [
                "var a1 = makeAdders([1, 2, 3]);\nassert(a1.length === 3 && a1[0](10) === 11 && a1[1](10) === 12 && a1[2](10) === 13, '每个函数捕获自己的值')",
                "var a2 = makeAdders([]);\nassert(a2.length === 0, '空数组返回空数组')",
                "var a3 = makeAdders([5]);\nvar a4 = makeAdders([100]);\nassert(a3[0](1) === 6 && a4[0](1) === 101, '两次调用互不影响')",
                "var box = makeBox({ size: 10 });\nassert(box.get().size === 10, 'get 返回持有的对象');\nbox.clear();\nassert(box.get() === null, 'clear 后返回 null')",
                "var box2 = makeBox(0);\nassert(box2.get() === 0, '假值也要原样返回');\nbox2.clear();\nassert(box2.get() === null, '清除后为 null')",
            ],
            r"""
function makeAdders(nums) {
  var out = [];
  for (var i = 0; i < nums.length; i++) {
    out.push((function (value) {
      return function (x) { return x + value; };
    })(nums[i]));
  }
  return out;
}

function makeBox(value) {
  var held = value;
  return {
    get: function () { return held; },
    clear: function () { held = null; }
  };
}

var adders = makeAdders([1, 2, 3]);
console.log(adders[0](10), adders[1](10), adders[2](10));
""",
        ),
    )


# ---------------------------------------------------------------------------
# js36 异步编程模型：事件循环、宏任务/微任务、Promise 链与错误传播
# ---------------------------------------------------------------------------

def js36():
    return lesson(
        "js36", 335,
        "异步编程模型",
        "事件循环 · 微任务 · Promise 链",
        150,
        [
            heading("先把「什么时候执行」想清楚"),
            paragraph(
                "JS 是单线程的：同一时刻只有一段代码在跑。js28 讲的是 Promise 的链式写法，这一讲往下挖一层：异步不是多线程，而是把任务排队，等当前代码跑完再执行。"
                "事件循环每轮做一件事：取一个宏任务执行，执行完把微任务队列清空，再进入下一轮。宏任务包括脚本、"
                "定时器、IO 回调；微任务包括 Promise 的 then、catch、finally（浏览器里还有 queueMicrotask）。因此「同步代码 → "
                "所有微任务 → 下一个宏任务」是固定的优先级。理解这条顺序，才能解释为什么 then 里的代码总在最后一行"
                "同步代码之后，也才能判断两段异步逻辑谁先谁后。微任务之间也有顺序：先注册的先执行，新产生的微任务追加到队尾，并在本轮继续清空，所以 then 里再返回 Promise 会增加轮次但不会跳到宏任务之后。Promise 的状态一旦确定就不再改变，重复 resolve 或 reject 都被忽略。executor 里的同步异常会自动转成 rejected，这一点常被忽略：new Promise 里 throw 与 reject 等价。"
            ),
            paragraph(
                "Promise 链的错误传播遵循一个规则：跳过成功回调，直到遇到 catch。链上任何一环 throw 或返回 rejected，"
                "后面的 then 都不执行；catch 的回调返回普通值时，链恢复成功，后续 then 继续。then 返回 Promise 时会"
                "等待它完成，这就是串行异步的基础。Promise.all 等全部成功，任一失败立即失败；Promise.race 取最先"
                "完成的结果。注意 all 的失败会丢掉其他结果，需要全部结果时给每个 Promise 单独 catch，包装成 "
                "{ ok, value } 或 { ok, error } 的结构，再交给 all。catch 的位置决定恢复点：放在链中间，后面的 then 会继续；放在链尾，前面所有错误都在这里汇总。finally 不改变结果，只做清理，它的返回值会被忽略，除非自己抛出异常。串行异步不要用 for 循环手动等待，而是用 reduce 或显式 chain 把 Promise 串起来；并行则直接用 all，不要手动计数。判断标准是任务之间有没有依赖：有依赖串行，无依赖并行。并行还有一个隐藏成本：同时发起的请求太多会拖垮下游，需要限制并发数，用计数器和队列控制同时在跑的任务数量，而不是一次全发出去。"
            ),
            paragraph(
                "本运行时的 Promise 是同步调度的：脚本执行完后统一清空微任务队列，没有真正的宏任务。所以示例里看不到"
                "定时器，但「同步先、回调后、按注册顺序执行」的语义与浏览器一致。写课程代码时不要依赖精确的微任务"
                "轮次，只依赖这三条稳定的规则。真实项目里，异步模型决定两件事：哪些代码必须放进回调，哪些错误必须用 "
                "catch 接住。把这两件事想清楚，比背 API 更重要；API 会变，执行顺序的模型不会。还有一个常被问的问题：Promise 能不能取消？标准 Promise 不能，需要自己封装带取消标记的对象，或者用 race 让超时先返回。错误对象要保留上下文：抛出 new Error 时附上操作名和关键参数，比只写「失败」有用得多。异步代码的测试要断言回调之后的状态，而不是回调之前的状态。判题用例天然按「上一条注册、下一条断言」的节奏写，这个节奏也提醒你：回调里的赋值不会立刻生效。测试异步代码时不要在回调内部断言后继续往下写同步代码，后续代码会在回调之前执行；把断言放进 then 链，或者拆成多条独立用例。"
            ),
            *pair(
                r"""
var log = [];
console.log('start');
Promise.resolve('A').then(function (v) {
  log.push(v);
  console.log('micro 1', v);
});
Promise.resolve('B').then(function (v) {
  log.push(v);
  console.log('micro 2', v);
});
log.push('sync');
console.log('sync end');
""",
                "start\nsync end\nmicro 1 A\nmicro 2 B",
            ),
            diagram(
                "同步脚本（调用栈）\n"
                "  └─ 执行完，清空微任务队列（then / catch / finally）\n"
                "      └─ 进入下一轮，取一个宏任务（定时器 / IO）"
            ),
            *pair(
                r"""
Promise.resolve(2)
  .then(function (v) { return v + 1; })
  .then(function (v) { throw new Error('bad ' + v); })
  .then(function () { console.log('skipped'); })
  ['catch'](function (e) { return 'recovered:' + e.message; })
  .then(function (v) { console.log(v); });

Promise.all([
  Promise.resolve('x'),
  Promise.resolve('y')
]).then(function (list) {
  console.log('all', list.join(''));
});

Promise.reject(new Error('denied'))
  ['catch'](function (e) { return e.message; })
  .then(function (v) { console.log('race', v); });
""",
                "all xy\nrace denied\nrecovered:bad 3",
            ),
            table(
                ["概念", "例子", "执行时机"],
                [
                    ["调用栈", "同步函数调用", "立即执行"],
                    ["微任务", "then / catch / finally", "当前宏任务结束后清空"],
                    ["宏任务", "定时器 / IO / 脚本", "下一轮事件循环"],
                    ["Promise.all", "全部成功才成功", "最慢的那个完成后"],
                    ["Promise.race", "最先完成的结果", "第一个 settle 后"],
                ],
            ),
            tip("需要按顺序执行的异步步骤写成扁平的 then 链，不要在 then 里再嵌套 then；嵌套会重新变成回调地狱。"),
            quiz(
                "同步代码打印 A，then 回调打印 B，再同步打印 C，输出顺序是？",
                ["A B C", "A C B", "B A C", "C A B"],
                1,
                "then 回调进入微任务队列，当前同步代码先跑完，所以是 A、C、B。",
            ),
            practice(
                "错误恢复",
                r"""
Promise.reject(new Error('timeout'))
  .then(function () { return 'never'; })
  ['catch'](function (e) { return 'fallback:' + e.message; })
  .then(function (v) { console.log(v); });
""",
                "fallback:timeout",
                "catch 的回调返回普通值，链条恢复成功，后续 then 收到这个值。",
            ),
            steps(
                [
                    "先分清同步代码和回调代码",
                    "then 里只做一步转换",
                    "错误统一交给链尾 catch",
                    "并行用 all，竞速用 race",
                    "需要全部结果时先包装成对象再 all",
                ]
            ),
        ],
        exercise(
            "异步工具",
            "实现 settle(p)：返回 Promise，成功时 resolve { ok: true, value }，失败时 resolve { ok: false, error }，"
            "调用方永远拿到对象而不是 rejected。实现 mapSeries(list, fn)：按顺序把每个元素传给 fn（fn 返回 Promise），"
            "全部完成后 resolve 结果数组；任一失败则 reject 该错误。",
            r"""
function settle(p) {
  // then 的两个回调都返回对象

}

function mapSeries(list, fn) {
  // 用 Promise 链串行

}

var seen = [];
mapSeries([1, 2, 3], function (n) {
  seen.push(n);
  return Promise.resolve(n * 2);
}).then(function (values) {
  console.log(values.join(','), seen.join(','));
});
""",
            "settle 用 p.then(onOk, onErr)；mapSeries 用 chain = chain.then(...) 串联，每步把结果 push 到 out，最后返回 out。",
            [
                "var r1 = null;\nvar r3 = null;\nvar order = [];\nvar result = null;\nvar emptyResult = null;\nsettle(Promise.resolve(5)).then(function (x) { r1 = x; });\nsettle(Promise.reject(new Error('boom'))).then(function (x) { r3 = x; });\nmapSeries([1, 2, 3], function (n) { order.push(n); return Promise.resolve(n * 10); }).then(function (v) { result = v; });\nmapSeries([], function (n) { return Promise.resolve(n); }).then(function (v) { emptyResult = v; });\nassert(r1 === null && r3 === null && result === null && emptyResult === null && order.join(',') === '', '回调尚未执行')",
                "assert(r1 !== null && r1.ok === true && r1.value === 5, '成功包装为 ok:true')",
                "assert(r3 !== null && r3.ok === false && r3.error.message === 'boom', '失败包装为 ok:false')",
                "assert(result !== null && result.join(',') === '10,20,30' && order.join(',') === '1,2,3', '串行执行且顺序稳定')",
                "assert(emptyResult !== null && emptyResult.length === 0, '空列表返回空数组')",
            ],
            r"""
function settle(p) {
  return p.then(
    function (value) { return { ok: true, value: value }; },
    function (error) { return { ok: false, error: error }; }
  );
}

function mapSeries(list, fn) {
  var out = [];
  var chain = Promise.resolve();
  for (var i = 0; i < list.length; i++) {
    (function (index) {
      chain = chain.then(function () {
        return fn(list[index]);
      }).then(function (value) {
        out.push(value);
      });
    })(i);
  }
  return chain.then(function () { return out; });
}

var seen = [];
mapSeries([1, 2, 3], function (n) {
  seen.push(n);
  return Promise.resolve(n * 2);
}).then(function (values) {
  console.log(values.join(','), seen.join(','));
});
""",
        ),
    )


# ---------------------------------------------------------------------------
# js37 测试与调试：断言、边界用例、console 调试、错误堆栈
# ---------------------------------------------------------------------------

def js37():
    return lesson(
        "js37", 336,
        "测试与调试",
        "断言 · 边界 · 堆栈",
        140,
        [
            heading("先写「怎么知道它错了」"),
            paragraph(
                "调试的第一步不是加断点，而是把期望写成断言。断言把模糊的「好像不对」变成明确的「第三个用例：期望 0，"
                "实际 undefined」。好的断言消息包含三件事：哪个用例、期望值、实际值。测试用例的设计比数量重要：正常"
                "路径一条，边界值各一条，空数组、单个元素、负数、重复值、最大值都算边界；错误路径一条，确认非法输入"
                "按约定处理。边界用例最容易暴露 off-by-one 和空值处理问题，也最容易在重构时被忽略。先写断言，再写"
                "实现，思路会清楚很多。断言粒度也要合适：一个用例只验证一个行为，失败时才知道是哪条约定被破坏。比较对象时用 JSON.stringify 或逐字段断言，直接比较引用永远为 false；浮点数用误差范围比较，不要用严格相等。测试数据要可读：用有意义的输入而不是 a、b、c，失败信息里带上输入，复现成本会低很多。"
            ),
            paragraph(
                "console 调试不是乱打日志，而是有策略地缩小范围：先在入口打印参数，再在分支打印走了哪条路，最后在"
                "出口打印结果；用带前缀的日志区分模块，用 JSON.stringify 打印结构。二分法比逐行打印快：注释掉一半"
                "逻辑，看错误是否还在，几轮就能定位。错误堆栈要从下往上看：最下面是调用起点，最上面是抛出点；先读"
                "错误类型和信息，再读第一行业务代码帧，库里的匿名帧可以跳过。栈里的函数名和行号是最可靠的线索，比"
                "猜测快得多。日志要能删：调试结束后搜一遍前缀统一清理，不要留在交付代码里。条件断点的替代品是带判断的日志，只在特定输入时打印，避免刷屏。二分排查也可以按数据分半：先跑前一半数据，再跑后一半，快速定位是哪个元素触发问题。堆栈里的行号在压缩或转译后会失真，这时候要靠 source map；没有 source map 时，用函数名和调用顺序推断。"
            ),
            paragraph(
                "测试代码本身也要可信：断言必须真的会失败。写完一个测试，故意把实现改坏一处，确认测试变红，再改回来；"
                "一直绿的测试可能根本没执行。测试之间要独立，共享状态会让失败依赖执行顺序。错误处理要区分预期内的"
                "失败和程序缺陷：前者用返回值或错误对象表达，后者才 throw。把这条边界划清楚，日志和测试都会干净很多。"
                "最后，测试命名要说明行为，不要写 test1、test2；名字本身就是文档，失败时能直接告诉你哪条约定被破坏。断言消息的格式统一之后，测试报告可以直接当清单看：哪条通过、哪条失败、期望什么、实际什么。回归测试要针对曾经出现的 bug，每修一个就补一条，防止同样的错误再次出现。调试的终点不是「不报错了」，而是「能解释为什么之前会错」；解释不清楚，说明还没找到根因，只是把症状按下去了。修复之后补一条回归用例，把触发条件固定下来；用例名写清业务含义，下次有人改坏时能立刻看懂。调试记录也值得留：把「现象、假设、验证、结论」四行写进提交说明，下次遇到同类问题可以直接查。"
            ),
            *pair(
                r"""
function assertEqual(actual, expected, label) {
  if (actual !== expected) {
    throw new Error(label + ': expected ' + expected + ', got ' + actual);
  }
  return true;
}

function runCases(cases) {
  var passed = 0;
  var failures = [];
  for (var i = 0; i < cases.length; i++) {
    try {
      cases[i].fn();
      passed = passed + 1;
    } catch (e) {
      failures.push(cases[i].name + ' -> ' + e.message);
    }
  }
  return { passed: passed, failed: failures.length, failures: failures };
}

var report = runCases([
  { name: 'sum empty', fn: function () { assertEqual(0, 0, 'sum empty'); } },
  { name: 'sum normal', fn: function () { assertEqual(6, 6, 'sum normal'); } },
  { name: 'sum broken', fn: function () { assertEqual(5, 6, 'sum broken'); } }
]);
console.log(report.passed, report.failed);
console.log(report.failures.join('|'));
""",
                "2 1\nsum broken -> sum broken: expected 6, got 5",
            ),
            table(
                ["手段", "用途", "注意"],
                [
                    ["assertEqual", "明确期望与实际", "消息要带用例名"],
                    ["边界用例", "空、单元素、负数、重复", "每条都写期望"],
                    ["console.log", "缩小范围", "带前缀、打印结构"],
                    ["二分排查", "快速定位", "注释掉一半逻辑"],
                    ["错误堆栈", "找抛出点与调用链", "从下往上看"],
                    ["故意改坏", "验证测试会红", "确认后改回"],
                ],
            ),
            *pair(
                r"""
function innerFrame(n) {
  if (n < 0) throw new Error('negative: ' + n);
  return n;
}

function outerFrame(n) {
  return innerFrame(n);
}

try {
  outerFrame(-1);
} catch (e) {
  console.log(e.name);
  console.log(e.message);
  console.log(e.stack.indexOf('innerFrame') >= 0);
  console.log(e.stack.indexOf('outerFrame') >= 0);
}

function safeDivide(a, b) {
  if (b === 0) return null;
  return a / b;
}
console.log(safeDivide(8, 2), safeDivide(1, 0));
""",
                "Error\nnegative: -1\ntrue\ntrue\n4 null",
            ),
            warn("一直通过的测试不等于有效测试。把被测函数改坏一处再跑，如果仍然全绿，说明断言没有覆盖到行为。"),
            quiz(
                "阅读错误堆栈时，最先应该看什么？",
                ["最上面的库帧", "错误类型与消息", "行号最多的那一帧", "最后一行的缩进"],
                1,
                "错误类型和消息直接说明发生了什么；堆栈用来定位调用链，从下往上看更接近调用起点。",
            ),
            practice(
                "边界检查",
                r"""
function first(list) {
  if (!Array.isArray(list) || list.length === 0) return null;
  return list[0];
}

console.log(first([]), first([0]), first(null), first([3, 4]));
""",
                "null 0 null 3",
                "空数组、假值元素、非数组三种边界都要覆盖。",
            ),
            steps(
                [
                    "先写断言和失败消息",
                    "补边界用例：空、单元素、负数、重复",
                    "日志带前缀，打印结构",
                    "用二分法缩小范围",
                    "堆栈从下往上看，先读类型和信息",
                ]
            ),
        ],
        exercise(
            "迷你测试框架",
            "实现 assertEqual(actual, expected, label)：不相等时 throw new Error，消息格式为 "
            "「label: expected <expected>, got <actual>」；相等返回 true。实现 runCases(cases)：cases 是 "
            "{ name, fn } 数组，逐个执行并捕获异常，返回 { passed, failed, failures }，failures 每项格式为 "
            "「name -> error.message」。",
            r"""
function assertEqual(actual, expected, label) {
  // 不相等就抛错

}

function runCases(cases) {
  // 逐条执行并统计

}

var report = runCases([
  { name: 'ok', fn: function () { assertEqual(1, 1, 'ok'); } },
  { name: 'bad', fn: function () { assertEqual(1, 2, 'bad'); } }
]);
console.log(report.passed, report.failed);
""",
            "assertEqual 用 !== 比较；runCases 用 try/catch 包住 cases[i].fn()，成功 passed 加一，失败把 name 和 message 拼起来。",
            [
                "assert(assertEqual(2, 2, 'same') === true, '相等返回 true')",
                "var msg = '';\ntry { assertEqual(2, 3, 'add'); } catch (e) { msg = e.message; }\nassert(msg === 'add: expected 3, got 2', '失败消息包含期望与实际')",
                "var r3 = runCases([\n  { name: 'a', fn: function () { assertEqual(1, 1, 'a'); } },\n  { name: 'b', fn: function () { assertEqual(1, 2, 'b'); } },\n  { name: 'c', fn: function () {} }\n]);\nassert(r3.passed === 2 && r3.failed === 1, '统计通过和失败')",
                "assert(r3.failures.length === 1 && r3.failures[0] === 'b -> b: expected 2, got 1', '失败列表格式正确')",
                "var r5 = runCases([]);\nassert(r5.passed === 0 && r5.failed === 0 && r5.failures.length === 0, '空用例集')",
            ],
            r"""
function assertEqual(actual, expected, label) {
  if (actual !== expected) {
    throw new Error(label + ': expected ' + expected + ', got ' + actual);
  }
  return true;
}

function runCases(cases) {
  var passed = 0;
  var failures = [];
  for (var i = 0; i < cases.length; i++) {
    try {
      cases[i].fn();
      passed = passed + 1;
    } catch (e) {
      failures.push(cases[i].name + ' -> ' + e.message);
    }
  }
  return { passed: passed, failed: failures.length, failures: failures };
}

var report = runCases([
  { name: 'ok', fn: function () { assertEqual(1, 1, 'ok'); } },
  { name: 'bad', fn: function () { assertEqual(1, 2, 'bad'); } }
]);
console.log(report.passed, report.failed);
""",
        ),
    )


# ---------------------------------------------------------------------------
# js38 综合项目：任务队列 / 状态机
# ---------------------------------------------------------------------------

def js38():
    return lesson(
        "js38", 337,
        "综合项目：任务队列",
        "状态机 + 观察者 + 串行执行",
        160,
        [
            heading("把八讲的能力装进一个可用的东西"),
            paragraph(
                "js30 的状态机是纯同步的文本冒险，这一讲把状态机放进异步执行器：做一个任务队列，任务有 id、优先级、状态和处理函数；队列按优先级取出任务，串行执行，成功进入 "
                "done，失败进入 failed 并记录错误；每次状态变化通过观察者通知外部。状态机是核心：queued 到 running "
                "再到 done 或 failed，任何一步都不允许跳转；状态更新返回新对象，旧状态保留，方便回放和对比。优先级"
                "用数字表示，数字小的先执行；同优先级按加入顺序，保证结果稳定。状态机的价值在于把「现在能做什么」写死，"
                "调用方无法把任务从 queued 直接改成 done。任务对象本身也应该是只读的：id、优先级、处理函数在入队后不再变化，状态变化体现在队列状态里，而不是改任务对象。优先级队列可以用数组加线性扫描，任务数量不大时完全够用；数量上千再考虑二叉堆。为了在取出元素后仍能比较先后，给每个任务记一个自增序号，而不是记数组下标；下标会随 splice 变化，序号不会。"
            ),
            paragraph(
                "执行器用 Promise 串行：从队列里取一个任务，调用处理函数，把结果写回状态，再取下一个。失败不中断整个"
                "队列，而是把该任务标记为 failed，继续下一个，这是批处理与流水线的关键区别。观察者负责把变化推给外部："
                "入队、开始、成功、失败、结束各发一个事件，订阅方可以记日志或更新界面。所有更新函数保持纯：给定旧状态"
                "和动作，返回新状态；副作用集中在执行器和通知器里。这样测试可以直接断言状态，不需要模拟输入输出，也"
                "不需要等待真实时间。执行器的串行链要小心闭包捕获：循环里创建 then 回调时，用立即执行的函数把当前任务固定下来，否则所有回调都会读到最后一个任务。失败处理要区分「任务失败」和「系统错误」：任务处理函数抛错算任务失败，队列本身的 bug 应该让 Promise reject，不要吞掉。观察者通知放在状态更新之后，保证监听器读到的 state 已经是最新的。"
            ),
            paragraph(
                "这个项目把前面的能力串起来：函数式负责纯更新和组合，设计模式负责工厂、观察者和策略，模块化负责闭包"
                "隐藏内部状态，性能负责一次遍历选出最高优先级，闭包负责私有状态与引用释放，异步负责 Promise 串行和"
                "错误传播，测试负责状态断言与边界用例。做完之后可以继续扩展：重试次数、并发上限、任务依赖、持久化。"
                "每加一个功能，先问它属于状态、执行还是通知，再决定改哪一层；层次清楚，扩展就不会互相纠缠。扩展重试时，把重试次数记在任务上，失败后判断是否重排；扩展并发时，把串行链换成固定数量的工作槽，每个槽取下一个任务；扩展持久化时，把状态序列化成 JSON 存起来，恢复时重新构造队列。三种扩展都只动一层，这就是分层的收益。项目交付前跑一遍完整流程，确认空队列、全成功、有失败、重复优先级四种情况都符合预期。做完这个项目，你已经把函数式、模式、模块、性能、闭包、异步、测试七块能力各用了一遍；下一步是把同样的结构套到自己熟悉的业务上，比如表单校验或请求编排。"
            ),
            *pair(
                r"""
function createQueue() {
  var tasks = [];
  var seq = 0;
  var state = { status: 'idle', done: [], failed: [] };

  function add(id, priority, handler) {
    tasks.push({ id: id, priority: priority, seq: seq, handler: handler });
    seq = seq + 1;
    return tasks.length;
  }

  function pick() {
    if (tasks.length === 0) return null;
    var best = 0;
    for (var i = 1; i < tasks.length; i++) {
      if (tasks[i].priority < tasks[best].priority) best = i;
    }
    return tasks.splice(best, 1)[0];
  }

  return {
    add: add,
    pick: pick,
    state: function () { return state; }
  };
}

var q = createQueue();
q.add('low', 5, function () { return 'L'; });
q.add('high', 1, function () { return 'H'; });
q.add('mid', 3, function () { return 'M'; });
console.log(q.pick().id);
console.log(q.pick().id);
console.log(q.pick().id);
console.log(q.pick());
""",
                "high\nmid\nlow\nnull",
            ),
            diagram(
                "queued ──run──> running ──成功──> done\n"
                "                    └──抛错──> failed\n\n"
                "只允许这两条路径，状态更新返回新对象。"
            ),
            *pair(
                r"""
var events = [];

function createRunner() {
  var listeners = [];
  var state = { status: 'idle', done: [], failed: [] };

  function on(fn) {
    listeners.push(fn);
    return function () {
      var idx = listeners.indexOf(fn);
      if (idx !== -1) listeners.splice(idx, 1);
    };
  }

  function emit(type, payload) {
    var list = listeners.slice();
    for (var i = 0; i < list.length; i++) list[i](type, payload);
  }

  function run(tasks) {
    state = { status: 'running', done: [], failed: [] };
    emit('start', tasks.length);
    var chain = Promise.resolve();
    for (var i = 0; i < tasks.length; i++) {
      (function (task) {
        chain = chain.then(function () {
          try {
            var value = task.handler();
            state = {
              status: 'running',
              done: state.done.concat([{ id: task.id, value: value }]),
              failed: state.failed
            };
            emit('done', task.id);
          } catch (e) {
            state = {
              status: 'running',
              done: state.done,
              failed: state.failed.concat([{ id: task.id, error: e.message }])
            };
            emit('failed', task.id);
          }
        });
      })(tasks[i]);
    }
    return chain.then(function () {
      state = { status: 'finished', done: state.done, failed: state.failed };
      emit('finish', state.done.length + '/' + state.failed.length);
      return state;
    });
  }

  return { on: on, run: run, state: function () { return state; } };
}

var runner = createRunner();
runner.on(function (type, payload) { events.push(type + ':' + payload); });
runner.run([
  { id: 'a', handler: function () { return 1; } },
  { id: 'b', handler: function () { throw new Error('boom'); } },
  { id: 'c', handler: function () { return 3; } }
]).then(function (s) {
  console.log(events.join('|'));
  console.log(s.done.length, s.failed.length);
  console.log(s.status);
});
""",
                "start:3|done:a|failed:b|done:c|finish:2/1\n2 1\nfinished",
            ),
            table(
                ["模块", "职责", "关键点"],
                [
                    ["状态机", "queued / running / done / failed", "只允许合法迁移"],
                    ["任务队列", "按优先级取任务", "遍历选最小，O(n)"],
                    ["执行器", "Promise 串行", "失败不中断后续"],
                    ["观察者", "start / done / failed / finish", "通知前复制监听器"],
                    ["纯更新", "返回新状态", "旧状态可回放"],
                    ["测试", "断言状态与事件", "覆盖失败与空队列"],
                ],
            ),
            warn("状态对象每次更新都重建，done 和 failed 数组用 concat 生成新数组；如果直接 push，旧快照会被污染，回放和对比就失效了。"),
            quiz(
                "任务处理函数抛错时，执行器应该怎么做？",
                ["立即停止整个队列", "标记 failed 并继续下一个", "把错误抛到全局", "静默删除该任务"],
                1,
                "批处理场景要把每个任务的失败隔离，记录后继续，最终汇总成功与失败数量。",
            ),
            practice(
                "状态迁移守卫",
                r"""
var allowed = { queued: ['running'], running: ['done', 'failed'] };

function canMove(from, to) {
  var list = allowed[from] || [];
  return list.indexOf(to) !== -1;
}

console.log(canMove('queued', 'running'), canMove('queued', 'done'), canMove('running', 'failed'));
""",
                "true false true",
                "只查表，不猜状态；非法迁移返回 false，由调用方处理。",
            ),
            steps(
                [
                    "定义状态与合法迁移表",
                    "写 add 和 pick，先保证优先级正确",
                    "用 Promise 串行执行，失败不中断",
                    "接观察者，发 start/done/failed/finish",
                    "写测试：空队列、全成功、有失败、顺序稳定",
                ]
            ),
        ],
        exercise(
            "任务队列",
            "实现 createTaskQueue()，返回 { add, run, state, on }。add(id, priority, handler) 入队并返回队列长度；"
            "run() 按优先级（数字小优先，同优先级按加入顺序）串行执行所有任务，返回 Promise，全部结束后 resolve "
            "最终状态 { status, done, failed }；成功任务进入 done（元素 { id, value }），抛错任务进入 failed"
            "（元素 { id, error }），失败不中断后续；on(fn) 注册监听器，收到 (type, payload)。state() 返回当前状态，"
            "run 开始时 status 为 running，结束时为 finished。",
            r"""
function createTaskQueue() {
  // 任务表、状态、监听器、add / run / state / on

}

var q = createTaskQueue();
q.add('a', 5, function () { return 1; });
q.add('b', 1, function () { return 2; });
q.run().then(function (s) {
  console.log(s.done[0].id, s.done[1].id, s.status);
});
""",
            "run 先把 state 置为 running，再用 chain = chain.then(...) 串起来；take 时遍历找 priority 最小、同优先级 seq 最小的任务。",
            [
                "var q1 = createTaskQueue();\nvar events1 = [];\nq1.on(function (type) { events1.push(type); });\nassert(q1.add('a', 5, function () { return 1; }) === 1, 'add 返回长度 1');\nassert(q1.add('b', 1, function () { return 2; }) === 2, 'add 返回长度 2');\nvar finished1 = null;\nq1.run().then(function (s) { finished1 = s; });\nassert(q1.state().status === 'running', 'run 后立即 running')",
                "assert(finished1 !== null && finished1.status === 'finished', '结束后 finished');\nassert(finished1.done.length === 2 && finished1.done[0].id === 'b' && finished1.done[0].value === 2 && finished1.done[1].id === 'a', '优先级小的先完成');\nassert(events1.join(',') === 'start,done,done,finish', '观察者收到完整事件序列')",
                "var q3 = createTaskQueue();\nq3.add('bad', 1, function () { throw new Error('boom'); });\nvar failed3 = null;\nq3.run().then(function (s) { failed3 = s; });\nvar q5 = createTaskQueue();\nvar empty5 = null;\nq5.run().then(function (s) { empty5 = s; });\nassert(failed3 === null && empty5 === null, '回调尚未执行')",
                "assert(failed3 !== null && failed3.failed.length === 1 && failed3.failed[0].id === 'bad' && failed3.failed[0].error === 'boom', '失败被记录且 run 正常 resolve')",
                "assert(empty5 !== null && empty5.status === 'finished' && empty5.done.length === 0 && empty5.failed.length === 0, '空队列正常结束')",
            ],
            r"""
function createTaskQueue() {
  var tasks = [];
  var seq = 0;
  var listeners = [];
  var state = { status: 'idle', done: [], failed: [] };

  function add(id, priority, handler) {
    tasks.push({ id: id, priority: priority, seq: seq, handler: handler });
    seq = seq + 1;
    return tasks.length;
  }

  function take() {
    if (tasks.length === 0) return null;
    var best = 0;
    for (var i = 1; i < tasks.length; i++) {
      var a = tasks[i];
      var b = tasks[best];
      if (a.priority < b.priority || (a.priority === b.priority && a.seq < b.seq)) {
        best = i;
      }
    }
    return tasks.splice(best, 1)[0];
  }

  function emit(type, payload) {
    var list = listeners.slice();
    for (var i = 0; i < list.length; i++) list[i](type, payload);
  }

  function run() {
    state = { status: 'running', done: [], failed: [] };
    emit('start', tasks.length);
    var chain = Promise.resolve();
    var total = tasks.length;
    for (var i = 0; i < total; i++) {
      chain = chain.then(function () {
        var task = take();
        if (task === null) return null;
        try {
          var value = task.handler();
          state = {
            status: 'running',
            done: state.done.concat([{ id: task.id, value: value }]),
            failed: state.failed
          };
          emit('done', task.id);
        } catch (e) {
          state = {
            status: 'running',
            done: state.done,
            failed: state.failed.concat([{ id: task.id, error: e.message }])
          };
          emit('failed', task.id);
        }
        return null;
      });
    }
    return chain.then(function () {
      state = { status: 'finished', done: state.done, failed: state.failed };
      emit('finish', state.done.length + '/' + state.failed.length);
      return state;
    });
  }

  return {
    add: add,
    run: run,
    state: function () { return state; },
    on: function (fn) { listeners.push(fn); return listeners.length; }
  };
}

var q = createTaskQueue();
q.add('a', 5, function () { return 1; });
q.add('b', 1, function () { return 2; });
q.run().then(function (s) {
  console.log(s.done[0].id, s.done[1].id, s.status);
});
""",
        ),
    )


# ---------------------------------------------------------------------------
# 结构自检
# ---------------------------------------------------------------------------

LESSONS = [js31(), js32(), js33(), js34(), js35(), js36(), js37(), js38()]


def _han(text):
    return sum(1 for ch in text if "\u4e00" <= ch <= "\u9fff")


def _strip_strings_and_comments(src):
    """粗略去掉字符串与注释，避免误报（课程代码里会出现 import 等词）。"""
    out = []
    i = 0
    n = len(src)
    while i < n:
        ch = src[i]
        if ch in ("'", '"', "`"):
            quote = ch
            i += 1
            while i < n:
                if src[i] == "\\":
                    i += 2
                    continue
                if src[i] == quote:
                    i += 1
                    break
                i += 1
            out.append(" ")
            continue
        if ch == "/" and i + 1 < n and src[i + 1] == "/":
            while i < n and src[i] != "\n":
                i += 1
            out.append(" ")
            continue
        if ch == "/" and i + 1 < n and src[i + 1] == "*":
            i += 2
            while i + 1 < n and not (src[i] == "*" and src[i + 1] == "/"):
                i += 1
            i += 2
            out.append(" ")
            continue
        out.append(ch)
        i += 1
    return "".join(out)


FORBIDDEN = [
    (r"\bclass\s+[A-Za-z_$]", "class 语法"),
    (r"\.\.\.", "展开运算符"),
    (r"function\s*\([^)]*=[^)]*\)", "函数默认参数"),
    (r"function\s+[A-Za-z_$][\w$]*\s*\([^)]*=", "函数默认参数"),
    (r"\basync\b", "async 语法"),
    (r"\bawait\b", "await 语法"),
    (r"\?\.", "可选链"),
    (r"\?\?", "空值合并"),
    (r"\bimport\s", "import 语法"),
    (r"\brequire\s*\(", "require 调用"),
    (r"\bprocess\b", "process 对象"),
    (r"\bsetTimeout\s*\(", "setTimeout"),
    (r"\bsetInterval\s*\(", "setInterval"),
    (r"\bfetch\s*\(", "fetch"),
    (r"\bdocument\b", "document 对象"),
    (r"\bwindow\b", "window 对象"),
]


def _check_forbidden(where, src):
    problems = []
    cleaned = _strip_strings_and_comments(src)
    for pattern, label in FORBIDDEN:
        if re.search(pattern, cleaned):
            problems.append(f"{where} 使用了 {label}")
    return problems


def validate(lessons):
    problems = []
    if len(lessons) != 8:
        problems.append(f"应为 8 讲，实际 {len(lessons)}")

    seen_ids = set()
    seen_orders = set()
    for i, item in enumerate(lessons):
        lid = item["id"]
        expected_id = f"js{FIRST_ID + i}"
        expected_order = FIRST_ORDER + i
        if lid != expected_id:
            problems.append(f"{lid}: id 应为 {expected_id}")
        if item["order"] != expected_order:
            problems.append(f"{lid}: order 应为 {expected_order}")
        if item["chapter"] != CHAPTER:
            problems.append(f"{lid}: chapter 应为 {CHAPTER}")
        if item["chapterName"] != CHAPTER_NAME:
            problems.append(f"{lid}: chapterName 不一致")
        if item["lang"] != "javascript":
            problems.append(f"{lid}: lang 应为 javascript")
        if lid in seen_ids:
            problems.append(f"重复 id: {lid}")
        if item["order"] in seen_orders:
            problems.append(f"重复 order: {item['order']}")
        seen_ids.add(lid)
        seen_orders.add(item["order"])

        blocks = item["blocks"]
        types = [b["type"] for b in blocks]
        if len(blocks) < 10:
            problems.append(f"{lid}: 内容块 {len(blocks)} 个，少于 10")
        if types.count("heading") != 1:
            problems.append(f"{lid}: 应有且仅有 1 个 heading")
        if types.count("text") != 3:
            problems.append(f"{lid}: 应有 3 段 text，实际 {types.count('text')}")
        body = "".join(b["text"] for b in blocks if b["type"] == "text")
        han = _han(body)
        if not 900 <= han <= 1600:
            problems.append(f"{lid}: 正文汉字 {han}，应在 900-1600")
        if types.count("code") < 2:
            problems.append(f"{lid}: 至少 2 个代码块")
        if types.count("code") != types.count("output"):
            problems.append(f"{lid}: code 与 output 数量不匹配")
        if not any(t in types for t in ("table", "diagram")):
            problems.append(f"{lid}: 缺少 table 或 diagram")
        if not any(t in types for t in ("tip", "warn")):
            problems.append(f"{lid}: 缺少 tip 或 warn")
        if "quiz" not in types:
            problems.append(f"{lid}: 缺少 quiz")
        if "practice" not in types:
            problems.append(f"{lid}: 缺少 practice")
        if "steps" not in types:
            problems.append(f"{lid}: 缺少 steps")

        for block in blocks:
            if block["type"] == "quiz":
                if len(block["options"]) != 4:
                    problems.append(f"{lid}: quiz 应有 4 个选项")
                if not (0 <= block["answer"] < len(block["options"])):
                    problems.append(f"{lid}: quiz answer 越界")
            if block["type"] == "code" and not block.get("runnable"):
                problems.append(f"{lid}: 代码块必须显式 runnable")
            if block["type"] == "table":
                width = len(block["headers"])
                for row in block["rows"]:
                    if len(row) != width:
                        problems.append(f"{lid}: 表格列数不一致")
            if block["type"] == "practice":
                if not block.get("output", "").strip():
                    problems.append(f"{lid}: practice 缺少 output")
                problems.extend(_check_forbidden(f"{lid} practice", block["code"]))
            if block["type"] == "code":
                problems.extend(_check_forbidden(f"{lid} 代码块", block["code"]))

        ex = item["exercise"]
        for key in ("title", "brief", "starterCode", "hint", "tests", "stdin", "xp", "solution"):
            if key not in ex:
                problems.append(f"{lid}: exercise 缺少 {key}")
        if not (3 <= len(ex["tests"]) <= 5):
            problems.append(f"{lid}: tests 应为 3-5 条，实际 {len(ex['tests'])}")
        if not ex["starterCode"].strip():
            problems.append(f"{lid}: starterCode 为空")
        if not ex["solution"].strip():
            problems.append(f"{lid}: solution 为空")
        problems.extend(_check_forbidden(f"{lid} starterCode", ex["starterCode"]))
        problems.extend(_check_forbidden(f"{lid} solution", ex["solution"]))
        for index, test in enumerate(ex["tests"]):
            problems.extend(_check_forbidden(f"{lid} test#{index + 1}", test))

    return problems


def main():
    problems = validate(LESSONS)
    if problems:
        for item in problems:
            print("STRUCT:", item)
        return 1

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(
        json.dumps(LESSONS, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    for item in LESSONS:
        body = "".join(b["text"] for b in item["blocks"] if b["type"] == "text")
        print(
            f"{item['id']}: {item['title']} | 块 {len(item['blocks'])} | "
            f"正文汉字 {_han(body)} | 判题 {len(item['exercise']['tests'])}"
        )
    print(f"js-expert: {len(LESSONS)} 讲 -> {OUT.name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

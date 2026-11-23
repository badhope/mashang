#!/usr/bin/env python3
"""生成 TypeScript 专业篇 8 讲，编号 ts31 到 ts38。

运行：python tools/gen_ts_expert.py
输出：content_packs/ts-expert.json

runnable 代码只用转译器擦得掉的语法：类型标注、interface、type 别名、泛型参数、
as、非空断言、enum。class、装饰器、命名空间、展开、默认参数、async/await、
可选链、空值合并、import/export 都不要用。条件类型、infer、映射类型、keyof、
泛型约束只放在 runnable:false 的参考块里，并注明“仅演示，本运行时无法执行”。

脚本会检查讲数、id、order、章节、块数、三个 text 块、正文汉字数、可运行块数量、
quiz 下标、exercise 用例数量和禁用语法。有一项不对就抛错，不写文件。
"""

import json
import pathlib
import re
import sys
import textwrap

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "content_packs" / "ts-expert.json"

CHAPTER = 64
CHAPTER_NAME = "TypeScript · 专业篇"
BASE_ORDER = 630

CJK = re.compile(r"[\u4e00-\u9fff]")

# runnable 代码里绝对不允许出现的语法（转译器不支持或会破坏 ES5 语义）
FORBIDDEN_RUNTIME = [
    "class ",
    "namespace ",
    "declare ",
    "import ",
    "export ",
    "async ",
    "await ",
    "=>",
    "...",
    "?.",
    "??",
    "`",
    ": void",
    "keyof",
    "infer",
    "readonly",
    " as const",
    " satisfies ",
    "let ",
    "const ",
]


def _t(src):
    return textwrap.dedent(src).strip("\n")


def h(text):
    return {"type": "heading", "text": text}


def p(text):
    return {"type": "text", "text": _t(text)}


def code(src):
    return {"type": "code", "code": _t(src), "runnable": True}


def code_static(src):
    return {"type": "code", "code": _t(src), "runnable": False}


def out(text):
    return {"type": "output", "text": _t(text)}


def table(headers, rows):
    return {"type": "table", "headers": headers, "rows": rows}


def diagram(text):
    return {"type": "diagram", "text": _t(text)}


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


def practice(title, src, output, hint):
    return {
        "type": "practice",
        "title": title,
        "code": _t(src),
        "output": _t(output),
        "hint": hint,
    }


def steps(items):
    return {"type": "steps", "items": items}


def exercise(title, brief, starter, hint, tests, solution, xp):
    return {
        "title": title,
        "brief": brief,
        "starterCode": _t(starter),
        "hint": hint,
        "tests": [_t(t) for t in tests],
        "stdin": [],
        "xp": xp,
        "solution": _t(solution),
    }


def lesson(lid, order_no, title, subtitle, xp, blocks, ex):
    return {
        "id": lid,
        "order": order_no,
        "chapter": CHAPTER,
        "chapterName": CHAPTER_NAME,
        "lang": "typescript",
        "title": title,
        "subtitle": subtitle,
        "xp": xp,
        "blocks": blocks,
        "exercise": ex,
    }


# ts31 高级泛型

TS31 = lesson(
    "ts31", BASE_ORDER,
    "高级泛型：约束与默认参数",
    "给类型参数划能力底线，也给推断留兜底值",
    140,
    [
        h("把约束当成一份能力契约"),
        p("""
            泛型参数默认是完全未知的，函数体里除了赋值和原样传递，几乎做不了别的事。约束 `T extends Shape` 把「未知」缩小成「至少具备 Shape 的能力」：函数体可以安全访问 Shape 的成员，调用方也必须提供合格的类型。进阶篇第 21 讲见过最基础的 length 约束，这一讲把它当成一份能力契约来设计。契约里只写函数体真正用到的成员：函数体只用 id，就写 `T extends { id: string }`，不要顺手把 name、createdAt 一起约束上。要求越少，能传进来的类型越多，函数越通用；要求越多，函数体越自由，调用方越受限，这条取舍线永远存在。

            约束可以组合。`T extends A & B` 要求同时满足两个形状，适合「既是实体又带时间戳」这类交叉能力；`K extends keyof T` 把键限制在 T 自己的字段集合里，是 pluck、pick、get 这类工具函数的骨架。`keyof T` 得到键名联合，`T[K]` 得到对应值类型，两者配合才能在编译期精确追踪「取了哪个字段、返回什么类型」。这些写法都只在类型层生效，运行时不产生任何检查。
        """),
        p("""
            默认类型参数解决「推断不出来时用什么」。写法是 `interface ApiResponse<T = unknown> { data: T }`：调用方只写 `ApiResponse`、不写尖括号时，T 自动取 unknown。默认值只在没有推断线索时兜底，不会压过实参推断；传了具体类型就用具体类型。默认值的选择很关键：选 any 等于把安全检查关掉，选 unknown 保留了「必须先收窄」的纪律，选 never 则表示「这个位置不该被使用」。多数公共 API 的默认值应该是 unknown，而不是 any。

            默认参数还有两个坑。第一，默认值不能引用后面的类型参数，`<T = U, U = string>` 不合法，默认值只能引用左边已声明的参数。第二，有默认值的参数必须放在没有默认值的参数后面，否则调用方无法省略。工程上把默认值当成 API 的「最小可用形态」：不写类型参数也能编译，写了则获得更精确的类型。这和函数默认参数的思路一致，只是作用在类型层。本应用的转译器不支持默认类型参数，`<T = string>` 会原样保留并导致语法错误，课程里只讲概念。
        """),
        p("""
            把约束和默认值放在一起看，它们解决的是两个方向的问题：约束回答「至少能做什么」，默认值回答「不知道时用什么」。设计泛型 API 时先问约束，再问默认值，最后才问要不要第二个类型参数。很多看起来需要三个类型参数的函数，其实一个就够；多出来的参数往往是因为把输入形状和输出形状都硬编码进了签名，而它们本可以由同一个 T 推导出来。

            本应用不支持 extends 约束、keyof、索引访问类型和默认类型参数，运行代码只能写「裸泛型」。替代方案是把能力变成参数：需要取长度就传一个取长度的函数，需要取字段就传字段名并在运行时读取。这样写少了编译期保证，但运行逻辑清楚、可测试，也不依赖转译器未实现的语法。等你在真实项目里用 tsc 时，再把同样的思路升级成约束即可，设计判断是一样的。

            给泛型 API 做设计评审时，可以用一张四问清单：类型参数之间的关系是否必须由调用方保持？约束里是否只包含函数体真正访问的成员？默认值是否选了最保守的 unknown？少一个类型参数是否会让表达力明显下降？四问里有两个答不上来，通常说明这个泛型是多余的，直接写具体类型更清楚。泛型是表达类型关系的工具，不是让签名显得高级的装饰；能用一个具体类型说清的地方，就不要引入参数。
        """),
        code("""
            interface Row {
              [key: string]: string | number;
            }

            function columnOf(rows: Row[], key: string): string[] {
              var out: string[] = [];
              for (var i = 0; i < rows.length; i++) {
                out.push(String(rows[i][key]));
              }
              return out;
            }

            function firstOf<T>(items: T[]): T {
              return items[0];
            }

            function pairOf<T>(a: T, b: T): T[] {
              return [a, b];
            }

            var rows: Row[] = [
              { id: 'A1', total: 30 },
              { id: 'B2', total: 45 }
            ];

            console.log(columnOf(rows, 'id').join(','));
            console.log(columnOf(rows, 'total').join(','));
            console.log(firstOf([10, 20]));
            console.log(pairOf('x', 'y').join('-'));
        """),
        out("A1,B2\n30,45\n10\nx-y"),
        code_static("""
            // 仅演示，本运行时无法执行（转译器不支持 extends / keyof / 默认类型参数）
            interface Entity {
              id: string;
            }

            function pluck<T extends Entity, K extends keyof T>(items: T[], key: K): T[K][] {
              return items.map(function (item: T): T[K] { return item[key]; });
            }

            interface ApiResponse<T = unknown> {
              ok: boolean;
              data: T;
              error: string | null;
            }

            type OrderSummary = ApiResponse<{ id: string; total: number }>;
        """),
        out("（纯类型声明，仅演示，本运行时无法执行，没有输出）"),
        table(["写法", "含义", "本应用可运行吗"], [
            ["<T extends { length: number }>", "T 必须具备 length 成员", "不可用，会留下 extends 导致语法错误"],
            ["<T = unknown>", "没有推断线索时用 unknown", "不可用，默认值会原样保留"],
            ["K extends keyof T", "K 只能是 T 的键名", "不可用，仅概念"],
            ["传入键名 / 提取函数", "把能力显式参数化", "可用，课程采用"],
        ]),
        tip("默认值优先选 unknown 而不是 any：unknown 强迫调用方在使用前收窄，any 则把检查全部关掉。只有确实允许任何操作的位置才考虑 any，并且要写明理由。"),
        warn("本应用只做类型擦除，不实现泛型约束与默认参数。写 `<T extends X>` 或 `<T = X>` 会原样保留，Rhino 直接报语法错误，判题必然失败。"),
        quiz("`interface ApiResponse<T = unknown>` 中，调用方只写 `ApiResponse` 时 T 是什么？", [
            "any",
            "unknown",
            "never",
            "string",
        ], 1, "没有显式类型参数也没有推断线索时，T 取默认值 unknown；默认值不会退化成 any。"),
        practice("按字段取一列", """
            interface Row {
              [key: string]: string | number;
            }

            function columnOf(rows: Row[], key: string): string[] {
              var out: string[] = [];
              for (var i = 0; i < rows.length; i++) {
                out.push(String(rows[i][key]));
              }
              return out;
            }

            console.log(columnOf([{ id: 'A', total: 1 }], 'total')[0]);
        """, "1", "运行时按键名取值，再统一转成字符串。"),
    ],
    exercise(
        "按字段去重与拼接",
        "实现 distinctValues(rows, key)：按 key 取字段，返回去重后的字符串数组，保持首次出现顺序；实现 joinValues(rows, key, sep)：把该字段的所有值用 sep 连接。",
        """
        interface Row {
          [key: string]: string | number;
        }

        function distinctValues(rows: Row[], key: string): string[] {
          // 保持首次出现顺序去重
          return [];
        }

        function joinValues(rows: Row[], key: string, sep: string): string {
          // 取所有值并用 sep 连接
          return '';
        }
        """,
        "去重可以用 out.indexOf(value) < 0 判断；拼接先收集到数组，再调用 join(sep)。",
        [
            "assert(distinctValues([{id:'A'},{id:'B'},{id:'A'}], 'id').join(',') === 'A,B', '应按首次出现顺序去重')",
            "assert(distinctValues([], 'id').length === 0, '空数组应返回空数组')",
            "assert(joinValues([{id:'A'},{id:'B'}], 'id', '-') === 'A-B', 'joinValues 应用指定分隔符')",
            "assert(joinValues([{id:'A'}], 'id', '-') === 'A', '单元素也要正确拼接')",
        ],
        """
        interface Row {
          [key: string]: string | number;
        }

        function distinctValues(rows: Row[], key: string): string[] {
          var out: string[] = [];
          for (var i = 0; i < rows.length; i++) {
            var value: string = String(rows[i][key]);
            if (out.indexOf(value) < 0) {
              out.push(value);
            }
          }
          return out;
        }

        function joinValues(rows: Row[], key: string, sep: string): string {
          var parts: string[] = [];
          for (var i = 0; i < rows.length; i++) {
            parts.push(String(rows[i][key]));
          }
          return parts.join(sep);
        }
        """,
        150,
    ),
)

# ts32 类型级编程

TS32 = lesson(
    "ts32", BASE_ORDER + 1,
    "类型级编程入门",
    "条件类型、infer 与映射类型",
    145,
    [
        h("类型系统也是一门语言"),
        p("""
            进阶篇第 22、23 讲分别看过条件类型与映射类型的语法形状，这一讲把它们当成一门小语言来用：infer 捕获、键重映射、递归展开，以及什么时候不该写这些。类型级编程指的是让类型系统替你计算类型。它和运行时代码是两套语言：运行时你写函数、判断、循环；类型层你写条件类型、infer、映射类型。条件类型 `T extends U ? X : Y` 是类型层的 if，判断的是「T 能否赋值给 U」。它有两条特殊规则：T 是联合类型时按成员分配，逐个判断再合并；T 还是未确定的类型参数时延迟求值，等调用时才计算。这两条规则是很多工具类型的实现基础，也是错误信息难读的原因。

            infer 是条件类型里的模式匹配变量。`T extends Array<infer U> ? U : never` 表示「如果 T 是数组，把元素类型捕获为 U」。它可以从函数类型里取参数、取返回值，也可以从 Promise、元组、构造函数里提取内部类型。把 infer 理解成「在类型结构里挖一个洞，让编译器把匹配到的部分填进来」，比死记语法有效。写不出来时先问：要匹配的结构长什么样？洞应该挖在哪一层？
        """),
        p("""
            映射类型是类型层的循环：`{ [K in keyof T]: 新类型 }` 遍历 T 的每个键，为每个键生成新字段。加上问号变可选，加上 `-?` 去掉可选，加上 readonly 变只读，加上 `-readonly` 去掉只读。键还能重映射，例如把每个字段变成 `get` 前缀的同名方法：`{ [K in keyof T as GetName<K>]: () => T[K] }`。模板字面量类型让键名可以拼接、替换、提取，这是类型层少有的字符串处理能力。

            递归类型让映射类型可以深入嵌套结构。`type DeepPartial<T> = { [K in keyof T]?: T[K] extends object ? DeepPartial<T[K]> : T[K] }` 描述「每一层都可选」的类型，标准库没有内置，只能自己写。递归必须有终止条件，否则编译器会报「类型实例化过深」。判断标准是：递归只发生在结构确实嵌套时，遇到基础类型就停下。写这类类型要像写递归函数一样，先想清楚出口。
        """),
        p("""
            类型级编程很容易滑向炫技。业务代码里真正需要自己写条件类型或映射类型的场景并不多：标准化数据结构、给第三方类型做适配、从既有类型派生视图。其余时候，标准库的 Partial、Pick、Omit、ReturnType、Parameters 已经够用。判断标准是：这段类型能不能用一句话说清它想表达的业务含义？能，就值得写；不能，多半是过度设计。

            本应用的转译器不支持条件类型、infer、映射类型和模板字面量类型，这些语法写进运行代码会留下残渣。课程把它们放进 runnable:false 的参考块，并标注「仅演示，本运行时无法执行」。运行代码只演示这些类型在运行时的等价物：数据形状的转换、字段的重命名、可选值的补全。理解「类型层在算什么」比记住语法重要，因为换到任何一门有类型系统的语言，思路都是相通的。

            调试类型级代码有一套固定手法。第一步把复杂别名拆成两三个中间别名，逐个悬停查看求值结果；第二步用具体类型做最小复现，把联合类型换成单个成员，排除分配律的干扰；第三步检查递归出口，确认基础类型分支返回的是自己而不是继续递归。类型错误信息往往把整个定义展开成几百行，盯着它读没有意义，缩小输入才是正解。这套方法和调试运行时代码完全一致：先定位，再缩小，最后验证。
        """),
        code_static("""
            // 仅演示，本运行时无法执行（转译器不支持条件类型 / infer / 映射类型）
            type ElementOf<T> = T extends Array<infer U> ? U : never;

            type Unwrap<T> = T extends Promise<infer U> ? U : T;

            type Mutable<T> = {
              -readonly [K in keyof T]: T[K];
            };

            type DeepPartial<T> = {
              [K in keyof T]?: T[K] extends object ? DeepPartial<T[K]> : T[K];
            };

            type ElementOfNumber = ElementOf<number[]>;   // number
            type Unwrapped = Unwrap<Promise<string>>;     // string
        """),
        out("（纯类型声明，仅演示，本运行时无法执行，没有输出）"),
        code("""
            interface User {
              id: string;
              name: string;
              email: string;
            }

            interface PublicUser {
              id: string;
              name: string;
            }

            function pick(user: User, keys: string[]): any {
              var source: any = user;
              var out: any = {};
              for (var i = 0; i < keys.length; i++) {
                out[keys[i]] = source[keys[i]];
              }
              return out;
            }

            function withOptional(user: User, patch: any): User {
              var out: User = { id: user.id, name: user.name, email: user.email };
              if (patch.name !== undefined) {
                out.name = patch.name;
              }
              if (patch.email !== undefined) {
                out.email = patch.email;
              }
              return out;
            }

            var u: User = { id: 'u1', name: 'V', email: 'v@x.io' };
            var pub: PublicUser = pick(u, ['id', 'name']);
            console.log(pub.id, pub.name);
            var next: User = withOptional(u, { name: 'Judy' });
            console.log(next.name, next.email);
        """),
        out("u1 V\nJudy v@x.io"),
        table(["类型层写法", "运行时等价物", "本应用"], [
            ["T extends U ? X : Y", "按运行时条件分支", "不可运行，仅概念"],
            ["{ [K in keyof T]: ... }", "遍历键并重建对象", "映射类型不可运行，普通循环可运行"],
            ["infer U", "从结构中取出内层值", "不可运行，仅概念"],
            ["Partial<T>", "逐字段判断 undefined 后覆盖", "可运行，本讲示例"],
        ]),
        tip("读懂类型级代码的顺序：先找最外层的条件或映射，再逐个展开别名，最后才看 infer 捕获了什么。不要试图一眼看完整个表达式。"),
        warn("条件类型和映射类型只存在于类型层，写了也不会生成运行代码。本应用会尝试擦除但结果不可靠，参考块里的代码不要粘进练习；练习只接受可运行的 ES5 写法。"),
        quiz("`T extends Array<infer U> ? U : never` 中 infer U 捕获的是什么？", [
            "数组本身",
            "数组元素类型",
            "数组长度",
            "数组的方法",
        ], 1, "infer U 在 Array<...> 的内部挖了一个洞，匹配到的元素类型被捕获为 U。"),
        steps([
            "先写出要匹配的完整类型结构，例如 Array<number>。",
            "在会变化的位置放 infer 变量，例如 Array<infer U>。",
            "用条件类型判断并返回捕获结果，例如 T extends Array<infer U> ? U : never。",
            "用几个具体类型展开验证：number[] 应得到 number，string 应得到 never。",
        ]),
    ],
    exercise(
        "字段投影与补丁合并",
        "实现 project(user, keys)：返回只包含 keys 中字段的新对象；实现 applyPatch(user, patch)：返回新 User，只覆盖 patch 中不为 undefined 的 name / email。",
        """
        interface User {
          id: string;
          name: string;
          email: string;
        }

        function project(user: User, keys: string[]): any {
          return {};
        }

        function applyPatch(user: User, patch: any): User {
          return user;
        }
        """,
        "project 先建空对象，再遍历 keys 把 user 的对应字段抄进去；applyPatch 先完整复制三个字段，再按 patch 里是否为 undefined 决定覆盖。",
        [
            "assert(project({id:'u1',name:'V',email:'e'}, ['id']).name === undefined, 'project 不应带出未指定的字段')",
            "assert(project({id:'u1',name:'V',email:'e'}, ['id','email']).email === 'e', 'project 应保留指定字段')",
            "assert(applyPatch({id:'u1',name:'V',email:'e'}, {name:'J'}).name === 'J', 'applyPatch 应覆盖提供的字段')",
            "assert(applyPatch({id:'u1',name:'V',email:'e'}, {}).email === 'e', 'applyPatch 不应清空未提供的字段')",
        ],
        """
        interface User {
          id: string;
          name: string;
          email: string;
        }

        function project(user: User, keys: string[]): any {
          var source: any = user;
          var out: any = {};
          for (var i = 0; i < keys.length; i++) {
            out[keys[i]] = source[keys[i]];
          }
          return out;
        }

        function applyPatch(user: User, patch: any): User {
          var out: User = { id: user.id, name: user.name, email: user.email };
          if (patch.name !== undefined) {
            out.name = patch.name;
          }
          if (patch.email !== undefined) {
            out.email = patch.email;
          }
          return out;
        }
        """,
        155,
    ),
)

# ts33 声明合并

TS33 = lesson(
    "ts33", BASE_ORDER + 2,
    "声明合并与模块增强",
    "给已有类型补上缺失的一块",
    150,
    [
        h("同名声明为什么会自动拼在一起"),
        p("""
            进阶篇第 27 讲讲过用 .d.ts 从零描述一个外部模块；这一讲处理另一种情况：类型已经存在，只想给它补一块。声明合并是 TypeScript 的一个特殊规则：同名的 interface 声明会自动合并成一个，字段取并集；同名的 namespace 会合并成员；namespace 和 class、function、enum 也能合并，用来给运行时对象补静态成员。合并发生在类型层，不产生新的运行时代码。它的价值在于扩展别人定义的类型：第三方库的 interface 少了一个字段，你不需要改它的源码，在自己的声明里再写一遍同名 interface 就能补上。

            interface 合并的规则要记牢：同名非函数成员的类型必须一致，不一致会报错；方法可以按重载的方式追加；后声明的接口不能悄悄改掉前一个接口的字段类型。合并没有顺序要求，但可读性上建议把扩展集中放在一个文件里，并在注释里写明「为什么补这个字段」。散落在各处的同名 interface 会让读者以为字段凭空出现，排查问题时非常痛苦。
        """),
        p("""
            模块增强是针对模块的合并：在 `declare module 'some-lib' { interface Options { debug?: boolean } }` 里写同名类型，就能给已安装模块的类型补字段。它和「声明新模块」不同：新模块是从零描述一个没有类型的包，增强是修改已有模块的类型。写增强时必须先 import 或 export 让文件成为模块，否则 declare module 会被当成全新的环境声明，而不是增强。

            全局增强用 `declare global { interface Window { ... } }`，只能写在模块文件里。它影响整个项目的全局类型，风险最大：一旦补错字段，所有文件都会受影响。更安全的替代是包装函数或局部适配器：需要额外字段时，在调用处构造一个扩展对象，而不是污染全局。模块增强适合「确实无法改源码、又必须在多处使用」的场景，比如给测试框架补自定义匹配器、给 UI 库补主题字段。
        """),
        p("""
            声明合并有三个常见风险。第一是版本漂移：上游库升级后自己加了同名字段，你的增强可能和新字段冲突，类型直接报错。第二是隐式依赖：增强写在某个文件里，只有该文件被 include 时才生效，CI 换一个入口就失效。第三是命名污染：给全局补太多字段，读者分不清哪些是标准库、哪些是项目补的。控制方法是把增强集中到 types 目录，在 tsconfig 的 include 里显式列出，并在每个增强文件顶部写清来源和原因。

            本应用的转译器不支持 namespace、模块增强和 declare module，遇到 declare 开头的行会整行删除，但不会真正把类型注入任何地方。所以这些内容只能作为只读片段讲解。运行代码演示的是「合并」在运行时的等价物：把默认配置和用户配置逐字段合并成一个新对象。理解这个等价物，你就能在没有类型系统的情况下，照样把扩展逻辑写对。

            判断该不该做增强，用一个简单的问题：这个字段是「库漏了」还是「我的业务需要」？库漏了，值得写增强并在上游提 issue；我的业务需要，应该在自己的类型里做包装，而不是改库的类型。包装的做法是定义一个继承或交叉出来的新类型，在项目内部使用，调用第三方函数时再做一次显式转换。这样升级依赖时不会冲突，业务扩展也不会污染库的公共契约。增强是为了补缺口，不是为了表达偏好。
        """),
        code_static("""
            // 仅演示，本运行时无法执行（转译器不支持模块增强 / 模块声明）
            // types/vendor.d.ts
            import 'some-ui-lib';

            declare module 'some-ui-lib' {
              interface Theme {
                accent?: string;
                density?: 'compact' | 'cozy';
              }
            }

            declare global {
              interface Window {
                __APP_VERSION__: string;
              }
            }

            export {};
        """),
        out("（纯类型声明，仅演示，本运行时无法执行，没有输出）"),
        code("""
            interface Options {
              retries: number;
              timeout: number;
              verbose: boolean;
            }

            function mergeOptions(base: Options, patch: any): Options {
              var out: Options = { retries: base.retries, timeout: base.timeout, verbose: base.verbose };
              if (patch.retries !== undefined) {
                out.retries = patch.retries;
              }
              if (patch.timeout !== undefined) {
                out.timeout = patch.timeout;
              }
              if (patch.verbose !== undefined) {
                out.verbose = patch.verbose;
              }
              return out;
            }

            var base: Options = { retries: 2, timeout: 1000, verbose: false };
            var merged: Options = mergeOptions(base, { timeout: 3000 });
            console.log(merged.retries, merged.timeout, merged.verbose);
            console.log(base.timeout);
        """),
        out("2 3000 false\n1000"),
        table(["合并形式", "合并对象", "典型用途", "本应用"], [
            ["interface 同名声明", "多个 interface", "给第三方类型补字段", "不可运行，仅概念"],
            ["declare module", "已有模块", "模块增强", "不可运行，仅概念"],
            ["declare global", "全局作用域", "补 Window 等全局对象", "不可运行，仅概念"],
            ["对象逐字段复制", "运行时对象", "默认配置 + 用户配置", "可运行，本讲示例"],
        ]),
        tip("模块增强文件要让 TypeScript 认为它是模块：文件里至少有一个 import 或 export。只有 declare module 而没有顶层 import/export 时，它会被当作环境声明，可能覆盖而不是增强原模块。"),
        warn("增强第三方类型前先确认上游版本。如果新版本已经补了同名字段，你的增强会冲突；升级依赖时把增强文件当成迁移检查点，逐个核对。"),
        quiz("两个同名 interface 声明合并时，同名字段类型不一致会怎样？", [
            "后者覆盖前者",
            "编译器报错",
            "自动合并成联合类型",
            "字段被删除",
        ], 1, "interface 合并要求同名非函数成员类型完全一致，不一致会直接报错，不存在覆盖规则。"),
        practice("合并默认配置", """
            interface Options {
              retries: number;
              timeout: number;
            }

            function mergeOptions(base: Options, patch: any): Options {
              var out: Options = { retries: base.retries, timeout: base.timeout };
              if (patch.timeout !== undefined) {
                out.timeout = patch.timeout;
              }
              return out;
            }

            var base: Options = { retries: 2, timeout: 1000 };
            console.log(mergeOptions(base, { timeout: 500 }).timeout, base.timeout);
        """, "500 1000", "合并不修改原对象，base.timeout 仍然是 1000。"),
    ],
    exercise(
        "配置合并与变更字段",
        "实现 mergeConfig(base, patch)：返回新 Config，只覆盖 patch 中不为 undefined 的字段；实现 changedKeys(base, patch)：返回实际发生变化的字段名，按字母升序。",
        """
        interface Config {
          host: string;
          port: number;
          debug: boolean;
        }

        function mergeConfig(base: Config, patch: any): Config {
          return base;
        }

        function changedKeys(base: Config, patch: any): string[] {
          return [];
        }
        """,
        "mergeConfig 先完整复制三个字段，再逐个判断 patch 字段是否为 undefined；changedKeys 只收集「提供了且不等于 base」的字段，最后 sort()。",
        [
            "assert(mergeConfig({host:'localhost',port:80,debug:false}, {port:8080}).port === 8080, '应覆盖提供的 port')",
            "assert(mergeConfig({host:'localhost',port:80,debug:false}, {}).host === 'localhost', '未提供字段应保留原值')",
            "assert(mergeConfig({host:'localhost',port:80,debug:false}, {debug:true}).debug === true, '应覆盖 debug')",
            "assert(changedKeys({host:'localhost',port:80,debug:false}, {port:8080,debug:true}).join(',') === 'debug,port', 'changedKeys 应按字母升序')",
        ],
        """
        interface Config {
          host: string;
          port: number;
          debug: boolean;
        }

        function mergeConfig(base: Config, patch: any): Config {
          var out: Config = { host: base.host, port: base.port, debug: base.debug };
          if (patch.host !== undefined) {
            out.host = patch.host;
          }
          if (patch.port !== undefined) {
            out.port = patch.port;
          }
          if (patch.debug !== undefined) {
            out.debug = patch.debug;
          }
          return out;
        }

        function changedKeys(base: Config, patch: any): string[] {
          var out: string[] = [];
          if (patch.host !== undefined && patch.host !== base.host) {
            out.push('host');
          }
          if (patch.port !== undefined && patch.port !== base.port) {
            out.push('port');
          }
          if (patch.debug !== undefined && patch.debug !== base.debug) {
            out.push('debug');
          }
          out.sort();
          return out;
        }
        """,
        160,
    ),
)

# ts34 数据层

TS34 = lesson(
    "ts34", BASE_ORDER + 3,
    "类型安全的数据层",
    "仓储接口、DTO 与错误类型",
    155,
    [
        h("把外部格式与内部模型分开"),
        p("""
            进阶篇第 29 讲的数据管道关注「数据怎么流」，这一讲关注「数据停在哪、由谁负责」：仓储、DTO、错误类型三层各管一段。数据层是类型系统收益最高的地方，因为它同时面对外部不可信数据和内部稳定模型。典型分层是三层：DTO 描述外部输入输出，字段名跟着接口走，可能是 snake_case；领域实体描述业务概念，字段名统一、值经过校验；行记录描述存储结构，可能多一个 version 或 deleted_at。三层之间用显式映射函数连接，不允许把 DTO 直接当实体用。多写几个映射函数看起来啰嗦，但它把外部格式变化隔离在一处，业务代码不用跟着改。

            仓储接口把「怎么存」和「怎么用」分开。`interface OrderRepository { findById: (id: string) => Order | null; save: (order: Order) => void; list: () => Order[] }` 只描述能力，不暴露数组、SQL 或缓存。调用方依赖接口，测试时可以换成内存实现，生产环境换成数据库实现，业务代码一行不改。这就是依赖倒置在类型层面的样子：先定义能力，再提供实现。
        """),
        p("""
            错误类型有两种表达方式：抛异常和返回值。抛异常适合程序错误，比如参数不合法、状态不可能出现；返回值适合业务失败，比如库存不足、支付被拒。类型安全的数据层推荐把业务失败建模成返回值：`interface Success<T> { ok: true; value: T }` 与 `interface Failure { ok: false; error: DbError }` 组成 `type Result<T> = Success<T> | Failure`。调用方必须先看 ok 才能取 value，编译器会逼你处理失败分支。异常适合让进程快速失败，不要用它表达可预期的业务结果。

            DbError 本身也应该是结构化的：code 是稳定的机器可读标识，message 是给人看的说明，retryable 表示是否可重试。不要只抛一个字符串，字符串无法被程序判断，也无法被测试断言。把错误码定义成字面量联合，调用方 switch 时就能穷尽检查。日志里记录 code 和上下文，不要记录用户隐私。
        """),
        p("""
            实现仓储时有几个类型细节值得注意。第一，findById 的返回类型是 `Order | null` 而不是 Order，空值必须显式表达；本应用不支持可选链，调用方要先判 null。第二，save 接收完整实体，更新用单独的方法或明确的 patch 类型，不要让 save 同时承担插入和部分更新两种语义。第三，列表查询返回新数组，避免调用方修改内部缓存。第四，所有从外部进入的数据先经过 parse 函数，校验通过后才构造成实体。

            分页是数据层最常见的接口演化。一开始 list() 返回全部，数据量上来后要改成分页，所有调用方都得改。更稳的做法是第一天就留出 list(page, size) 或返回 Page<T>，即使暂时只返回一页。类型设计要考虑演化成本：新增可选字段是兼容的，改变返回结构不是。把「以后可能要加的东西」提前放进类型里，比事后改十个调用点便宜。

            还有一条容易被忽略的纪律：数据层不要返回内部对象的引用。仓储里缓存了实体数组，list 直接返回 rows，调用方一 sort 就把内部顺序改了，后续查询全部错乱。正确做法是返回副本，或者返回只读视图。本应用没有 readonly 数组类型，运行时就靠 slice() 复制来保证。这条规则和类型无关，但类型设计会诱导你忽略它：标注成 Order[] 看起来无害，实际共享了可变状态。类型安全不等于数据安全，两者要分别保证。

            错误处理的分层也要提前想清楚。仓储层把数据库异常转成 DbError，服务层把 DbError 转成业务错误，接口层再转成 HTTP 状态码，每一层只认识自己那一层的错误类型。不要在仓储里直接拼用户可见的文案，也不要让数据库驱动的错误对象穿过三层。转换函数写起来很枯燥，但它是定位线上问题的分界线：看到 code 就知道失败发生在哪一层，日志里不用再猜。这一讲的所有设计都围绕一个判断：类型是给人看的契约，不是给编译器看的装饰。
        """),
        code("""
            interface Order {
              id: string;
              total: number;
            }

            interface Page<T> {
              items: T[];
              total: number;
            }

            interface DbError {
              code: string;
              message: string;
            }

            interface Success<T> {
              ok: true;
              value: T;
              error: null;
            }

            interface Failure {
              ok: false;
              value: null;
              error: DbError;
            }

            type Result<T> = Success<T> | Failure;

            interface OrderRepository {
              findById: (id: string) => Order | null;
              save: (order: Order) => void;
              list: (page: number, size: number) => Page<Order>;
            }

            function createRepo(): OrderRepository {
              var rows: Order[] = [];

              function findRow(id: string): Order | null {
                for (var i = 0; i < rows.length; i++) {
                  if (rows[i].id === id) {
                    return rows[i];
                  }
                }
                return null;
              }

              return {
                findById: function (id: string): Order | null {
                  return findRow(id);
                },
                save: function (order: Order) {
                  var existing: Order | null = findRow(order.id);
                  if (existing === null) {
                    rows.push({ id: (order.id), total: (order.total) });
                  } else {
                    existing.total = order.total;
                  }
                },
                list: function (page: number, size: number): Page<Order> {
                  var start: number = (page - 1) * size;
                  return { items: rows.slice(start, start + size), total: rows.length };
                }
              };
            }

            function parseOrder(input: any): Result<Order> {
              if (input === null || typeof input !== 'object' || typeof input.id !== 'string' || input.id === '') {
                return { ok: false, value: null, error: { code: 'invalid_id', message: 'id 必须是非空字符串' } };
              }
              var total: number = Number(input.total);
              if (isNaN(total) || total < 0) {
                return { ok: false, value: null, error: { code: 'invalid_total', message: 'total 必须是非负数字' } };
              }
              return { ok: true, value: { id: input.id, total: total }, error: null };
            }

            var repo: OrderRepository = createRepo();
            var parsed: Result<Order> = parseOrder({ id: 'A1', total: '30' });
            if (parsed.ok) {
              repo.save(parsed.value);
            }
            repo.save({ id: 'B2', total: 45 });
            var page: Page<Order> = repo.list(1, 10);
            console.log(page.total, page.items.length);
            var found: Order | null = repo.findById('A1');
            if (found !== null) {
              console.log(found.total);
            }
            var bad: Result<Order> = parseOrder({ id: '', total: 1 });
            if (!bad.ok) {
              console.log(bad.ok, bad.error.code);
            }
        """),
        out("2 2\n30\nfalse invalid_id"),
        table(["层", "代表类型", "职责", "变化频率"], [
            ["DTO", "OrderDto", "对齐外部接口字段", "高，随接口版本变化"],
            ["领域实体", "Order", "业务规则与不变量", "低，随业务模型变化"],
            ["行记录", "OrderRow", "对齐数据库字段", "中，随表结构变化"],
            ["仓储接口", "OrderRepository", "只描述存取能力", "低，按用例扩展"],
        ]),
        tip("仓储接口只描述能力，不描述实现。方法签名里不要出现数组、SQL、缓存这些实现词；一旦出现，说明抽象漏了。"),
        warn("不要用空对象表示缺失：`{} as Order` 会让调用方以为数据完整，错误被推迟到访问字段时才爆。缺失就返回 null，并让调用方显式处理。"),
        quiz("为什么业务失败更适合用返回值 Result，而不是抛异常？", [
            "异常性能更差",
            "返回值把失败变成类型的一部分，编译器会逼调用方处理",
            "异常无法携带错误码",
            "Result 运行更快",
        ], 1, "Result 是联合类型，调用方必须先判断 ok 才能取 value，失败分支无法被静默忽略；异常则容易被上层漏接。"),
        practice("内存仓储", """
            interface Item {
              id: string;
              total: number;
            }

            function createRepo() {
              var rows: Item[] = [];
              return {
                save: function (item: Item) { rows.push(item); },
                count: function (): number { return rows.length; }
              };
            }

            var repo = createRepo();
            repo.save({ id: 'A', total: 10 });
            console.log(repo.count());
        """, "1", "闭包里的 rows 就是内存表，save 直接 push。"),
        steps([
            "先定义 DTO，字段完全对齐外部接口，不夹带业务方法。",
            "再定义领域实体，字段用统一命名，并写下必须成立的不变量。",
            "写 toDomain(dto) 与 toDto(entity) 两个映射函数，所有转换集中在这里。",
            "定义仓储接口，只写业务需要的存取能力，不暴露底层数据结构。",
            "最后写内存实现与测试，确认 findById 的空值、save 的更新语义都被覆盖。",
        ]),
    ],
    exercise(
        "订单仓储与结果类型",
        "实现 parseOrder(input)：校验 id 为非空字符串、total 为非负数字，成功返回 { ok: true, value, error: null }，失败返回对应 code 的 Failure；实现 createRepo()：save 按 id 覆盖更新，findById 找不到返回 null，count 返回条数。",
        """
        interface Order {
          id: string;
          total: number;
        }

        interface DbError {
          code: string;
          message: string;
        }

        interface Success<T> {
          ok: true;
          value: T;
          error: null;
        }

        interface Failure {
          ok: false;
          value: null;
          error: DbError;
        }

        type Result<T> = Success<T> | Failure;

        interface Repo {
          save: (order: Order) => void;
          findById: (id: string) => Order | null;
          count: () => number;
        }

        function parseOrder(input: any): Result<Order> {
          return { ok: false, value: null, error: { code: 'todo', message: 'todo' } };
        }

        function createRepo(): Repo {
          return {
            save: function (order: Order) {},
            findById: function (id: string): Order | null { return null; },
            count: function (): number { return 0; }
          };
        }
        """,
        "parseOrder 先判 input 和 id，再 Number(total) 并判 isNaN / 负数；createRepo 用闭包数组，save 先 findById 决定 push 还是改 total。",
        [
            "assert(parseOrder({id:'A', total:10}).ok === true, '合法输入应返回 ok')",
            "assert(parseOrder({id:'', total:1}).error.code === 'invalid_id', '空 id 应返回 invalid_id')",
            "assert(parseOrder({id:'A', total:-1}).error.code === 'invalid_total', '负数金额应返回 invalid_total')",
            "var repo = createRepo(); repo.save({id:'A', total:1}); repo.save({id:'A', total:5}); assert(repo.count() === 1 && repo.findById('A').total === 5, '同一 id 应覆盖更新而不是重复插入')",
        ],
        """
        interface Order {
          id: string;
          total: number;
        }

        interface DbError {
          code: string;
          message: string;
        }

        interface Success<T> {
          ok: true;
          value: T;
          error: null;
        }

        interface Failure {
          ok: false;
          value: null;
          error: DbError;
        }

        type Result<T> = Success<T> | Failure;

        interface Repo {
          save: (order: Order) => void;
          findById: (id: string) => Order | null;
          count: () => number;
        }

        function parseOrder(input: any): Result<Order> {
          if (input === null || typeof input !== 'object' || typeof input.id !== 'string' || input.id === '') {
            return { ok: false, value: null, error: { code: 'invalid_id', message: 'id 必须是非空字符串' } };
          }
          var total: number = Number(input.total);
          if (isNaN(total) || total < 0) {
            return { ok: false, value: null, error: { code: 'invalid_total', message: 'total 必须是非负数字' } };
          }
          return { ok: true, value: { id: input.id, total: total }, error: null };
        }

        function createRepo(): Repo {
          var rows: Order[] = [];
          function findRow(id: string): Order | null {
            for (var i = 0; i < rows.length; i++) {
              if (rows[i].id === id) {
                return rows[i];
              }
            }
            return null;
          }
          return {
            save: function (order: Order) {
              var existing: Order | null = findRow(order.id);
              if (existing === null) {
                rows.push({ id: (order.id), total: (order.total) });
              } else {
                existing.total = order.total;
              }
            },
            findById: function (id: string): Order | null {
              return findRow(id);
            },
            count: function (): number { return rows.length; }
          };
        }
        """,
        165,
    ),
)

# ts35 领域建模

TS35 = lesson(
    "ts35", BASE_ORDER + 4,
    "领域建模：可辨识联合",
    "让非法状态无法表示，让漏分支无法编译",
    160,
    [
        h("状态不是几个布尔开关"),
        p("""
            进阶篇第 25 讲介绍过可辨识联合的语法，这一讲把它落到订单状态机上，并补上穷尽检查与非法转换的处理。领域建模的核心问题是让非法状态无法表示。用三个布尔字段 isLoading、hasError、isEmpty 描述界面状态，会产生八种组合，其中五种是非法的；每加一个标志，组合数再翻一倍。可辨识联合把状态压成互斥的几种：`{ kind: 'loading' } | { kind: 'ready'; data: Order[] } | { kind: 'failed'; error: string }`，合法状态只有三种，非法组合根本构造不出来。标签字段 kind 是字面量类型，switch 时编译器能逐个收窄。

            可辨识联合的关键不是语法，而是每个分支只带自己需要的字段。ready 有 data，failed 有 error，loading 什么都没有。不要为了统一把所有字段都加上再留空，那样又回到了布尔标志的老路。字段的缺失本身就是信息：看到 failed 没有 data，读者立刻知道失败时不该读数据。
        """),
        p("""
            穷尽检查是穷尽联合所有分支的编译期保证。写法是在 switch 的 default 分支调用 `assertNever(value: never): never`，如果还有成员没处理，传入的值不是 never，tsc 会报错；全部处理完时 default 不可达，函数永远不会执行。运行时它抛异常，作为最后一道防线。两者配合，新增一个状态时，所有没更新的 switch 都会亮红，不会静默漏掉。

            本应用不做类型检查，never 标注也会被擦除，所以运行时的穷尽检查要靠自己写：default 分支抛出带 kind 值的错误，测试里专门构造未知 kind 验证它会抛。类型层的 assertNever 只是编译期保险，运行时保险必须单独写。不要把两者混为一谈——很多线上事故就是「以为类型检查覆盖了，其实新数据来自外部，类型标注根本管不住」。
        """),
        p("""
            状态机建模还有两个实践要点。第一，状态转换写成纯函数：`transition(state, event): State`，输入旧状态和事件，返回新状态，不修改参数。这样每次转换都能单独测试，也能把转换历史记录下来。第二，非法转换要有明确结果：返回原状态、抛错、或返回 Result，三选一并在团队内统一。最糟的是静默忽略，调用方以为成功了，实际状态没变。

            事件和命令要分开。命令是「我想做什么」，比如 PlaceOrder、CancelOrder，可能被拒绝；事件是「已经发生了什么」，比如 OrderPlaced、OrderCancelled，不可撤销。类型上可以用两套可辨识联合，也可以合并成一个大联合，但语义要清楚。订单这类业务对象，建议状态用联合、转换用纯函数、历史用事件数组，三者职责分明，测试也好写。

            穷尽检查真正的价值在演化。半年后产品要加「退款中」状态，你只需要在联合里加一个成员，编译器会列出所有没处理的 switch；如果没有穷尽检查，漏掉的分支会在某个深夜返回 undefined，然后变成线上事故。这就是「让编译器替你找漏」的意义。运行时兜底仍然要写，因为外部数据可能带来未知状态；类型层的穷尽检查管的是你自己的代码，运行时兜底管的是真实世界，两道防线缺一不可。

            领域建模还要防止「状态膨胀」。订单状态从五个涨到二十个时，转换表会变成一张巨大的二维矩阵，维护成本急剧上升。这时应该回头看：有些状态是不是可以合并？有些是不是其实属于另一个维度，比如支付状态和物流状态本来就该分开建模，而不是塞进同一个联合？拆成两个独立的联合，每个都只有三五个成员，转换规则各自独立，反而比一个大联合清楚。模型的分界线应该跟着业务概念走，而不是跟着某一次需求走。
        """),
        code("""
            type OrderState = 'draft' | 'placed' | 'paid' | 'shipped' | 'cancelled';

            interface Order {
              id: string;
              state: OrderState;
              total: number;
            }

            interface Transition {
              from: OrderState;
              event: string;
              to: OrderState;
            }

            var RULES: Transition[] = [
              { from: 'draft', event: 'place', to: 'placed' },
              { from: 'placed', event: 'pay', to: 'paid' },
              { from: 'placed', event: 'cancel', to: 'cancelled' },
              { from: 'paid', event: 'ship', to: 'shipped' },
              { from: 'paid', event: 'cancel', to: 'cancelled' }
            ];

            function transition(order: Order, event: string): Order {
              for (var i = 0; i < RULES.length; i++) {
                if (RULES[i].from === order.state && RULES[i].event === event) {
                  return { id: order.id, state: RULES[i].to, total: order.total };
                }
              }
              return { id: order.id, state: order.state, total: order.total };
            }

            function label(state: OrderState): string {
              switch (state) {
                case 'draft':
                  return '草稿';
                case 'placed':
                  return '已下单';
                case 'paid':
                  return '已支付';
                case 'shipped':
                  return '已发货';
                case 'cancelled':
                  return '已取消';
              }
              throw new Error('unexpected state: ' + state);
            }

            var order: Order = { id: 'A1', state: 'draft', total: 99 };
            var events: string[] = ['place', 'pay', 'ship'];
            for (var i = 0; i < events.length; i++) {
              order = transition(order, events[i]);
              console.log(events[i] + ' -> ' + order.state + ' (' + label(order.state) + ')');
            }
            var rejected: Order = transition(order, 'cancel');
            console.log('cancel -> ' + rejected.state);
            console.log('same object: ' + (rejected === order));
        """),
        out("place -> placed (已下单)\npay -> paid (已支付)\nship -> shipped (已发货)\ncancel -> shipped\nsame object: false"),
        code_static("""
            // 仅演示，本运行时无法执行（never 收窄与 assertNever 只在 tsc 中生效）
            interface Circle {
              kind: 'circle';
              radius: number;
            }

            interface Square {
              kind: 'square';
              side: number;
            }

            type Shape = Circle | Square;

            function assertNever(value: never): never {
              throw new Error('unhandled: ' + JSON.stringify(value));
            }

            function area(shape: Shape): number {
              switch (shape.kind) {
                case 'circle':
                  return Math.PI * shape.radius * shape.radius;
                case 'square':
                  return shape.side * shape.side;
                default:
                  return assertNever(shape);
              }
            }
        """),
        out("（纯类型声明，仅演示，本运行时无法执行，没有输出）"),
        table(["建模方式", "合法状态数", "非法组合", "新增状态的成本"], [
            ["3 个布尔标志", "最多 8 种组合", "5 种以上", "组合数翻倍，所有判断都要复查"],
            ["字符串状态字段", "靠约定", "运行时才知道", "漏改不会报错"],
            ["可辨识联合 + 转换表", "显式枚举", "构造不出来", "补一个分支，编译器提示漏项"],
            ["联合 + assertNever", "显式枚举", "构造不出来", "漏分支直接编译失败"],
        ]),
        tip("状态转换永远返回新对象。原地修改旧状态会让历史记录失效，也无法做时间旅行调试；复制三个字段的成本远低于排查状态被谁改过。"),
        warn("本应用不做类型检查，可辨识联合的收窄只存在于 tsc。运行代码必须自己判断 kind，并在 default 分支抛错或返回兜底值。"),
        quiz("用三个布尔标志描述界面状态，理论上最多有多少种组合？", [
            "3 种",
            "6 种",
            "8 种",
            "9 种",
        ], 2, "每个布尔有两种取值，三个标志共 2 的 3 次方，也就是 8 种组合；其中只有少数是业务允许的。"),
        practice("分支描述", """
            type Shape = 'circle' | 'square';

            function sides(shape: Shape): number {
              if (shape === 'circle') {
                return 0;
              }
              return 4;
            }

            console.log(sides('circle'), sides('square'));
        """, "0 4", "字面量联合在运行时就是普通字符串，直接比较即可。"),
    ],
    exercise(
        "订单状态机与非法转换",
        "实现 nextState(order, event)：按规则 draft+place→placed、placed+pay→paid、placed+cancel→cancelled、paid+ship→shipped、paid+cancel→cancelled 返回新对象；非法转换返回原状态的新副本。再实现 canCancel(state)：placed 与 paid 返回 true，其余返回 false。",
        """
        type OrderState = 'draft' | 'placed' | 'paid' | 'shipped' | 'cancelled';

        interface Order {
          id: string;
          state: OrderState;
          total: number;
        }

        function nextState(order: Order, event: string): Order {
          return order;
        }

        function canCancel(state: OrderState): boolean {
          return false;
        }
        """,
        "用转换表逐条比较 from 与 event；命中就返回 { id, state: to, total }，没命中返回原 state 的新对象。canCancel 判断 state 是不是 placed 或 paid。",
        [
            "assert(nextState({id:'A',state:'draft',total:1}, 'place').state === 'placed', 'draft + place 应变为 placed')",
            "assert(nextState({id:'A',state:'placed',total:1}, 'pay').state === 'paid', 'placed + pay 应变为 paid')",
            "assert(nextState({id:'A',state:'shipped',total:1}, 'cancel').state === 'shipped', '非法转换应保持原状态')",
            "assert(nextState({id:'A',state:'paid',total:1}, 'cancel').state === 'cancelled', 'paid + cancel 应变为 cancelled')",
            "assert(canCancel('placed') === true && canCancel('shipped') === false, 'canCancel 只允许 placed 与 paid')",
        ],
        """
        type OrderState = 'draft' | 'placed' | 'paid' | 'shipped' | 'cancelled';

        interface Order {
          id: string;
          state: OrderState;
          total: number;
        }

        interface Transition {
          from: OrderState;
          event: string;
          to: OrderState;
        }

        var RULES: Transition[] = [
          { from: 'draft', event: 'place', to: 'placed' },
          { from: 'placed', event: 'pay', to: 'paid' },
          { from: 'placed', event: 'cancel', to: 'cancelled' },
          { from: 'paid', event: 'ship', to: 'shipped' },
          { from: 'paid', event: 'cancel', to: 'cancelled' }
        ];

        function nextState(order: Order, event: string): Order {
          for (var i = 0; i < RULES.length; i++) {
            if (RULES[i].from === order.state && RULES[i].event === event) {
              return { id: order.id, state: RULES[i].to, total: order.total };
            }
          }
          return { id: order.id, state: order.state, total: order.total };
        }

        function canCancel(state: OrderState): boolean {
          return state === 'placed' || state === 'paid';
        }
        """,
        170,
    ),
)

# ts36 迁移实战

TS36 = lesson(
    "ts36", BASE_ORDER + 5,
    "从 JS 迁移实战",
    "渐进式加类型，给 any 找替代",
    160,
    [
        h("迁移是一段可控的旅程"),
        p("""
            核心篇第 20 讲给过迁移的四步框架，这一讲补三件实战细节：改法、any 的替代路线、验收标准。迁移不是把 .js 改成 .ts 就结束了。第一步是让旧代码在 TypeScript 下原样跑通：tsconfig 开 allowJs 和 checkJs，先用 `// @ts-check` 在单个文件上试水，错误多到修不完就先关掉，只保留构建。第二步才是补类型，而且顺序很重要：先数据形状（interface / type），再函数边界（参数、返回值），最后内部变量。反过来先给局部变量加标注，往往会在改数据结构时全部推倒重来。

            JSDoc 是被低估的过渡工具。`/** @param {string} id @returns {Order | null} */` 写在 .js 文件里，TypeScript 能读懂并检查，编辑器也有补全。它适合暂时不想改后缀的模块，也适合给老代码加最小约束。等模块稳定后再整体改成 .ts，JSDoc 可以保留，也可以转成正式标注。迁移的每一步都应该可回滚、可单独发布。
        """),
        p("""
            any 的替代不是一句「改成 unknown」就完了。按场景分：外部输入用 unknown，进函数先收窄；键值不固定的字典用 Record 或索引签名；确实任意但只想透传的值用泛型 T；需要描述若干可选字段的配置用接口加可选属性；第三方库没类型就写最小 .d.ts；函数参数形态多就拆成多个函数或写重载。每种替代都比 any 多一点约束，但正是这些约束让下游安全。

            迁移中最容易滥用的写法是 `as any`。它同时绕过赋值检查和属性检查，还会把 any 传染给后续表达式。更温和的替代是 `as unknown as T`（明确表示两次转换）、`@ts-expect-error`（只压一行且要求下一行确实有错）、以及局部 `eslint-disable-next-line`。三者都要留注释说明原因和移除条件，否则三个月后没人敢删。
        """),
        p("""
            度量迁移进度不要只看还剩几个 any，要看类型覆盖了哪些边界。建议记录三张清单：已迁移模块、仍是 any 的入口、未校验的外部数据源。每完成一个模块，跑一次类型检查、跑一次回归测试，确认行为没变。类型迁移原则上不改逻辑；如果发现必须改逻辑才能标注，先停下来，把逻辑改动拆成单独提交，否则出问题时无法判断是哪一类改动引起的。

            本应用的转译器支持类型标注、interface、泛型参数表、as、非空断言和 enum，不支持 import/export、可选链、空值合并、默认参数和 async。把真实项目代码搬进来时要先做语法降级。运行示例会用同一段逻辑的 any 版本和 unknown 加守卫版本对比：输出完全一致，但后者把校验写在了明面上，这就是迁移真正带来的东西。

            迁移完成后的维护才是长期成本。建议在仓库里放一份「类型边界清单」：所有外部数据入口（HTTP、本地存储、消息队列、命令行参数）逐条列出，标明是否已校验、校验函数在哪个文件。新增入口时必须同步登记，代码评审时对照检查。这份清单比 any 计数更有价值，因为风险集中在边界，内部类型再漂亮，边界漏一个校验就等于零。清单不需要工具支持，一个 Markdown 表格就够，关键是有人维护。

            验收迁移成果要拿行为证据，而不是类型覆盖率截图。同一个输入，迁移前后的输出必须完全一致；错误输入的表现可以改进，但要在提交说明里写清改了什么、为什么改。测试用例是最好的迁移文档：它把「这个函数接受什么、返回什么、异常时怎样」固定下来，后来的人改类型标注时能立刻知道有没有改变语义。如果没有测试，先补测试再迁移，顺序不要颠倒。
        """),
        code("""
            interface Config {
              host: string;
              port: number;
              debug: boolean;
            }

            function parseConfigLoose(input: any): Config {
              return { host: input.host, port: input.port, debug: input.debug };
            }

            function parseConfigSafe(input: unknown): Config {
              var source: any = input;
              if (input === null || typeof input !== 'object') {
                return { host: 'localhost', port: 80, debug: false };
              }
              var host: string = typeof source.host === 'string' ? source.host : 'localhost';
              var port: number = typeof source.port === 'number' ? source.port : 80;
              var debug: boolean = source.debug === true;
              return { host: host, port: port, debug: debug };
            }

            var raw: any = { host: 'api.local', port: 8080 };
            console.log(JSON.stringify(parseConfigLoose(raw)));
            console.log(JSON.stringify(parseConfigSafe(raw)));
            console.log(JSON.stringify(parseConfigSafe({ port: 'x' })));
        """),
        out('{"host":"api.local","port":8080}\n{"host":"api.local","port":8080,"debug":false}\n{"host":"localhost","port":80,"debug":false}'),
        code_static("""
            // 仅演示，本运行时无法执行（JSDoc 由 tsc / 编辑器读取，Rhino 不认识）
            // @ts-check

            /**
             * @param {string} id
             * @returns {Order | null}
             */
            function findOrder(id) {
              var row = index[id];
              return row || null;
            }

            /** @type {{ host: string, port: number }} */
            var defaults = { host: 'localhost', port: 80 };
        """),
        out("（纯类型注释，仅演示，本运行时无法执行，没有输出）"),
        table(["场景", "any 写法", "更安全的替代", "本应用可运行"], [
            ["外部输入", "input: any", "input: unknown + typeof 守卫", "可运行"],
            ["字典", "data: any", "Record<string, number> / 索引签名", "类型层可写，运行时是普通对象"],
            ["透传", "value: any", "function id<T>(value: T): T", "可运行"],
            ["第三方库", "declare any", "最小 .d.ts 声明", "仅概念"],
            ["多形态参数", "data: any", "拆成多个函数或重载", "拆函数可运行"],
        ]),
        tip("把 any 当成借条：每写一个都要写清向谁借、什么时候还。没有归还计划的 any 会变成永久负债。"),
        warn("`as any` 不是迁移工具，而是迁移漏洞。它让错误从编译期漏到运行时；用它换来的「编译通过」没有任何价值。"),
        quiz("外部 JSON 解析结果在验证前最合适的类型是？", [
            "any",
            "unknown",
            "object",
            "never",
        ], 1, "unknown 可以接收任何值，但使用前必须收窄；any 会直接放弃检查，object 又假设了非空对象。"),
        steps([
            "把文件后缀改成 .ts，开启 allowJs / checkJs，先保证构建能过。",
            "按「数据形状 → 函数边界 → 内部变量」的顺序补标注。",
            "把入口处的 any 换成 unknown，并补 typeof / Array.isArray 守卫。",
            "为无类型的第三方库写最小 .d.ts，只声明实际用到的部分。",
            "统计剩余 any 与已校验边界，跑回归测试，确认行为未变。",
        ]),
    ],
    exercise(
        "迁移一个配置解析器",
        "实现 parseConfig(input: unknown)：输入必须是对象，host 为字符串时采用，否则用 'localhost'；port 为数字时采用，否则用 80；debug 只有严格等于 true 才为 true。实现 configText(config)：返回 host:port debug=布尔值。",
        """
        interface Config {
          host: string;
          port: number;
          debug: boolean;
        }

        function parseConfig(input: unknown): Config {
          return { host: 'localhost', port: 80, debug: false };
        }

        function configText(config: Config): string {
          return '';
        }
        """,
        "先把 input 收窄成 any 源对象，判 null 和非 object 后直接返回默认配置；三个字段分别用 typeof 和 === true 判断。configText 用字符串拼接。",
        [
            "assert(parseConfig({host:'a',port:1,debug:true}).host === 'a', '合法 host 应被采用')",
            "assert(parseConfig({}).port === 80, '缺失 port 应使用默认值')",
            "assert(parseConfig({port:'x'}).port === 80, '类型错误的 port 应使用默认值')",
            "assert(parseConfig(null).host === 'localhost', 'null 输入应返回默认配置')",
            "assert(configText({host:'h',port:1,debug:false}) === 'h:1 debug=false', 'configText 格式错误')",
        ],
        """
        interface Config {
          host: string;
          port: number;
          debug: boolean;
        }

        function parseConfig(input: unknown): Config {
          var source: any = input;
          if (input === null || typeof input !== 'object') {
            return { host: 'localhost', port: 80, debug: false };
          }
          var host: string = typeof source.host === 'string' ? source.host : 'localhost';
          var port: number = typeof source.port === 'number' ? source.port : 80;
          var debug: boolean = source.debug === true;
          return { host: host, port: port, debug: debug };
        }

        function configText(config: Config): string {
          return config.host + ':' + config.port + ' debug=' + config.debug;
        }
        """,
        170,
    ),
)

# ts37 工程化

TS37 = lesson(
    "ts37", BASE_ORDER + 6,
    "工程化：严格项与 CI 门禁",
    "tsconfig 逐条、lint 规则与流水线",
    165,
    [
        h("strict 不是一个开关"),
        p("""
            进阶篇第 28 讲讲的是编译流水线与 tsconfig 的整体结构，这一讲只盯两件事：严格项逐条在防什么，以及怎么把它们变成 CI 门禁。严格模式不是一个开关，而是一组独立选项。strict 只是把大部分严格项打包打开，逐条理解才能知道每条在防什么。noImplicitAny 禁止隐式 any，逼你给参数和返回值写类型；strictNullChecks 让 null 和 undefined 不再能赋给任意类型，是收益最大的一条；strictFunctionTypes 检查函数参数的逆变，防止把接收宽类型的函数当成接收窄类型用；strictBindCallApply 检查 call、apply、bind 的参数；strictPropertyInitialization 要求类字段在构造函数里初始化；noImplicitThis 禁止 this 隐式 any；alwaysStrict 输出 use strict。此外还有 useUnknownInCatchVariables，让 catch 到的值默认是 unknown。

            strict 之外还有几条高价值选项。noUncheckedIndexedAccess 让数组下标和字典取值带上 undefined，逼你判空，代价是代码变啰嗦；exactOptionalPropertyTypes 区分字段缺失和字段值为 undefined，适合对序列化格式敏感的 API；noImplicitOverride 要求重写方法加 override；noFallthroughCasesInSwitch 防止 switch 漏写 break；noUnusedLocals 和 noUnusedParameters 清理死代码；isolatedModules 保证每个文件能单独转译；verbatimModuleSyntax 要求类型导入显式写 type，避免运行时残留。
        """),
        p("""
            lint 和类型检查分工不同。tsc 管类型对不对，ESLint 管写法危不危险。typescript-eslint 的推荐组合里，最值得开的是 no-explicit-any、no-floating-promises、no-misused-promises、switch-exhaustiveness-check、consistent-type-imports。规则不要一次全开，否则历史代码会积压上千条警告，团队很快就会集体无视。新增规则时先修完再开，不要带着一堆例外上线。

            CI 门禁的顺序应该是：先类型检查，再 lint，再单元测试，最后构建。类型检查放第一位，因为它最快、最确定；构建放最后，因为它最慢。所有步骤都要在同一个提交上跑，失败即阻断合并。缓存 node_modules 和 tsc 的增量信息能显著缩短时间，但缓存键必须包含 lockfile 的哈希，否则会用到过期依赖。
        """),
        p("""
            门禁要防止两种退化：一是本地能过、CI 不过，通常是 tsconfig 或 ESLint 配置没提交全；二是 CI 能过但没人看，警告越积越多。解决办法是把配置当成代码管理：tsconfig 分基础配置和项目配置，用 extends 复用；lint 规则写进仓库，不在编辑器里做个人覆盖；CI 里用 --max-warnings=0，让警告等于失败。

            严格项的推进要有节奏。新项目一开始就全开；老项目按 strictNullChecks、noImplicitAny、其余的先后分批开，每批修完再开下一批。每开一条都记录日期、范围和剩余例外，形成可追溯的清单。不要为了通过门禁把类型改成 any，那等于用破坏目标的方式达成指标。门禁的目的是长期可维护，不是当天绿灯。

            还有一个常被忽略的配置项：skipLibCheck 与 lib。lib 决定你能用哪些标准库类型，写 ES5 目标却用了 Promise 类型，tsc 会报找不到名字；target 和 lib 要配套设置。skipLibCheck 只跳过依赖声明的互相检查，能显著缩短大项目的检查时间，但它不掩盖你代码里的错误。工程化的本质是把「检查什么、跳过什么、失败怎么办」写清楚并固化到流水线，而不是堆一堆开关。每一个开关都要能回答「它防住了哪类事故」。

            配置复用靠 extends：仓库根目录放 tsconfig.base.json 承载公共选项，各子项目写一份短配置继承它，只覆盖 target、paths、include 这些项目特有的部分。编辑器、CI、构建脚本读同一份配置，才不会出现「本地过、CI 挂」的经典问题。配置也要进代码评审：改一个严格项等于改全团队的开发体验，不能悄悄改。

            最后记住，工程化配置不是目的。门禁存在的意义是让团队敢改代码：改完立刻知道有没有破坏类型、有没有引入危险写法、测试有没有覆盖。如果一个门禁让人不敢提交、只能靠 disable 注释过关，那它设计错了。把规则数量控制在团队能维护的范围，把每条规则的理由写进注释或文档，定期清理失效规则，比一次性配齐所有选项更可持续。
        """),
        code("""
            interface GateConfig {
              strict: boolean;
              noImplicitAny: boolean;
              strictNullChecks: boolean;
              maxWarnings: number;
            }

            interface GateResult {
              passed: boolean;
              failed: string[];
            }

            function runGate(config: GateConfig, warnings: number): GateResult {
              var failed: string[] = [];
              if (!config.strict) {
                failed.push('strict');
              }
              if (!config.noImplicitAny) {
                failed.push('noImplicitAny');
              }
              if (!config.strictNullChecks) {
                failed.push('strictNullChecks');
              }
              if (warnings > config.maxWarnings) {
                failed.push('warnings');
              }
              return { passed: failed.length === 0, failed: failed };
            }

            var base: GateConfig = { strict: true, noImplicitAny: true, strictNullChecks: true, maxWarnings: 0 };
            var r1: GateResult = runGate(base, 0);
            console.log(r1.passed, r1.failed.length);
            var r2: GateResult = runGate(
              { strict: true, noImplicitAny: false, strictNullChecks: true, maxWarnings: 2 },
              3
            );
            console.log(r2.passed, r2.failed.join(','));
        """),
        out("true 0\nfalse noImplicitAny,warnings"),
        table(["选项", "拦住什么", "开启代价", "建议"], [
            ["strictNullChecks", "null / undefined 被当普通值用", "需要处理大量空分支", "老项目第一批开启"],
            ["noImplicitAny", "参数和返回值隐式 any", "要补标注", "第二批开启"],
            ["noUncheckedIndexedAccess", "下标取值未判空", "代码变啰嗦", "核心模块开启"],
            ["exactOptionalPropertyTypes", "缺失与 undefined 混用", "需要区分两种写法", "序列化边界开启"],
            ["noFallthroughCasesInSwitch", "switch 漏写 break", "几乎没有", "直接开启"],
            ["verbatimModuleSyntax", "类型导入残留到运行时", "要写 import type", "新项目直接开启"],
        ]),
        tip("先开 strictNullChecks 的收益通常最大：线上大量崩溃来自「以为一定有值」。它逼你把 null 当成正常分支处理，而不是异常。"),
        warn("不要把 skipLibCheck 当成万能药。它只跳过 .d.ts 的检查，不跳过你的代码；如果第三方类型互相冲突，应该升级依赖或写适配层，而不是永久关掉检查。"),
        quiz("下面哪条选项让数组下标访问的结果带上 undefined？", [
            "strictNullChecks",
            "noUncheckedIndexedAccess",
            "noImplicitAny",
            "exactOptionalPropertyTypes",
        ], 1, "noUncheckedIndexedAccess 会给索引访问的结果加上 undefined，要求调用方先判空；strictNullChecks 只处理显式的 null / undefined 类型。"),
        steps([
            "新项目直接开 strict；老项目先只开 strictNullChecks，修完空值分支。",
            "第二批开 noImplicitAny，给所有公共函数补参数与返回值标注。",
            "第三批开 noUncheckedIndexedAccess、exactOptionalPropertyTypes 等强约束项。",
            "接入 typescript-eslint，先开 no-explicit-any 与 switch-exhaustiveness-check。",
            "CI 固定顺序：tsc --noEmit → lint --max-warnings=0 → test → build。",
        ]),
    ],
    exercise(
        "严格项门禁报告",
        "实现 missingFlags(config)：按 strict、noImplicitAny、strictNullChecks 的固定顺序，返回值为 false 的选项名；实现 gatePassed(config, warnings)：没有缺失项且 warnings 不超过 maxWarnings 才返回 true。",
        """
        interface GateConfig {
          strict: boolean;
          noImplicitAny: boolean;
          strictNullChecks: boolean;
          maxWarnings: number;
        }

        function missingFlags(config: GateConfig): string[] {
          return [];
        }

        function gatePassed(config: GateConfig, warnings: number): boolean {
          return false;
        }
        """,
        "missingFlags 用三个 if 依次检查并 push 名称；gatePassed 直接复用 missingFlags 的长度，再比较 warnings 与 maxWarnings。",
        [
            "assert(missingFlags({strict:true,noImplicitAny:true,strictNullChecks:true,maxWarnings:0}).length === 0, '全开时不应有缺失项')",
            "assert(missingFlags({strict:false,noImplicitAny:true,strictNullChecks:true,maxWarnings:0}).join(',') === 'strict', '缺失项应按固定顺序返回')",
            "assert(missingFlags({strict:true,noImplicitAny:false,strictNullChecks:false,maxWarnings:0}).join(',') === 'noImplicitAny,strictNullChecks', '多个缺失项顺序错误')",
            "assert(gatePassed({strict:true,noImplicitAny:true,strictNullChecks:true,maxWarnings:0}, 0) === true, '零警告且全开应通过')",
            "assert(gatePassed({strict:true,noImplicitAny:true,strictNullChecks:true,maxWarnings:0}, 1) === false, '超出警告上限应失败')",
        ],
        """
        interface GateConfig {
          strict: boolean;
          noImplicitAny: boolean;
          strictNullChecks: boolean;
          maxWarnings: number;
        }

        function missingFlags(config: GateConfig): string[] {
          var out: string[] = [];
          if (!config.strict) {
            out.push('strict');
          }
          if (!config.noImplicitAny) {
            out.push('noImplicitAny');
          }
          if (!config.strictNullChecks) {
            out.push('strictNullChecks');
          }
          return out;
        }

        function gatePassed(config: GateConfig, warnings: number): boolean {
          return missingFlags(config).length === 0 && warnings <= config.maxWarnings;
        }
        """,
        175,
    ),
)

# ts38 综合项目

TS38 = lesson(
    "ts38", BASE_ORDER + 7,
    "综合项目：事件总线与订单状态机",
    "把八讲的知识装进一个可运行系统",
    180,
    [
        h("两个模块，一条主线"),
        p("""
            这一讲把前面七讲拼成一个完整系统：一个类型化的事件总线，加上一个订单状态机。事件总线负责谁关心什么：业务代码只声明事件名和处理函数，不直接调用其他模块；状态机负责订单能变成什么：所有合法转换集中在一张表里，非法转换有明确结果。两者通过事件连接，状态机产生事件，总线把事件分发给订阅者。这样订单模块不需要知道邮件、库存、日志的存在，新增订阅者也不用改订单代码。

            类型化事件总线的核心是一张事件名到载荷类型的映射表。真实 TypeScript 里写 `interface EventMap { 'order.placed': { id: string; total: number }; 'order.shipped': { id: string; carrier: string } }`，然后 `on<K extends keyof EventMap>(type: K, handler: (payload: EventMap[K]) => void)`。订阅 order.placed 时，回调参数自动推断成对应载荷，写错字段名会被 tsc 拒绝。本应用不支持 keyof 和索引访问类型，运行代码用字符串事件名加运行时校验来模拟同样的结构。
        """),
        p("""
            状态机用字面量联合描述状态，用一张转换表描述规则。订单对象只带 id、state、total 三个字段，状态之外的信息不塞进状态里。转换函数接收当前状态和事件，查表得到新状态；查不到就返回原状态的新副本。所有转换都返回新对象，不修改旧状态，这样历史记录天然完整，回放也容易。当每种状态需要携带不同字段时（草稿带创建人、已发货带运单号），再把状态升级成可辨识联合对象，思路和上一讲一致。

            事件总线要实现三件事：订阅、退订、发布。订阅时把处理函数按事件名存进表；发布时遍历对应列表，逐个调用；某个处理函数抛错不能影响其他订阅者，所以要 try/catch 并记录错误。退订需要拿到订阅时返回的句柄，或者在订阅时传入一个 key。本讲示例用数字句柄：订阅返回 id，退订时把 id 传回来，简单、可日志、也便于测试。
        """),
        p("""
            综合项目的验收标准不是能跑，而是行为可断言。至少覆盖：合法转换成功、非法转换保持原状态、事件按订阅顺序送达、单个处理函数抛错不影响其他订阅者、未知事件不崩溃。每一条都对应一个测试用例。写测试时先构造最小状态，再触发一个事件，最后断言输出状态或收到的载荷；不要一次测试整条链路，出问题时无法定位。

            最后给一个扩展方向：给总线加一次性订阅 once，给状态机加「转换被拒绝」事件，给订单加金额校验，支付金额必须等于订单金额。这三个需求都会逼你修改类型设计，是检验这套结构是否真正可扩展的好办法。做完之后回头看，你会发现真正撑住系统的不是某个语法，而是两条纪律：状态用联合表达，边界用函数收窄。

            复盘时可以把两个模块的接口抄在一张纸上，只留方法名和参数类型，然后问自己三个问题：调用方需要知道实现细节吗？新增一个订阅者要改几个文件？新增一个状态要改几个文件？如果第一个问题的答案是「需要」，说明抽象漏了；如果后两个问题的答案大于一，说明耦合还在。好的结构应该让常见扩展只改一处，让错误在编译或测试阶段暴露，而不是在生产环境兜底打印。这些问题没有标准答案，但每次复盘都会让下一版更接近目标。
        """),
        code("""
            type Handler = (payload: string) => void;

            interface Entry {
              id: number;
              fn: Handler;
            }

            interface Bus {
              on: (type: string, fn: Handler) => number;
              off: (type: string, id: number) => boolean;
              emit: (type: string, payload: string) => number;
            }

            function createBus(): Bus {
              var table: Entry[][] = [];
              var names: string[] = [];
              var nextId: number = 1;

              function indexOf(type: string): number {
                for (var i = 0; i < names.length; i++) {
                  if (names[i] === type) {
                    return i;
                  }
                }
                return -1;
              }

              function add(type: string, fn: Handler): number {
                var idx: number = indexOf(type);
                if (idx < 0) {
                  names.push(type);
                  table.push([]);
                  idx = names.length - 1;
                }
                var id: number = nextId;
                nextId = nextId + 1;
                table[idx].push({ id: (id), fn: (fn) });
                return id;
              }

              return {
                on: function (type: string, fn: Handler): number {
                  return add(type, fn, false);
                },
                off: function (type: string, id: number): boolean {
                  var idx: number = indexOf(type);
                  if (idx < 0) {
                    return false;
                  }
                  var list: Entry[] = table[idx];
                  for (var i = 0; i < list.length; i++) {
                    if (list[i].id === id) {
                      list.splice(i, 1);
                      return true;
                    }
                  }
                  return false;
                },
                emit: function (type: string, payload: string): number {
                  var idx: number = indexOf(type);
                  if (idx < 0) {
                    return 0;
                  }
                  var list: Entry[] = table[idx].slice();
                  var count: number = 0;
                  for (var i = 0; i < list.length; i++) {
                    try {
                      list[i].fn(payload);
                      count = count + 1;
                    } catch (e) {
                      console.log('handler failed: ' + e);
                    }
                  }
                  return count;
                }
              };
            }

            var bus: Bus = createBus();
            var seen: string[] = [];
            bus.on('order.placed', function (p: string) { seen.push('mail:' + p); });
            var badId: number = bus.on('order.placed', function (p: string) { throw new Error('boom'); });
            bus.on('order.placed', function (p: string) { seen.push('stock:' + p); });
            var delivered: number = bus.emit('order.placed', 'A1');
            console.log(delivered, seen.join('|'));
            console.log(bus.off('order.placed', badId));
            console.log(bus.emit('order.placed', 'A2'));
            console.log(seen.join('|'));
            console.log(bus.emit('unknown.event', 'x'));
        """),
        out("handler failed: Error: boom\n2 mail:A1|stock:A1\ntrue\n2\nmail:A1|stock:A1|mail:A2|stock:A2\n0"),
        code("""
            type OrderState = 'draft' | 'placed' | 'paid' | 'shipped' | 'cancelled';

            interface Order {
              id: string;
              state: OrderState;
              total: number;
            }

            interface Transition {
              from: OrderState;
              event: string;
              to: OrderState;
            }

            var RULES: Transition[] = [
              { from: 'draft', event: 'place', to: 'placed' },
              { from: 'placed', event: 'pay', to: 'paid' },
              { from: 'placed', event: 'cancel', to: 'cancelled' },
              { from: 'paid', event: 'ship', to: 'shipped' },
              { from: 'paid', event: 'cancel', to: 'cancelled' }
            ];

            function transition(order: Order, event: string): Order {
              for (var i = 0; i < RULES.length; i++) {
                if (RULES[i].from === order.state && RULES[i].event === event) {
                  return { id: order.id, state: RULES[i].to, total: order.total };
                }
              }
              return { id: order.id, state: order.state, total: order.total };
            }

            function label(state: OrderState): string {
              switch (state) {
                case 'draft':
                  return '草稿';
                case 'placed':
                  return '已下单';
                case 'paid':
                  return '已支付';
                case 'shipped':
                  return '已发货';
                case 'cancelled':
                  return '已取消';
              }
              throw new Error('unexpected state: ' + state);
            }

            var order: Order = { id: 'A1', state: 'draft', total: 99 };
            var events: string[] = ['place', 'pay', 'ship'];
            for (var i = 0; i < events.length; i++) {
              order = transition(order, events[i]);
              console.log(events[i] + ' -> ' + order.state + ' (' + label(order.state) + ')');
            }
            var rejected: Order = transition(order, 'cancel');
            console.log('cancel -> ' + rejected.state);
        """),
        out("place -> placed (已下单)\npay -> paid (已支付)\nship -> shipped (已发货)\ncancel -> shipped"),
        diagram("""
            订单事件流
            ─────────────────────────────────────────────
            transition(order, event)
                    │
                    ├─ 命中转换表 → 新 Order（新对象）
                    │        │
                    │        └─ bus.emit('order.changed', state)
                    │                 │
                    │                 ├─ mail 订阅者
                    │                 ├─ stock 订阅者
                    │                 └─ audit 订阅者
                    │
                    └─ 未命中 → 原状态新副本（拒绝变更）

            订阅者只依赖事件名与载荷，不依赖订单模块。
        """),
        table(["模块", "输入", "输出", "职责"], [
            ["createBus", "事件名 + 处理函数", "订阅 id", "维护订阅表、分发事件"],
            ["transition", "Order + 事件名", "新 Order", "查转换表、拒绝非法转换"],
            ["label", "OrderState", "中文标签", "展示层映射"],
            ["订阅者", "事件载荷", "副作用", "邮件、库存、审计各自独立"],
        ]),
        tip("发布事件时对订阅列表做 slice 复制再遍历。处理函数里如果调用 off 退订，直接遍历原数组会跳过下一个订阅者。"),
        warn("事件总线不要做成全局单例。全局总线会让模块间依赖隐形，测试时互相污染；按业务域创建实例，通过参数传递，依赖关系才看得见。"),
        quiz("事件总线中某个处理函数抛错时，正确的行为是？", [
            "中断整个发布",
            "记录错误并继续通知其他订阅者",
            "自动重试该处理函数",
            "删除该订阅者",
        ], 1, "订阅者之间应互相隔离：一个失败不能影响其他订阅者，错误要记录以便排查。"),
        steps([
            "先定义事件名与载荷的结构，保证同一事件只有一个载荷形状。",
            "实现订阅、退订、发布，发布时复制订阅列表并隔离异常。",
            "把订单状态写成可辨识联合，把合法转换集中到一张表。",
            "让状态机在转换成功后发布事件，订阅者只读载荷、不反向修改订单。",
            "补测试：合法转换、非法转换、异常隔离、退订、未知事件。",
        ]),
    ],
    exercise(
        "可退订事件总线与状态机",
        "实现 createBus()：on 订阅并返回数字 id，once 只触发一次，off 按 id 退订并返回是否成功，emit 返回成功处理的订阅者数量；实现 nextState(order, event)：按 draft+place→placed、placed+pay→paid、placed+cancel→cancelled、paid+ship→shipped、paid+cancel→cancelled 转换，非法转换保持原状态。",
        """
        type Handler = (payload: string) => void;

        interface Bus {
          on: (type: string, fn: Handler) => number;
          once: (type: string, fn: Handler) => number;
          off: (type: string, id: number) => boolean;
          emit: (type: string, payload: string) => number;
        }

        type OrderState = 'draft' | 'placed' | 'paid' | 'shipped' | 'cancelled';

        interface Order {
          id: string;
          state: OrderState;
          total: number;
        }

        function createBus(): Bus {
          return {
            on: function (type: string, fn: Handler): number { return 0; },
            once: function (type: string, fn: Handler): number { return 0; },
            off: function (type: string, id: number): boolean { return false; },
            emit: function (type: string, payload: string): number { return 0; }
          };
        }

        function nextState(order: Order, event: string): Order {
          return order;
        }
        """,
        "总线内部用表存 { id, fn, once }；emit 时把非 once 条目写回表，once 条目只调用不保留。nextState 用转换表逐条比较 from 与 event，未命中返回原 state 的新对象。",
        [
            "var b1 = createBus(); var got = []; b1.on('x', function (p: string) { got.push(p); }); assert(b1.emit('x', '1') === 1 && got[0] === '1', 'on + emit 应送达载荷')",
            "var b2 = createBus(); var n = 0; b2.once('x', function (p: string) { n = n + 1; }); b2.emit('x', 'a'); b2.emit('x', 'b'); assert(n === 1, 'once 只应触发一次')",
            "var b3 = createBus(); var id = b3.on('x', function (p: string) {}); assert(b3.off('x', id) === true && b3.emit('x', 'a') === 0, 'off 后不应再收到事件')",
            "assert(nextState({id:'A',state:'draft',total:1}, 'place').state === 'placed', 'draft + place 应变为 placed')",
            "assert(nextState({id:'A',state:'shipped',total:1}, 'cancel').state === 'shipped', '非法转换应保持原状态')",
        ],
        """
        type Handler = (payload: string) => void;

        interface Entry {
          id: number;
          fn: Handler;
          once: boolean;
        }

        interface Bus {
          on: (type: string, fn: Handler) => number;
          once: (type: string, fn: Handler) => number;
          off: (type: string, id: number) => boolean;
          emit: (type: string, payload: string) => number;
        }

        type OrderState = 'draft' | 'placed' | 'paid' | 'shipped' | 'cancelled';

        interface Order {
          id: string;
          state: OrderState;
          total: number;
        }

        interface Transition {
          from: OrderState;
          event: string;
          to: OrderState;
        }

        var RULES: Transition[] = [
          { from: 'draft', event: 'place', to: 'placed' },
          { from: 'placed', event: 'pay', to: 'paid' },
          { from: 'placed', event: 'cancel', to: 'cancelled' },
          { from: 'paid', event: 'ship', to: 'shipped' },
          { from: 'paid', event: 'cancel', to: 'cancelled' }
        ];

        function createBus(): Bus {
          var table: Entry[][] = [];
          var names: string[] = [];
          var nextId: number = 1;

          function indexOf(type: string): number {
            for (var i = 0; i < names.length; i++) {
              if (names[i] === type) {
                return i;
              }
            }
            return -1;
          }

          function add(type: string, fn: Handler, once: boolean): number {
            var idx: number = indexOf(type);
            if (idx < 0) {
              names.push(type);
              table.push([]);
              idx = names.length - 1;
            }
            var id: number = nextId;
            nextId = nextId + 1;
            table[idx].push({ id: (id), fn: (fn), once: (once) });
            return id;
          }

          return {
            on: function (type: string, fn: Handler): number {
              return add(type, fn, false);
            },
            once: function (type: string, fn: Handler): number {
              return add(type, fn, true);
            },
            off: function (type: string, id: number): boolean {
              var idx: number = indexOf(type);
              if (idx < 0) {
                return false;
              }
              var list: Entry[] = table[idx];
              for (var i = 0; i < list.length; i++) {
                if (list[i].id === id) {
                  list.splice(i, 1);
                  return true;
                }
              }
              return false;
            },
            emit: function (type: string, payload: string): number {
              var idx: number = indexOf(type);
              if (idx < 0) {
                return 0;
              }
              var list: Entry[] = table[idx];
              var keep: Entry[] = [];
              var count: number = 0;
              for (var i = 0; i < list.length; i++) {
                var entry: Entry = list[i];
                if (!entry.once) {
                  keep.push(entry);
                }
                try {
                  entry.fn(payload);
                  count = count + 1;
                } catch (e) {
                  console.log('handler failed: ' + e);
                }
              }
              table[idx] = keep;
              return count;
            }
          };
        }

        function nextState(order: Order, event: string): Order {
          for (var i = 0; i < RULES.length; i++) {
            if (RULES[i].from === order.state && RULES[i].event === event) {
              return { id: order.id, state: RULES[i].to, total: order.total };
            }
          }
          return { id: order.id, state: order.state, total: order.total };
        }
        """,
        190,
    ),
)

PACK = [TS31, TS32, TS33, TS34, TS35, TS36, TS37, TS38]


# 自检

def prose_cjk(item):
    """只统计 text 块的汉字数，与仓库验证器的口径一致。"""
    return sum(len(CJK.findall(b.get("text", ""))) for b in item["blocks"] if b["type"] == "text")


def _blank_declarations(src):
    """把 interface 整块与 type 别名整行替换成空行，用于代码扫描。

    这些声明在 TypeScriptTranspiler 里会被整体删除，其中的 `=>`、`keyof`
    等字符不会进入运行时，因此扫描禁用语法前必须先屏蔽。
    """
    out = []
    i = 0
    n = len(src)
    while i < n:
        m = re.search(r"\binterface\s+\w+(?:\s*<[^<>{}]*>)?(?:\s+extends\s+[^{]+)?\s*\{", src[i:])
        if not m:
            out.append(src[i:])
            break
        start = i + m.start()
        out.append(src[i:start])
        j = i + m.end() - 1
        depth = 0
        while j < n:
            if src[j] == "{":
                depth += 1
            elif src[j] == "}":
                depth -= 1
                if depth == 0:
                    break
            j += 1
        out.append("\n")
        i = j + 1

    lines = "".join(out).split("\n")
    kept = []
    k = 0
    while k < len(lines):
        trimmed = lines[k].lstrip()
        if re.match(r"type\s+\w+(\s*<[^>]*>)?\s*=", trimmed):
            depth = lines[k].count("{") - lines[k].count("}")
            j = k
            while depth > 0 and j + 1 < len(lines):
                j += 1
                depth += lines[j].count("{") - lines[j].count("}")
            kept.append("")
            k = j + 1
            continue
        kept.append(lines[k])
        k += 1
    return "\n".join(kept)


def self_check(pack):
    ids = [x["id"] for x in pack]
    expected_ids = [f"ts{i}" for i in range(31, 39)]
    if ids != expected_ids:
        raise SystemExit(f"id 序列错误：{ids}")

    for i, item in enumerate(pack):
        lid = item["id"]
        if item["order"] != BASE_ORDER + i:
            raise SystemExit(f"{lid} order 应为 {BASE_ORDER + i}")
        if item["chapter"] != CHAPTER:
            raise SystemExit(f"{lid} chapter 应为 {CHAPTER}")
        if item["chapterName"] != CHAPTER_NAME:
            raise SystemExit(f"{lid} chapterName 错误")
        if item["lang"] != "typescript":
            raise SystemExit(f"{lid} lang 错误")

        blocks = item["blocks"]
        if len(blocks) < 10:
            raise SystemExit(f"{lid} 内容块不足 10：{len(blocks)}")
        kinds = [b["type"] for b in blocks]
        if kinds.count("heading") < 1:
            raise SystemExit(f"{lid} 缺 heading")
        if kinds.count("text") != 3:
            raise SystemExit(f"{lid} 应有 3 个 text，实际 {kinds.count('text')}")
        prose = prose_cjk(item)
        if not 900 <= prose <= 1600:
            raise SystemExit(f"{lid} 正文汉字数 {prose} 不在 900-1600")
        if kinds.count("quiz") < 1:
            raise SystemExit(f"{lid} 缺 quiz")
        if kinds.count("table") + kinds.count("diagram") < 1:
            raise SystemExit(f"{lid} 缺 table/diagram")
        if kinds.count("tip") + kinds.count("warn") < 1:
            raise SystemExit(f"{lid} 缺 tip/warn")
        if kinds.count("practice") + kinds.count("steps") < 1:
            raise SystemExit(f"{lid} 缺 practice/steps")

        runnable = [b for b in blocks if b["type"] == "code" and b.get("runnable", True)]
        if not 1 <= len(runnable) <= 2:
            raise SystemExit(f"{lid} runnable 代码块应为 1-2 个，实际 {len(runnable)}")
        if kinds.count("output") < len(runnable):
            raise SystemExit(f"{lid} output 块少于 runnable 代码块")

        for b in blocks:
            if b["type"] != "code":
                continue
            if b.get("runnable", True):
                src = _blank_declarations(b["code"])
                for token in FORBIDDEN_RUNTIME:
                    if token in src:
                        raise SystemExit(f"{lid} runnable 代码含禁用语法 {token!r}")
            else:
                if "仅演示" not in b["code"] or "无法执行" not in b["code"]:
                    raise SystemExit(f"{lid} 静态代码块缺少「仅演示，本运行时无法执行」标注")

        ex = item["exercise"]
        if not 3 <= len(ex["tests"]) <= 5:
            raise SystemExit(f"{lid} tests 应为 3-5 条")
        if ex["starterCode"].strip() == ex["solution"].strip():
            raise SystemExit(f"{lid} starter 与 solution 相同")
        for t in ex["tests"]:
            if "assert(" not in t:
                raise SystemExit(f"{lid} 测试未使用 assert")

        for label, src in [("starter", ex["starterCode"]), ("solution", ex["solution"])] + [
            (f"test{i}", t) for i, t in enumerate(ex["tests"], 1)
        ]:
            cleaned = _blank_declarations(src)
            for token in FORBIDDEN_RUNTIME:
                if token in cleaned:
                    raise SystemExit(f"{lid} exercise {label} 含禁用语法 {token!r}")

        prints = [b for b in blocks if b["type"] == "quiz"]
        for q in prints:
            if not (0 <= q["answer"] < len(q["options"])):
                raise SystemExit(f"{lid} quiz answer 越界")


def main():
    self_check(PACK)
    OUT.write_text(
        json.dumps(PACK, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    total_cases = sum(len(x["exercise"]["tests"]) for x in PACK)
    print(f"{OUT.name}: {len(PACK)} lessons, {total_cases} cases")
    for item in PACK:
        kinds = [b["type"] for b in item["blocks"]]
        runnable = sum(1 for b in item["blocks"] if b["type"] == "code" and b.get("runnable", True))
        print(
            f"  {item['id']} {item['title']}  块={len(kinds)}  "
            f"正文={prose_cjk(item)}字  runnable={runnable}  用例={len(item['exercise']['tests'])}"
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())

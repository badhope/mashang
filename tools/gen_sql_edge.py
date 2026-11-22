#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 SQL「边界专题」课程包（4 讲，sql39–sql42）。

运行：python tools/gen_sql_edge.py
产物：content_packs/sql-edge.json

主题：NULL 分组、重复行去重、排序并列、事务与 SAVEPOINT。
所有输出块由 sql_runner 真实执行生成；每讲参考答案与起始代码都在生成期判一遍。
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "app" / "src" / "main" / "python"))
sys.path.insert(0, str(ROOT / "tools"))

import sql_runner  # noqa: E402
import gen_sql_expert as base  # noqa: E402

OUT = ROOT / "content_packs" / "sql-edge.json"
CHAPTER = 55
CHAPTER_NAME = "SQL · 边界专题"


def lesson(lid, order, title, subtitle, xp, blocks, ex):
    return {
        "id": lid,
        "order": order,
        "chapter": CHAPTER,
        "chapterName": CHAPTER_NAME,
        "lang": "sql",
        "title": title,
        "subtitle": subtitle,
        "xp": xp,
        "blocks": blocks,
        "exercise": ex,
    }


def exercise(title, brief, starter, hint, tests, solution, xp=130):
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


def pair(sql):
    return [base.code(sql), base.output(base.run_out(sql))]


def sql39():
    setup = """CREATE TABLE visits (user TEXT, city TEXT);
INSERT INTO visits VALUES
  ('V','Neon'), ('Judy',NULL), ('Rogue','Neon'), ('Echo',NULL), ('V',NULL);

SELECT city,
       COUNT(*) AS rows_all,
       COUNT(city) AS rows_nonnull,
       COUNT(DISTINCT user) AS users
FROM visits
GROUP BY city
ORDER BY users DESC, city;"""
    coalesce_query = """SELECT COALESCE(city, '未知') AS city,
       COUNT(*) AS visits,
       COUNT(DISTINCT user) AS users
FROM visits
GROUP BY COALESCE(city, '未知')
ORDER BY users DESC, city;"""
    coalesce = setup + "\n\n" + coalesce_query
    return lesson(
        "sql39", 540, "NULL 也要分组", "COUNT(*) / COUNT(col) 与 COALESCE", 130,
        [
            base.heading("NULL 不是「空字符串」，也不是 0"),
            base.paragraph(
                "SQL 里的 NULL 表示「未知」。它不等于任何值，连 NULL = NULL 都不成立。"
                "这会直接影响聚合：COUNT(*) 数行，COUNT(city) 只数 city 非 NULL 的行；"
                "GROUP BY 则会把 NULL 当成一个独立分组保留下来。三个函数放在一起，"
                "才能看出「这一组到底有多少行、多少行有值、多少个不同用户」。"
            ),
            base.paragraph(
                "很多报表事故来自把 COUNT(*) 当成「有值行数」。例如城市字段缺失时，"
                "COUNT(*) 仍然计入，COUNT(city) 却是 0。要让缺失值进入业务口径，"
                "用 COALESCE(city, '未知') 显式命名；不要用 ISNULL 之后的隐式转换去猜。"
            ),
            base.paragraph(
                "分组键必须与 SELECT 里的表达式一致，否则 SQLite 会按原始列分组、"
                "却在输出时显示另一个标签，看起来像「同一组出现两次」。"
                "写 GROUP BY COALESCE(...) 而不是 GROUP BY city，是为了让分组与展示同源。"
            ),
            base.paragraph(
                "还有一个容易忽略的点：NULL 在 ORDER BY 里默认排在最前（SQLite 把 NULL 视为最小）。"
                "如果报表要求「未知」排最后，不能只写 ORDER BY city，要显式写 "
                "ORDER BY city IS NULL, city，或者用 CASE 把 NULL 映射成固定排序键。"
                "排序规则和分组规则一样，都要在 SQL 里写清楚，不能靠默认行为碰运气。"
            ),
            base.paragraph(
                "做数据质量巡检时，可以先用 COUNT(*) 与 COUNT(列) 的差值算缺失率，"
                "再按业务键分组看缺失集中在哪些维度。缺失率本身不是问题，"
                "缺失集中在某个城市、某条渠道才是问题；只看总数会掩盖分布。"
            ),
            *pair(setup),
            *pair(coalesce),
            base.table(
                ["聚合写法", "统计对象", "NULL 行是否计入"],
                [
                    ["COUNT(*)", "行数", "计入"],
                    ["COUNT(city)", "city 有值的行", "不计入"],
                    ["COUNT(DISTINCT user)", "不同 user", "user 为 NULL 时不计入"],
                    ["COALESCE(city,'未知')", "把缺失值命名后分组", "计入「未知」组"],
                ],
            ),
            base.tip("先问「这一列缺失时业务上叫什么」，再决定 COALESCE 的默认值。默认值不是装饰，是口径。"),
            base.warn("不要用 `city = NULL` 判断空值，它永远不成立；用 `IS NULL` 或 COALESCE。"),
            base.quiz(
                "哪条查询能统计「每个城市有多少不同用户」，并把 NULL 城市命名为未知？",
                [
                    "SELECT city, COUNT(*) FROM visits GROUP BY city",
                    "SELECT COALESCE(city,'未知'), COUNT(DISTINCT user) FROM visits GROUP BY COALESCE(city,'未知')",
                    "SELECT city, COUNT(user) FROM visits WHERE city IS NULL",
                    "SELECT DISTINCT city, COUNT(*) FROM visits",
                ],
                1,
                "分组键与展示表达式都用 COALESCE，COUNT(DISTINCT user) 才统计不同用户。",
            ),
        ],
        exercise(
            "城市用户盘点",
            "按城市统计访问行数与不同用户数；NULL 城市显示为「未知」，按用户数降序、城市升序。",
            """CREATE TABLE visits (user TEXT, city TEXT);
INSERT INTO visits VALUES
  ('V','Neon'), ('Judy',NULL), ('Rogue','Neon'), ('Echo',NULL), ('V',NULL);

-- 在这里写分组查询
""",
            "SELECT 里用 COALESCE(city, '未知')，GROUP BY 用同一个表达式；行数用 COUNT(*)，用户数用 COUNT(DISTINCT user)。",
            [
                "assert columns == ['city','users','visits'], columns",
                "assert rows == [['未知',3,3],['Neon',2,2]], rows",
                "assert len(rows) == 2, '只应有两个城市分组（含未知）'",
                "assert all(isinstance(r[1], int) for r in rows), '用户数应为整数'",
            ],
            """CREATE TABLE visits (user TEXT, city TEXT);
INSERT INTO visits VALUES
  ('V','Neon'), ('Judy',NULL), ('Rogue','Neon'), ('Echo',NULL), ('V',NULL);

SELECT COALESCE(city, '未知') AS city,
       COUNT(DISTINCT user) AS users,
       COUNT(*) AS visits
FROM visits
GROUP BY COALESCE(city, '未知')
ORDER BY users DESC, city;""",
        ),
    )


def sql40():
    setup = """CREATE TABLE signups (email TEXT);
INSERT INTO signups VALUES
  ('v@neon.dev'), ('judy@neon.dev'), ('v@neon.dev'),
  ('rogue@afterlife.net'), ('echo@afterlife.net'), ('echo@afterlife.net');

SELECT email, COUNT(*) AS times
FROM signups
GROUP BY email
HAVING COUNT(*) > 1
ORDER BY times DESC, email;"""
    summary_query = """SELECT COUNT(*) AS rows_all,
       COUNT(DISTINCT email) AS unique_emails,
       COUNT(*) - COUNT(DISTINCT email) AS duplicates
FROM signups;"""
    summary = setup + "\n\n" + summary_query
    return lesson(
        "sql40", 541, "重复行不是「多了一行」", "DISTINCT、GROUP BY 与 HAVING", 130,
        [
            base.heading("先去重，再统计"),
            base.paragraph(
                "重复行有两种含义：完全相同的整行重复，以及业务键重复（邮箱相同但注册时间不同）。"
                "SELECT DISTINCT 处理前者，按业务键 GROUP BY 处理后者。混用会把「同一邮箱的两次注册」"
                "误当成两行不同数据，也会把真正的重复悄悄合并掉。"
            ),
            base.paragraph(
                "找重复用 GROUP BY 业务键 + HAVING COUNT(*) > 1；统计规模用 COUNT(*) 与 "
                "COUNT(DISTINCT key) 的差值。注意 HAVING 是在分组之后过滤，WHERE 是分组之前过滤，"
                "位置写反会得到完全不同的结果。"
            ),
            base.paragraph(
                "去重不是无条件正确：如果重复行代表两次真实事件，删掉就丢了事实。"
                "先确认业务键，再决定是保留明细、聚合计数，还是只留最新一条。"
            ),
            base.paragraph(
                "SELECT DISTINCT 作用于最终结果集，不是「先去重再计算」。"
                "如果你写 SELECT DISTINCT email, COUNT(*)，DISTINCT 会把每个 "
                "(email, 计数) 组合去重，根本得不到「每个邮箱一行」。"
                "聚合与去重必须分两层：内层 GROUP BY 算计数，外层再决定要不要 DISTINCT。"
            ),
            base.paragraph(
                "保留最新一条也有标准做法：用 ROW_NUMBER() OVER (PARTITION BY 业务键 "
                "ORDER BY 时间 DESC) 编号，再取编号为 1 的行。不要用 GROUP BY + MAX(时间) "
                "直接查，因为同一时间可能有多条，且其他列不一定是最大值那一行的值。"
            ),
            *pair(setup),
            *pair(summary),
            base.table(
                ["写法", "作用", "常见误用"],
                [
                    ["SELECT DISTINCT *", "整行去重", "把业务上不同的行合并"],
                    ["GROUP BY email", "按业务键分组", "忘了 HAVING，重复组也留下"],
                    ["COUNT(DISTINCT email)", "不同键数量", "键为 NULL 时不计入"],
                    ["HAVING COUNT(*) > 1", "只留重复组", "写成 WHERE，聚合尚未产生"],
                ],
            ),
            base.tip("先定义业务键：是邮箱、订单号，还是「邮箱 + 日期」？键定义不清，去重一定错。"),
            base.warn("DELETE 去重前先 SELECT 出将被删除的行；线上数据没有撤销键。"),
            base.quiz(
                "要找出重复邮箱及出现次数，正确的骨架是？",
                [
                    "SELECT email FROM signups WHERE COUNT(*) > 1",
                    "SELECT email, COUNT(*) FROM signups GROUP BY email HAVING COUNT(*) > 1",
                    "SELECT DISTINCT email, COUNT(*) FROM signups",
                    "SELECT COUNT(*) FROM signups GROUP BY email",
                ],
                1,
                "GROUP BY 业务键，再用 HAVING 对聚合结果过滤。",
            ),
        ],
        exercise(
            "重复邮箱清单",
            "列出出现两次以上的邮箱及次数，按次数降序、邮箱升序。",
            """CREATE TABLE signups (email TEXT);
INSERT INTO signups VALUES
  ('v@neon.dev'), ('judy@neon.dev'), ('v@neon.dev'),
  ('rogue@afterlife.net'), ('echo@afterlife.net'), ('echo@afterlife.net');

-- 在这里写重复检测
""",
            "GROUP BY email，HAVING COUNT(*) > 1，输出 email 与 COUNT(*) AS times。",
            [
                "assert columns == ['email','times'], columns",
                "assert rows == [['echo@afterlife.net',2],['v@neon.dev',2]], rows",
                "assert len(rows) == 2, '只有两个邮箱重复'",
                "assert all(r[1] >= 2 for r in rows), '每个结果至少出现两次'",
            ],
            """CREATE TABLE signups (email TEXT);
INSERT INTO signups VALUES
  ('v@neon.dev'), ('judy@neon.dev'), ('v@neon.dev'),
  ('rogue@afterlife.net'), ('echo@afterlife.net'), ('echo@afterlife.net');

SELECT email, COUNT(*) AS times
FROM signups
GROUP BY email
HAVING COUNT(*) > 1
ORDER BY times DESC, email;""",
        ),
    )


def sql41():
    setup = """CREATE TABLE scores (player TEXT, points INTEGER);
INSERT INTO scores VALUES
  ('V',90), ('Judy',90), ('Jackie',80), ('Rogue',70), ('Echo',70);

SELECT player, points,
       ROW_NUMBER() OVER (ORDER BY points DESC) AS row_no,
       RANK()       OVER (ORDER BY points DESC) AS rank_no,
       DENSE_RANK() OVER (ORDER BY points DESC) AS dense_no
FROM scores
ORDER BY points DESC, player;"""
    top_query = """SELECT player, points,
       DENSE_RANK() OVER (ORDER BY points DESC) AS rnk
FROM scores
WHERE points >= 80
ORDER BY rnk, player;"""
    top = setup + "\n\n" + top_query
    return lesson(
        "sql41", 542, "并列不是「谁先谁后」", "ROW_NUMBER / RANK / DENSE_RANK", 130,
        [
            base.heading("三种排名，三种业务含义"),
            base.paragraph(
                "ROW_NUMBER 给每行一个唯一序号，即使分数相同也强行分先后；RANK 遇到并列会跳号"
                "（1、1、3）；DENSE_RANK 不跳号（1、1、2）。奖牌榜用 DENSE_RANK，"
                "分页取前 N 条用 ROW_NUMBER，按名次发奖且允许并列用 RANK。"
            ),
            base.paragraph(
                "并列时的「谁先谁后」由 ORDER BY 的附加键决定。只写 points DESC，SQLite 不保证"
                "同分行的顺序；要稳定输出，必须再加 player 之类的唯一键。"
            ),
            base.paragraph(
                "排名函数不会自动去重，也不会改变行数。要「每个名次只留一行」，还得在外层按 "
                "名次分组或加过滤条件。别把排名和去重混为一谈。"
            ),
            base.paragraph(
                "分页场景要特别小心：ORDER BY points DESC LIMIT 20 OFFSET 20 在并列分数上"
                "可能重复或漏行，因为数据库不保证同分行的稳定顺序。正确做法是把唯一键"
                "（例如 player）加进 ORDER BY，或者先用 ROW_NUMBER 固定序号再按序号分页。"
                "线上分页出现「第二页又看到第一页的人」，通常是排序键不唯一。"
            ),
            base.paragraph(
                "奖牌榜还有一层业务规则：并列第一时，第三名是季军还是第四名？"
                "RANK 会跳成 1、1、3，DENSE_RANK 是 1、1、2。前者符合「占名次」的体育排名，"
                "后者符合「等级」口径。写之前先和业务确认，别让函数替业务做决定。"
            ),
            base.paragraph(
                "窗口函数也可以带 PARTITION BY：按班级分区排名，每个班级各自从 1 开始，"
                "互不影响。忘记 PARTITION BY 会把全校学生放在一条榜上，这不是数据错，"
                "而是统计口径错。排名前先问「在哪个范围内排」。"
            ),
            *pair(setup),
            *pair(top),
            base.table(
                ["函数", "90,90,80 的返回值", "是否跳号", "适用"],
                [
                    ["ROW_NUMBER", "1,2,3", "—", "分页、唯一序号"],
                    ["RANK", "1,1,3", "跳", "允许并列且占名次"],
                    ["DENSE_RANK", "1,1,2", "不跳", "奖牌榜、等级"],
                ],
            ),
            base.tip("排名结果必须附带稳定的并列处理键，否则同一份数据两次运行顺序可能不同。"),
            base.warn("不要用 ORDER BY points DESC LIMIT 3 代替「取前三名」，并列第三会被截掉。"),
            base.quiz(
                "分数 90、90、80 用 DENSE_RANK 降序，第三行的名次是？",
                ["3", "2", "1", "0"],
                1,
                "DENSE_RANK 不跳号，两个 90 都是 1，80 是 2。",
            ),
        ],
        exercise(
            "并列奖牌榜",
            "给 80 分及以上的选手按分数降序排名，使用 DENSE_RANK，并输出 player、points、rnk。",
            """CREATE TABLE scores (player TEXT, points INTEGER);
INSERT INTO scores VALUES
  ('V',90), ('Judy',90), ('Jackie',80), ('Rogue',70), ('Echo',70);

-- 在这里写窗口排名
""",
            "DENSE_RANK() OVER (ORDER BY points DESC) AS rnk；外层 ORDER BY rnk, player。",
            [
                "assert columns == ['player','points','rnk'], columns",
                "assert rows == [['Judy',90,1],['V',90,1],['Jackie',80,2]], rows",
                "assert len(rows) == 3, '80 分以下不应进入结果'",
                "assert all(r[2] in (1, 2) for r in rows), 'DENSE_RANK 不应跳号'",
            ],
            """CREATE TABLE scores (player TEXT, points INTEGER);
INSERT INTO scores VALUES
  ('V',90), ('Judy',90), ('Jackie',80), ('Rogue',70), ('Echo',70);

SELECT player, points,
       DENSE_RANK() OVER (ORDER BY points DESC) AS rnk
FROM scores
WHERE points >= 80
ORDER BY rnk, player;""",
        ),
    )


def sql42():
    setup = """CREATE TABLE accounts (name TEXT PRIMARY KEY, balance INTEGER);
INSERT INTO accounts VALUES ('V',100), ('Judy',50);

BEGIN;
UPDATE accounts SET balance = balance - 30 WHERE name = 'V';
UPDATE accounts SET balance = balance + 30 WHERE name = 'Judy';
SAVEPOINT before_big;
UPDATE accounts SET balance = balance - 200 WHERE name = 'V';
ROLLBACK TO before_big;
COMMIT;

SELECT name, balance FROM accounts ORDER BY name;"""
    rollback = """CREATE TABLE accounts (name TEXT PRIMARY KEY, balance INTEGER);
INSERT INTO accounts VALUES ('V',100), ('Judy',50);

BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE name = 'V';
ROLLBACK;

SELECT name, balance FROM accounts ORDER BY name;"""
    return lesson(
        "sql42", 543, "事务不是「一起执行」这么简单", "BEGIN / ROLLBACK / SAVEPOINT", 140,
        [
            base.heading("要么全成，要么全退"),
            base.paragraph(
                "转账不能只成功一半：扣了 V 的钱却没加到 Judy，账就坏了。事务把多条语句包成一个"
                "原子单位，COMMIT 之前任何一步失败都可以 ROLLBACK，把内存库恢复到 BEGIN 之前。"
                "SQLite 的内存库同样遵守这套规则，不是「测试库就可以乱来」。"
            ),
            base.paragraph(
                "SAVEPOINT 是事务里的检查点：大事务中某一步可能失败，但不该把前面已确认的步骤"
                "一起回滚。ROLLBACK TO 只退到检查点，后面的 COMMIT 仍会提交检查点之前的修改。"
                "这适合「主流程已成功，附加权益失败」的场景。"
            ),
            base.paragraph(
                "事务也有边界：BEGIN 后长时间不提交会占锁；嵌套 BEGIN 在 SQLite 里会报错，"
                "要用 SAVEPOINT 而不是再开一层。写操作前先想清楚失败时回到哪里，再决定检查点位置。"
            ),
            base.paragraph(
                "原子性只保证「全成或全退」，不保证「业务一定正确」。转账还要校验余额、"
                "账户状态、幂等键，否则同一请求重试两次会扣两次钱。事务解决的是执行中断，"
                "业务校验仍然要写在 SQL 条件或应用层判断里。"
            ),
            base.paragraph(
                "排查事务问题时，先看当前连接是否还有未提交的事务，再看每条语句影响的行数。"
                "UPDATE 影响 0 行往往不是语法错，而是 WHERE 没命中；如果把它当成成功继续提交，"
                "就会出现「事务提交了但钱没动」的假成功。"
            ),
            base.paragraph(
                "还要区分「事务回滚」和「语句报错」：单条语句报错时，SQLite 默认只撤销该语句，"
                "事务里的其他修改仍在，是否整体回滚由调用方决定。所以生产代码必须在异常分支"
                "显式 ROLLBACK，不能假设数据库会自动帮你收拾。"
            ),
            *pair(setup),
            *pair(rollback),
            base.table(
                ["语句", "作用", "失败时"],
                [
                    ["BEGIN", "开启事务", "后续语句可整体撤销"],
                    ["COMMIT", "提交", "修改落盘，不可回退"],
                    ["ROLLBACK", "回滚整个事务", "回到 BEGIN 前"],
                    ["SAVEPOINT / ROLLBACK TO", "局部检查点", "只回到检查点"],
                ],
            ),
            base.tip("把「扣款 + 入账」放在同一个事务里；只包住其中一条，等于没包。"),
            base.warn("ROLLBACK 之后别再执行依赖已撤销数据的语句；先确认状态，再继续。"),
            base.quiz(
                "事务中执行了 SAVEPOINT sp 后再 ROLLBACK TO sp，sp 之前的修改会怎样？",
                ["一起回滚", "保持，并在 COMMIT 时提交", "自动提交", "报错"],
                1,
                "ROLLBACK TO 只撤销检查点之后的修改，检查点之前的仍留在事务里。",
            ),
        ],
        exercise(
            "带检查点的转账",
            "从 V 转 30 给 Judy；随后尝试从 V 再扣 200（会透支），用 SAVEPOINT 撤销这一步，最后提交。输出最终余额。",
            """CREATE TABLE accounts (name TEXT PRIMARY KEY, balance INTEGER);
INSERT INTO accounts VALUES ('V',100), ('Judy',50);

-- 在这里写事务：转账 30，再用 SAVEPOINT 撤销透支的 200
""",
            "BEGIN；先转 30；SAVEPOINT before_big；再扣 200；ROLLBACK TO before_big；COMMIT；最后 SELECT。",
            [
                "assert columns == ['name','balance'], columns",
                "assert rows == [['Judy',80],['V',70]], rows",
                "assert len(rows) == 2, '应保留两个账户'",
                "assert all(r[1] >= 0 for r in rows), '不允许透支余额'",
            ],
            """CREATE TABLE accounts (name TEXT PRIMARY KEY, balance INTEGER);
INSERT INTO accounts VALUES ('V',100), ('Judy',50);

BEGIN;
UPDATE accounts SET balance = balance - 30 WHERE name = 'V';
UPDATE accounts SET balance = balance + 30 WHERE name = 'Judy';
SAVEPOINT before_big;
UPDATE accounts SET balance = balance - 200 WHERE name = 'V';
ROLLBACK TO before_big;
COMMIT;

SELECT name, balance FROM accounts ORDER BY name;""",
        ),
    )


def sql43():
    setup = """CREATE TABLE events (id INTEGER PRIMARY KEY, user_id INTEGER, created_at TEXT, kind TEXT);
INSERT INTO events VALUES
  (1,7,'2026-01-05','login'), (2,7,'2026-02-10','buy'), (3,7,'2026-03-01','login'),
  (4,8,'2026-02-11','buy'), (5,8,'2026-03-02','login'), (6,9,'2026-03-03','buy');

CREATE INDEX idx_user_time ON events(user_id, created_at);"""
    search = setup + """

-- 等值列在前，范围列在后：能走复合索引
EXPLAIN QUERY PLAN
SELECT * FROM events
WHERE user_id = 7 AND created_at >= '2026-02-01';"""
    scan = setup + """

-- 跳过最左列 user_id：索引失效，只能全表扫描
EXPLAIN QUERY PLAN
SELECT * FROM events
WHERE created_at >= '2026-02-01';"""
    covering = setup + """

-- 只查索引内的列：覆盖索引，不回表
EXPLAIN QUERY PLAN
SELECT user_id, created_at FROM events
WHERE user_id = 7;"""
    return lesson(
        "sql43", 544, "复合索引的左前缀", "最左前缀、范围列与覆盖索引", 140,
        [
            base.heading("联合索引不是「把两列都加上」"),
            base.paragraph(
                "复合索引按列顺序建一棵树：先按第一列排，第一列相同再按第二列排。"
                "查询条件必须从最左列开始连续命中，索引才有机会被用上。"
                "只查 created_at、不查 user_id，就像拿「姓 + 名」的电话簿只按名字找，"
                "整本都要翻。EXPLAIN QUERY PLAN 会诚实地告诉你：是 SEARCH 还是 SCAN。"
            ),
            base.paragraph(
                "等值条件要放在范围条件前面。`(user_id, created_at)` 能同时处理 "
                "`user_id = ? AND created_at >= ?`；如果写成 `(created_at, user_id)`，"
                "范围列在前，user_id 的等值条件就无法继续收窄索引区间。"
                "设计索引时先列出所有等值条件，再放范围条件，最后才是排序键。"
            ),
            base.paragraph(
                "覆盖索引是另一个收益点：如果 SELECT 的列全在索引里，SQLite 可以只读索引、"
                "不回表。`SELECT user_id, created_at` 会显示 COVERING INDEX；一旦多查一个 "
                "kind，就要回表取数据。覆盖索引能省 IO，但不要把大字段塞进索引，"
                "索引体积会反噬写入性能。"
            ),
            base.paragraph(
                "还要注意「索引存在」不等于「索引被用」。数据量太小、选择性和全表扫描差不多时，"
                "优化器可能仍然选 SCAN；统计信息过期也会让它误判。判断标准永远是实际执行计划，"
                "不是看仓库里有没有 CREATE INDEX 那一行。"
            ),
            base.paragraph(
                "最后看排序：联合索引本身有序，`WHERE user_id = 7 ORDER BY created_at` 可以直接"
                "顺着索引读，不再建临时 B 树。把排序键放在联合索引最后，常常能同时省掉过滤和排序"
                "两次开销；这也是列表页、时间线查询最常用的索引形状。"
            ),
            *pair(search),
            *pair(scan),
            *pair(covering),
            base.table(
                ["查询条件", "能否用 idx_user_time", "计划动作"],
                [
                    ["user_id = 7", "能，命中左前缀", "SEARCH"],
                    ["user_id = 7 AND created_at >= ...", "能，等值 + 范围", "SEARCH"],
                    ["created_at >= ...（无 user_id）", "不能，跳过最左列", "SCAN"],
                    ["user_id = 7 ORDER BY created_at", "能，顺带满足排序", "SEARCH"],
                ],
            ),
            base.tip("建联合索引前先写 WHERE：等值列排最前，范围列排后面，排序键跟最后。"),
            base.warn("不要给每个查询都新建索引；索引越多，INSERT / UPDATE 越慢，先看执行计划再决定。"),
            base.quiz(
                "索引 (user_id, created_at)，哪条查询最可能用不上它？",
                [
                    "WHERE user_id = 7",
                    "WHERE user_id = 7 AND created_at >= '2026-02-01'",
                    "WHERE created_at >= '2026-02-01'",
                    "WHERE user_id = 7 ORDER BY created_at",
                ],
                2,
                "没有 user_id 等值条件，跳过了联合索引的最左列，通常退化为全表扫描。",
            ),
        ],
        exercise(
            "给事件表建对索引",
            "为 events 建复合索引，查询 user_id = 7 且 created_at >= '2026-02-01' 的记录，按时间升序返回 user_id、created_at、kind。",
            """CREATE TABLE events (id INTEGER PRIMARY KEY, user_id INTEGER, created_at TEXT, kind TEXT);
INSERT INTO events VALUES
  (1,7,'2026-01-05','login'), (2,7,'2026-02-10','buy'), (3,7,'2026-03-01','login'),
  (4,8,'2026-02-11','buy'), (5,8,'2026-03-02','login'), (6,9,'2026-03-03','buy');

-- 在这里建索引并写查询
""",
            "CREATE INDEX idx_user_time ON events(user_id, created_at)；查询条件按 user_id、created_at 的顺序写。",
            [
                "assert columns == ['user_id','created_at','kind'], columns",
                "assert rows == [[7,'2026-02-10','buy'],[7,'2026-03-01','login']], rows",
                "assert len(rows) == 2, '只应返回 2 月及以后的 user 7 事件'",
                "assert rows[0][0] == 7 and rows[1][0] == 7, '结果必须都属于 user 7'",
            ],
            """CREATE TABLE events (id INTEGER PRIMARY KEY, user_id INTEGER, created_at TEXT, kind TEXT);
INSERT INTO events VALUES
  (1,7,'2026-01-05','login'), (2,7,'2026-02-10','buy'), (3,7,'2026-03-01','login'),
  (4,8,'2026-02-11','buy'), (5,8,'2026-03-02','login'), (6,9,'2026-03-03','buy');

CREATE INDEX idx_user_time ON events(user_id, created_at);

SELECT user_id, created_at, kind
FROM events
WHERE user_id = 7 AND created_at >= '2026-02-01'
ORDER BY created_at;""",
        ),
    )



LESSONS = [sql39(), sql40(), sql41(), sql42(), sql43()]


def check():
    problems = []
    ids = [l["id"] for l in LESSONS]
    if ids != [f"sql{n}" for n in range(39, 44)]:
        problems.append(f"讲 id 应为 sql39..sql43，实际 {ids}")
    if [l["order"] for l in LESSONS] != list(range(540, 545)):
        problems.append("order 应为 540..544")
    for l in LESSONS:
        lid = l["id"]
        blocks = l["blocks"]
        text_cjk = sum(len(b.get("text", "")) for b in blocks if b["type"] == "text")
        if not (600 <= text_cjk <= 1200):
            problems.append(f"{lid}: 正文长度 {text_cjk} 不在 600–1200")
        code_n = sum(1 for b in blocks if b["type"] == "code")
        out_n = sum(1 for b in blocks if b["type"] == "output")
        if code_n != out_n:
            problems.append(f"{lid}: code {code_n} / output {out_n} 不配对")
        if not any(b["type"] == "table" for b in blocks):
            problems.append(f"{lid}: 缺 table")
        if not any(b["type"] == "quiz" for b in blocks):
            problems.append(f"{lid}: 缺 quiz")
        for b in blocks:
            if b["type"] == "quiz" and not (0 <= b["answer"] < len(b["options"])):
                problems.append(f"{lid}: quiz 答案越界")
        ex = l["exercise"]
        if not (3 <= len(ex["tests"]) <= 5):
            problems.append(f"{lid}: tests 数量 {len(ex['tests'])} 不在 3–5")
        r = sql_runner.check_sql(ex["solution"], ex["tests"])
        if not r["passed"]:
            problems.append(f"{lid}: 参考答案未通过 {r['error']}")
        rs = sql_runner.check_sql(ex["starterCode"], ex["tests"])
        if rs["passed"]:
            problems.append(f"{lid}: starter 不应通过")
    return problems


def main():
    problems = check()
    if problems:
        print("自检失败：")
        for p in problems:
            print(" -", p)
        return 1
    OUT.write_text(json.dumps(LESSONS, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(f"sql-edge: {len(LESSONS)} 讲 -> {OUT.name}")
    print(f"判题用例={sum(len(l['exercise']['tests']) for l in LESSONS)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

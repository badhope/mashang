# 课程样例

这里放两节真实课程，方便先看内容风格，再决定要不要下载。完整的 245 讲在 App 里。

## Python 第 1 讲：第一次握手

目标：搞清 `print()`、字符串引号和布尔值，完成第一个判题。

课程里先给一段可运行的 `print` 示例，再讲逗号分隔、引号成对和常见报错。练习是“建立身份档案”：

```python
hacker_name = '你的代号'
online = ____

print(hacker_name, online)
```

要求把 `online` 补成 `True`，并保证 `hacker_name` 是非空字符串。判题不是比对代码，而是跑这三条断言：

```python
assert isinstance(hacker_name, str) and len(hacker_name) > 0
assert online is True
assert hacker_name in __stdout__, '请用 print 输出 hacker_name 的值'
```

这一节练的是三件事：变量赋值、布尔值、用 `print` 把结果送到标准输出。写错时提示会指到具体断言，不会只说“答案错误”。

## SQL 第 1 讲：第一次查询

目标：看懂 `SELECT`、`FROM` 和列名，能按指定列取数据。

题目给一张 `players` 表，四行玩家数据，要求取出 `name` 与 `level` 两列，并按 `id` 升序：

```sql
CREATE TABLE players (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  region TEXT,
  level INTEGER
);

INSERT INTO players VALUES
  (1, 'V', 'Neon', 66),
  (2, 'Judy', 'Neon', 42),
  (3, 'Jackie', 'Watson', 30),
  (4, 'Rogue', 'Watson', 12);

-- 在这里写查询
```

参考答案：

```sql
SELECT name, level FROM players ORDER BY id;
```

判题检查列名、行内容和行数：

```python
assert columns == ['name', 'level'], columns
assert rows == [['V', 66], ['Judy', 42], ['Jackie', 30], ['Rogue', 12]], rows
assert len(rows) == 4, rows
```

SQL 课的判题不是“看起来像”，而是真的在内存 SQLite 里执行，拿结果集和期望值比。写错列顺序、漏掉排序、忘记 `FROM`，都会给出具体差异。

## 这两节能看出什么

- 每节课都有可运行代码、解释、练习和断言，不是只放视频或选择题。
- 判题看运行结果，不要求按参考答案的写法逐字抄。
- 难度从第一讲开始就是动手写，不先把语法表背完。
- 课程正文带一点赛博朋克设定，但技术解释保持直白，不用故事掩盖知识点。

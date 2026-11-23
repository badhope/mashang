# CodeLearning 网页版

Android 之外还有一个网页端，打开浏览器就能看课、写 Python。课程 JSON 和判题引擎 `runner.py` 都跟手机端共用，所以判题结果、变量快照的口径是一致的。浏览器里的 Python 由 Pyodide 0.29.5 提供，版本是 CPython 3.13.2。

在线地址：<https://badhope.github.io/mashang/web/>

## 怎么打开

本地起一个静态服务就行：

```bash
cd web
python -m http.server 8765
```

然后访问 `http://127.0.0.1:8765/`。

也可以直接双击 `index.html`。课程和引擎已经内联在 `data.js`、`engine.js` 里，不需要额外请求。Pyodide 内核第一次加载要联网，会依次尝试 jsdelivr、fastly、unpkg，加载完由浏览器缓存。

## 能做什么

- 首页有 Python 主线进度、XP、段位和下一讲入口。
- 课程列表支持按语言筛选，共 245 讲，包含内置课程和扩展包。
- 课程详情能渲染表格、代码、对照、选择题、填空题、排序题、练习和判题等全部内容块。
- Python 代码块可以直接改、直接跑，运行后显示 stdout 和变量快照。
- 判题用 assert 用例，通过才记通关和 XP。
- 终端是有状态的 REPL，表达式会自动回显，方向键翻历史，Shift+Enter 换行，Ctrl+L 清屏。
- 挑战页共 22 道题，其中 7 道 Python 能在网页里判，其他语言标注为去 App 体验。
- 进度存在 `localStorage`，键名 `mashang.web.v1`，刷新和关页面都不会丢。路由用 hash，`#/lesson/l01`、`#/challenge/c01` 这类地址可以直接分享。
- 右上角可以切明暗主题，选择也会存进 `localStorage`。

## 执行和安全

- 用户代码放在 Web Worker 里跑。主线程 15 秒没响应就 `terminate()`，页面不会卡死。
- 课程和用户文本统一用 `textContent` 写入，不拼 HTML 字符串。
- 浏览器不支持 Worker 时会退回主线程执行。这时 C 阻塞调用没法中断，只能刷新页面。
- 引擎加载失败会给“重试”和“仅浏览课程”两个选择，课程内容仍然能看。
- Pyodide 里的 Python 可以通过 `js` 模块访问浏览器全局对象，这是运行用户自己代码的固有限制。更多说明见仓库根目录的 `SECURITY.md`。

## 和手机端怎么同步

- 课程来自 `app/src/main/assets/lessons_*.json`、`challenges.json`，再加上 `content_packs/` 里的扩展包，统一生成 `web/data.js`。
- 引擎来自 `app/src/main/python/runner.py`，生成 `web/engine.js`，内容是 `window.ENGINE_PY`。
- 网页端在 Pyodide 里执行这段引擎代码，然后调用 `run_code_json` 和 `check_exercise_json`。终端的持久命名空间和表达式回显放在 `gen_data.py` 里的 `REPL_HELPER_PY`。
- 改完课程或 `runner.py` 后运行 `python gen_data.py`。`tests/test_web_sync.py` 会逐字节检查生成结果有没有同步。

## 已知边界

- 第一次打开要下载约 12 MB 的 Pyodide WASM，之后走缓存。
- 网页版是 App 的子集，没有成就、连击、毕业证书和内容中心下载。
- 网页端只装了 Python 运行时。JavaScript、TypeScript、C、Lua、SQL 的课程能读，代码不能在这里跑，判题要去手机端。

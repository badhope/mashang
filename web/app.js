/* CodeLearning · 码上 — 网页版 (web/app.js)
 * 浏览器内 CPython 3.13（Pyodide 0.29.5），与 Android 端共用同一份 runner.py。
 * 设计要点：
 *  - 所有用户/课程文本走 textContent（el()），不做 HTML 字符串拼接，杜绝 DOM XSS。
 *  - 执行放在 Web Worker：死循环 / C 阻塞可 terminate，主线程永不冻结。
 *  - 进度、XP、完成状态存 localStorage；hash 路由可刷新/分享。
 *  - 与 App 相同的判题契约：assert + 变量快照 + __stdout__ 断言。
 */
(function () {
  "use strict";

  var STORAGE_KEY = "mashang.web.v1";
  var THEME_KEY = "mashang.web.theme";
  var APP_VERSION = "0.7.1";
  var PYODIDE_VERSION = "0.29.5";
  var PYODIDE_SOURCES = [
    { js: "https://cdn.jsdelivr.net/pyodide/v" + PYODIDE_VERSION + "/full/pyodide.js", index: "https://cdn.jsdelivr.net/pyodide/v" + PYODIDE_VERSION + "/full/" },
    { js: "https://fastly.jsdelivr.net/pyodide/v" + PYODIDE_VERSION + "/full/pyodide.js", index: "https://fastly.jsdelivr.net/pyodide/v" + PYODIDE_VERSION + "/full/" },
    { js: "https://unpkg.com/pyodide@" + PYODIDE_VERSION + "/pyodide.js", index: "https://unpkg.com/pyodide@" + PYODIDE_VERSION + "/" }
  ];
  var RUN_TIMEOUT_MS = 15000;

  var lessons = (window.LESSONS || []).slice().sort(function (a, b) {
    return (a.order || 0) - (b.order || 0) || String(a.id).localeCompare(String(b.id));
  });
  var challenges = window.CHALLENGES || [];
  var view = document.getElementById("view");
  var bootEl = document.getElementById("boot");
  var bootLine = document.getElementById("boot-line");
  var bootActions = document.getElementById("boot-actions");
  var engineMode = "loading"; // loading | worker | main | browse | failed
  var pyodide = null;
  var worker = null;
  var workerSeq = 0;
  var workerPending = {};
  var replHistory = [];
  var replHistoryIdx = -1;

  // ---------- 基础工具 ----------
  function el(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = String(text);
    return e;
  }
  function clear(node) {
    while (node.firstChild) node.removeChild(node.firstChild);
  }
  function icon(name) {
    var map = {
      play: "▶", check: "✓", lock: "▣", back: "‹", fire: "◉", bolt: "⚡",
      clock: "◷", code: "</>", book: "▤", terminal: "▸_", trophy: "♛"
    };
    return map[name] || "•";
  }

  // ---------- 进度存档 ----------
  function defaultProgress() {
    return { version: 1, xp: 0, completed: {}, solved: {}, lastRoute: "#/home" };
  }
  function loadProgress() {
    try {
      var raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return defaultProgress();
      var p = JSON.parse(raw);
      if (!p || typeof p !== "object") return defaultProgress();
      return {
        version: 1,
        xp: Number(p.xp) || 0,
        completed: p.completed && typeof p.completed === "object" ? p.completed : {},
        solved: p.solved && typeof p.solved === "object" ? p.solved : {},
        lastRoute: typeof p.lastRoute === "string" ? p.lastRoute : "#/home"
      };
    } catch (_) {
      return defaultProgress();
    }
  }
  var progress = loadProgress();
  function saveProgress() {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(progress)); } catch (_) { /* 隐私模式忽略 */ }
  }
  function awardOnce(key, xp) {
    if (progress.solved[key]) return false;
    progress.solved[key] = true;
    progress.xp += xp;
    saveProgress();
    return true;
  }
  function markLessonDone(id, xp) {
    if (progress.completed[id]) return false;
    progress.completed[id] = true;
    progress.xp += xp;
    saveProgress();
    return true;
  }
  function isLessonDone(id) { return !!progress.completed[id]; }
  function doneCount(list) {
    var n = 0;
    for (var i = 0; i < list.length; i++) if (isLessonDone(list[i].id)) n++;
    return n;
  }
  function coreLessons() { return lessons.filter(function (l) { return (l.lang || "python") === "python"; }); }

  // 网页端只有 Pyodide（CPython）。其他语言的课程可读、可看答案与讲解，
  // 但代码不能在此运行——明确标注，而不是假装能跑或干脆隐藏内容。
  var RUNNABLE_LANGS = { python: true };
  function langRunnable(lang) { return !!RUNNABLE_LANGS[lang || "python"]; }
  function langLabel(lang) {
    var m = { python: "Python", javascript: "JavaScript", typescript: "TypeScript", c: "C", lua: "Lua", sql: "SQL" };
    return m[lang || "python"] || (lang || "python");
  }

  // ---------- 引擎：Web Worker 优先 ----------
  function setStatus(text, ok) {
    var d = document.getElementById("py-status");
    var t = document.getElementById("py-text");
    if (d) d.className = "dot " + (ok ? "on" : "off");
    if (t) t.textContent = text;
  }

  function workerSource() {
    return [
      "var pyodide=null;",
      "self.onmessage=function(ev){",
      "  var msg=ev.data;",
      "  if(msg.type==='init'){",
      "    var sources=msg.sources; var i=0;",
      "    function tryNext(){",
      "      if(i>=sources.length){ self.postMessage({type:'boot-error',message:'全部 CDN 源均不可用'}); return; }",
      "      var src=sources[i++];",
      "      try{",
      "        if(typeof loadPyodide!=='function'){ importScripts(src.js); }",
      "        loadPyodide({indexURL:src.index}).then(function(py){",
      "          pyodide=py;",
      "          py.runPython(msg.engine);",
      "          if(msg.repl){ py.runPython(msg.repl); }",
      "          self.postMessage({type:'ready',version:py.runPython('import sys; sys.version').split(' ')[0]});",
      "        }).catch(function(err){ tryNext(); });",
      "      }catch(err){ tryNext(); }",
      "    }",
      "    tryNext();",
      "    return;",
      "  }",
      "  if(!pyodide){ self.postMessage({type:'error',id:msg.id,message:'引擎未就绪'}); return; }",
      "  try{",
      "    var out;",
      "    if(msg.type==='run'){ out=pyodide.globals.get('run_code_json')(JSON.stringify(msg.payload)); }",
      "    else if(msg.type==='check'){ out=pyodide.globals.get('check_exercise_json')(JSON.stringify(msg.payload)); }",
      "    else if(msg.type==='repl'){ out=pyodide.globals.get('repl_exec_json')(JSON.stringify(msg.payload)); }",
      "    else if(msg.type==='repl-reset'){ pyodide.globals.get('repl_reset_json')('{}'); out='{}'; }",
      "    else { out='{}'; }",
      "    self.postMessage({type:'result',id:msg.id,data:out});",
      "  }catch(err){ self.postMessage({type:'error',id:msg.id,message:String(err && err.message || err)}); }",
      "};"
    ].join("\n");
  }

  function initEngine() {
    return new Promise(function (resolve, reject) {
      var supported = typeof Worker !== "undefined";
      if (supported) {
        try {
          var blob = new Blob([workerSource()], { type: "application/javascript" });
          worker = new Worker(URL.createObjectURL(blob));
          var settled = false;
          var bootTimer = setTimeout(function () {
            if (settled) return;
            settled = true;
            try { worker.terminate(); } catch (_) {}
            worker = null;
            reject(new Error("引擎启动超时（45 秒）"));
          }, 45000);
          worker.onmessage = function (ev) {
            var msg = ev.data || {};
            if (msg.type === "ready") {
              if (settled) return;
              settled = true;
              clearTimeout(bootTimer);
              engineMode = "worker";
              setStatus("引擎在线 · CPython " + msg.version, true);
              resolve(msg.version);
              return;
            }
            if (msg.type === "boot-error" || msg.type === "error") {
              if (msg.id && workerPending[msg.id]) {
                workerPending[msg.id].reject(new Error(msg.message || "执行失败"));
                delete workerPending[msg.id];
                return;
              }
              if (settled) return;
              settled = true;
              clearTimeout(bootTimer);
              reject(new Error(msg.message || "引擎启动失败"));
              return;
            }
            if (msg.type === "result" && workerPending[msg.id]) {
              var p = workerPending[msg.id];
              delete workerPending[msg.id];
              clearTimeout(p.timer);
              try { p.resolve(JSON.parse(msg.data)); } catch (err) { p.reject(err); }
            }
          };
          worker.onerror = function (ev) {
            if (!settled) {
              settled = true;
              clearTimeout(bootTimer);
              reject(new Error("Worker 错误：" + (ev.message || "未知")));
            }
          };
          worker.postMessage({
            type: "init",
            sources: PYODIDE_SOURCES,
            engine: window.ENGINE_PY || "",
            repl: window.REPL_HELPER_PY || ""
          });
          return;
        } catch (_) {
          worker = null;
        }
      }
      loadMainThread().then(resolve, reject);
    });
  }

  function loadScript(url) {
    return new Promise(function (resolve, reject) {
      var s = document.createElement("script");
      s.src = url;
      s.onload = function () { resolve(); };
      s.onerror = function () { s.remove(); reject(new Error("脚本加载失败：" + url)); };
      document.head.appendChild(s);
    });
  }

  async function loadMainThread() {
    var lastErr = null;
    for (var i = 0; i < PYODIDE_SOURCES.length; i++) {
      var src = PYODIDE_SOURCES[i];
      try {
        if (typeof loadPyodide !== "function") {
          bootLine.textContent = "> 加载 Pyodide 运行器（源 " + (i + 1) + "/" + PYODIDE_SOURCES.length + "）…";
          await loadScript(src.js);
        }
        if (typeof loadPyodide !== "function") throw new Error("loadPyodide 未就绪");
        bootLine.textContent = "> 下载 CPython 内核（约 12 MB，源 " + (i + 1) + "/" + PYODIDE_SOURCES.length + "）…";
        pyodide = await loadPyodide({ indexURL: src.index });
        bootLine.textContent = "> 注入执行引擎 (runner.py)…";
        pyodide.runPython(window.ENGINE_PY || "");
        if (window.REPL_HELPER_PY) pyodide.runPython(window.REPL_HELPER_PY);
        engineMode = "main";
        var ver = pyodide.runPython("import sys; sys.version.split(' ')[0]");
        setStatus("引擎在线 · CPython " + ver, true);
        return ver;
      } catch (err) {
        lastErr = err;
        console.warn("Pyodide 源失败：", src.index, err);
      }
    }
    throw lastErr || new Error("全部 CDN 源均不可用");
  }

  function callEngine(type, payload) {
    if (engineMode === "browse") {
      return Promise.reject(new Error("当前为「仅浏览」模式：引擎未加载，运行与判题不可用。"));
    }
    if (engineMode === "worker" && worker) {
      return new Promise(function (resolve, reject) {
        var id = ++workerSeq;
        var timer = setTimeout(function () {
          delete workerPending[id];
          try { worker.terminate(); } catch (_) {}
          worker = null;
          engineMode = "failed";
          setStatus("引擎已中断", false);
          reject(new Error("执行超时（" + (RUN_TIMEOUT_MS / 1000) + " 秒），已终止运行时。请刷新页面重试。"));
        }, RUN_TIMEOUT_MS);
        workerPending[id] = { resolve: resolve, reject: reject, timer: timer };
        worker.postMessage({ type: type, id: id, payload: payload });
      });
    }
    if (engineMode === "main" && pyodide) {
      try {
        if (type === "repl-reset") {
          pyodide.runPython("repl_reset_json('{}')");
          return Promise.resolve({});
        }
        var fn = type === "run" ? "run_code_json" : type === "check" ? "check_exercise_json" : "repl_exec_json";
        pyodide.globals.set("__payload", JSON.stringify(payload));
        var out = pyodide.runPython(fn + "(__payload)");
        return Promise.resolve(JSON.parse(out));
      } catch (err) {
        return Promise.reject(err);
      }
    }
    return Promise.reject(new Error("引擎尚未就绪"));
  }

  function runCode(code, stdin) {
    return callEngine("run", { code: code, stdin: stdin || [], timeout: 8 });
  }
  function checkExercise(code, tests, stdin) {
    return callEngine("check", { code: code, tests: tests || [], stdin: stdin || [] });
  }
  function replExec(code) {
    return callEngine("repl", { code: code, stdin: [], timeout: 8 });
  }
  function replReset() {
    return callEngine("repl-reset", {});
  }

  // ---------- 结果渲染（全部 textContent） ----------
  function snapshotNode(vars) {
    var s = el("div", "snap");
    s.appendChild(el("div", "snap-title", "◢ 变量快照"));
    var g = el("div", "snap-grid");
    if (!vars || !vars.length) g.appendChild(el("span", "dim", "（无变量）"));
    (vars || []).forEach(function (v) {
      var c = el("div", "chip");
      c.appendChild(el("span", "cname", v.name));
      c.appendChild(document.createTextNode(" "));
      c.appendChild(el("span", "ctype", ":" + v.type));
      c.appendChild(document.createTextNode(" = "));
      c.appendChild(el("span", "cval", v.value));
      g.appendChild(c);
    });
    s.appendChild(g);
    return s;
  }

  function runResultNode(r) {
    var wrap = el("div", "out-inner");
    if (r.error) {
      wrap.appendChild(el("div", "term-out err", (r.error.type || "Error") + ": " + (r.error.message || "")));
    } else {
      wrap.appendChild(el("div", "term-out" + (r.stdout ? "" : " empty"), r.stdout || ""));
    }
    if (r.stderr) wrap.appendChild(el("div", "term-out err", r.stderr));
    wrap.appendChild(snapshotNode(r.variables || []));
    wrap.appendChild(el("div", "run-tag", "耗时 " + (r.duration_ms || 0) + " ms"));
    return wrap;
  }

  function checkResultNode(r) {
    var wrap = el("div", "out-inner");
    if (!r.ok) {
      var msg = r.error ? (r.error.type || "Error") + ": " + (r.error.message || "") : "运行出错";
      wrap.appendChild(el("div", "term-out err", msg));
      if (r.stdout) wrap.appendChild(el("div", "term-out", r.stdout));
      wrap.appendChild(snapshotNode(r.variables || []));
      return wrap;
    }
    wrap.appendChild(el("div", "verdict " + (r.passed ? "pass" : "fail"), r.passed ? "✓ 全部用例通过！" : "✗ 判题未通过"));
    if (r.test_output) wrap.appendChild(el("div", "term-out", r.test_output));
    if (!r.passed && r.error && r.error.message) wrap.appendChild(el("div", "quiz-explain", r.error.message));
    wrap.appendChild(snapshotNode(r.variables || []));
    return wrap;
  }

  // ---------- 可运行代码卡 ----------
  function codeCard(code, expected, stdin, lang) {
    lang = lang || "python";
    var card = el("div", "code-card");
    if (!langRunnable(lang)) {
      // 非 Python 语言：只读代码 + 明确说明在 App 内可运行
      card.appendChild(el("pre", "term", code));
      card.appendChild(el("div", "run-tag",
        langLabel(lang) + " · 网页端仅有 Python 引擎，此代码请在「CodeLearning」App 内运行"));
      return card;
    }
    var ta = el("textarea", "code-area");
    ta.value = code;
    ta.spellcheck = false;
    ta.setAttribute("aria-label", "可编辑代码");
    card.appendChild(ta);
    var bar = el("div", "code-bar");
    var run = el("button", "btn-run", "▶ 运行");
    run.type = "button";
    var tag = el("span", "run-tag", "");
    bar.appendChild(run);
    bar.appendChild(tag);
    var out = el("div", "out");
    var live = el("div", "live-region");
    live.setAttribute("aria-live", "polite");
    card.appendChild(bar);
    card.appendChild(out);
    card.appendChild(live);
    run.onclick = function () {
      if (engineMode === "browse") { clear(out); out.textContent = "仅浏览模式：请刷新页面并加载引擎后再运行。"; return; }
      run.disabled = true;
      run.textContent = "运行中…";
      clear(out);
      out.appendChild(el("div", "run-tag", "执行中…"));
      runCode(ta.value, stdin).then(function (r) {
        clear(out);
        out.appendChild(runResultNode(r));
        if (expected != null) {
          var ok = (r.stdout || "").trim() === String(expected).trim();
          out.appendChild(el("div", "verdict " + (ok ? "pass" : "fail"), ok ? "✓ 输出与预期一致" : "✗ 输出与预期不一致（对照上方预览）"));
        }
        live.textContent = r.error ? "运行失败" : "运行完成";
      }).catch(function (e) {
        clear(out);
        out.appendChild(el("div", "term-out err", e.message));
        live.textContent = "运行失败";
      }).finally(function () {
        run.disabled = false;
        run.textContent = "▶ 运行";
      });
    };
    return card;
  }

  // ---------- 内容块 ----------
  function renderTable(b) {
    var wrap = el("div", "table-wrap");
    var t = el("table", "neon");
    var thead = el("thead");
    var hr = el("tr");
    (b.headers || []).forEach(function (h) { hr.appendChild(el("th", null, h)); });
    thead.appendChild(hr);
    t.appendChild(thead);
    var tb = el("tbody");
    (b.rows || []).forEach(function (row) {
      var tr = el("tr");
      row.forEach(function (c) { tr.appendChild(el("td", null, c)); });
      tb.appendChild(tr);
    });
    t.appendChild(tb);
    wrap.appendChild(t);
    return wrap;
  }

  function renderQuiz(b) {
    var c = el("div", "card");
    c.appendChild(el("div", "quiz-q", b.question));
    var opts = el("div", "quiz-opts");
    var fb = el("div", "quiz-explain");
    fb.setAttribute("aria-live", "polite");
    (b.options || []).forEach(function (o, i) {
      var lab = el("button", "quiz-opt", o);
      lab.type = "button";
      lab.onclick = function () {
        Array.prototype.forEach.call(opts.children, function (x) { x.classList.remove("correct", "wrong"); });
        lab.classList.add(i === b.answer ? "correct" : "wrong");
        if (i !== b.answer && opts.children[b.answer]) opts.children[b.answer].classList.add("correct");
        fb.textContent = (i === b.answer ? "✓ 正确。" : "✗ 不对。") + (b.explain || "");
      };
      opts.appendChild(lab);
    });
    c.appendChild(opts);
    c.appendChild(fb);
    return c;
  }

  function renderOrder(b) {
    var c = el("div", "card");
    c.appendChild(el("div", "quiz-q", b.title || "把代码排成正确顺序"));
    var list = el("ul", "order-list");
    var lines = (b.lines || []).slice();
    function paint() {
      clear(list);
      lines.forEach(function (ln, i) {
        var li = el("li", "order-item");
        li.appendChild(el("span", "idx", String(i + 1)));
        li.appendChild(el("span", "oline", ln));
        var up = el("button", null, "↑");
        up.type = "button";
        up.setAttribute("aria-label", "上移");
        up.onclick = function () {
          if (i > 0) { var t = lines[i - 1]; lines[i - 1] = lines[i]; lines[i] = t; paint(); }
        };
        var dn = el("button", null, "↓");
        dn.type = "button";
        dn.setAttribute("aria-label", "下移");
        dn.onclick = function () {
          if (i < lines.length - 1) { var t = lines[i + 1]; lines[i + 1] = lines[i]; lines[i] = t; paint(); }
        };
        li.appendChild(up);
        li.appendChild(dn);
        list.appendChild(li);
      });
    }
    paint();
    c.appendChild(list);
    var bar = el("div", "code-bar");
    var run = el("button", "btn-run", "▶ 运行拼接结果");
    run.type = "button";
    var out = el("div", "out");
    run.onclick = function () {
      run.disabled = true;
      run.textContent = "运行中…";
      clear(out);
      runCode(lines.join("\n")).then(function (r) {
        out.appendChild(runResultNode(r));
      }).catch(function (e) {
        out.appendChild(el("div", "term-out err", e.message));
      }).finally(function () {
        run.disabled = false;
        run.textContent = "▶ 运行拼接结果";
      });
    };
    bar.appendChild(run);
    c.appendChild(bar);
    c.appendChild(out);
    return c;
  }

  function renderPractice(b) {
    var c = el("div", "card");
    c.appendChild(el("div", "quiz-q", b.title || "随堂练习"));
    c.appendChild(el("div", "hint", "预期输出："));
    c.appendChild(el("pre", "term preview", b.output || ""));
    if (b.hint) c.appendChild(el("div", "hint", "💡 " + b.hint));
    c.appendChild(codeCard(b.code, b.output, b.stdin || []));
    return c;
  }

  // 填空题：与 App 的 FillCard 语义一致 —— 输入缺失片段，答错两次给答案与解析
  function renderFill(b) {
    var c = el("div", "card fill-card");
    c.appendChild(el("div", "quiz-q", b.goal || "补全代码"));
    c.appendChild(el("pre", "term fill-code", b.code || ""));
    var row = el("div", "fill-row");
    var input = el("input", "fill-input");
    input.type = "text";
    input.spellcheck = false;
    input.setAttribute("aria-label", "填空答案");
    input.placeholder = "填入 ____ 处的代码";
    var btn = el("button", "btn-run", "提交");
    btn.type = "button";
    row.appendChild(input);
    row.appendChild(btn);
    c.appendChild(row);
    var fb = el("div", "quiz-explain");
    fb.setAttribute("aria-live", "polite");
    c.appendChild(fb);
    var tries = 0;
    function submit() {
      var val = (input.value || "").trim();
      if (!val) { fb.textContent = "先输入答案再提交。"; return; }
      if (val === String(b.answer || "").trim()) {
        fb.textContent = "✓ 正确。" + (b.explain || "");
        input.classList.add("ok");
        input.disabled = true;
        btn.disabled = true;
      } else {
        tries++;
        input.classList.add("bad");
        fb.textContent = tries >= 2
          ? "✗ 还不对。正确答案是「" + b.answer + "」。" + (b.explain || "")
          : "✗ 不对，再试一次。（答错两次会给出答案与解析）";
      }
    }
    btn.onclick = submit;
    input.addEventListener("keydown", function (e) { if (e.key === "Enter") submit(); });
    return c;
  }

  function renderExercise(b, lesson) {
    var c = el("div", "card exercise-card");
    c.appendChild(el("div", "blk-heading", "◢ 实战判题 · " + (b.title || "")));
    if (b.brief) c.appendChild(el("div", "brief", b.brief));

    // 非 Python 语言：网页端没有对应运行时，判题会调用错误的引擎。
    // 改为只读展示参考答案 + 明确指引，绝不给出假判题结果。
    var exLang = (lesson && lesson.lang) || "python";
    if (!langRunnable(exLang)) {
      var pre = el("pre", "term");
      pre.textContent = b.starterCode || "";
      c.appendChild(pre);
      if (b.hint) c.appendChild(el("div", "hint", "💡 " + b.hint));
      c.appendChild(el("div", "run-tag",
        langLabel(exLang) + " · 网页端仅有 Python 引擎，判题请在「CodeLearning」App 内进行"));
      return c;
    }
    var ta = el("textarea", "code-area");
    ta.value = b.starterCode || "";
    ta.spellcheck = false;
    ta.style.minHeight = "140px";
    ta.setAttribute("aria-label", "判题代码");
    c.appendChild(ta);
    var bar = el("div", "code-bar");
    var run = el("button", "btn-run", "▶ 运行判题");
    run.type = "button";
    bar.appendChild(run);
    var doneTag = el("span", "run-tag", lesson && isLessonDone(lesson.id) ? "已通关" : "");
    bar.appendChild(doneTag);
    c.appendChild(bar);
    var out = el("div", "out");
    var live = el("div", "live-region");
    live.setAttribute("aria-live", "polite");
    c.appendChild(out);
    c.appendChild(live);
    if (b.hint) c.appendChild(el("div", "hint", "💡 " + b.hint));
    run.onclick = function () {
      if (engineMode === "browse") { clear(out); out.textContent = "仅浏览模式：请刷新页面并加载引擎后再判题。"; return; }
      run.disabled = true;
      run.textContent = "判定中…";
      clear(out);
      out.appendChild(el("div", "run-tag", "判题中…"));
      checkExercise(ta.value, b.tests || [], b.stdin || []).then(function (r) {
        clear(out);
        out.appendChild(checkResultNode(r));
        if (r.passed) {
          var newLesson = lesson ? markLessonDone(lesson.id, lesson.xp || 60) : false;
          var exXp = b.xp || (lesson && lesson.exercise && lesson.exercise.xp) || 80;
          awardOnce("exercise:" + (lesson ? lesson.id : (b.title || "misc")), exXp);
          doneTag.textContent = "已通关";
          if (newLesson) toast("通关！+" + (lesson.xp || 60) + " XP / 练习 +" + exXp + " XP");
          else toast("判题通过（此前已记录）");
        }
        live.textContent = r.passed ? "判题通过" : "判题未通过";
      }).catch(function (e) {
        clear(out);
        out.appendChild(el("div", "term-out err", e.message));
        live.textContent = "判题失败";
      }).finally(function () {
        run.disabled = false;
        run.textContent = "▶ 运行判题";
      });
    };
    return c;
  }

  function renderBlock(b, lesson) {
    switch (b.type) {
      case "heading": return el("h2", "blk-heading", b.text);
      case "text": return el("div", "blk-text", b.text);
      case "tip": return el("div", "blk-tip", "💡 " + b.text);
      case "warn": return el("div", "blk-warn", "⚠ " + b.text);
      case "task": return el("div", "blk-task", b.text);
      case "steps": {
        var u = el("ul", "blk-steps");
        (b.items || []).forEach(function (it) { u.appendChild(el("li", null, it)); });
        return u;
      }
      case "diagram": return el("pre", "diagram", b.text);
      case "output": return el("pre", "term preview", b.text);
      case "table": return renderTable(b);
      case "code": return b.runnable === false ? el("pre", "term", b.code) : codeCard(b.code, null, b.stdin || [], b.lang || lesson.lang);
      case "quiz": return renderQuiz(b);
      case "order": return renderOrder(b);
      case "practice": return renderPractice(b);
      case "fill": return renderFill(b);
      case "compare": return renderCompare(b);
      case "visualize": return renderVisualize(b);
      case "exercise": return renderExercise(b, lesson);
      default: return el("div", "dim", "[未知块类型: " + b.type + "]");
    }
  }

  // ---------- 多语言对照 / 过程可视化 ----------
  function renderCompare(b) {
    var wrap = el("div", "blk-compare");
    if (b.title) wrap.appendChild(el("div", "blk-heading", b.title));
    (b.columns || []).forEach(function (col) {
      var box = el("div", "cmp-col");
      var head = el("div", "cmp-head");
      head.appendChild(el("span", "cmp-lang", langLabel(col.lang)));
      if (col.note) head.appendChild(el("span", "cmp-note", col.note));
      box.appendChild(head);
      box.appendChild(codeCard(col.code, null, [], col.lang));
      wrap.appendChild(box);
    });
    return wrap;
  }

  function renderVisualize(b) {
    var wrap = el("div", "blk-viz");
    var frames = b.frames || [];
    if (!frames.length) return wrap;
    if (b.title) wrap.appendChild(el("div", "blk-heading", b.title));
    if (b.caption) wrap.appendChild(el("div", "cmp-note", b.caption));
    var idx = 0;
    var stage = el("div", "viz-stage");
    var counter = el("span", "run-tag", "");
    function paint() {
      clear(stage);
      var f = frames[idx];
      stage.appendChild(el("div", "viz-label", f.label || ""));
      var hl = f.highlight || [];
      (f.items || []).forEach(function (it) {
        var changed = hl.indexOf(it.name) >= 0;
        var row = el("div", "viz-row" + (changed ? " changed" : ""));
        row.appendChild(el("span", "viz-name", it.name));
        row.appendChild(el("span", "viz-val", it.value));
        stage.appendChild(row);
      });
      counter.textContent = (idx + 1) + " / " + frames.length;
    }
    var bar = el("div", "code-bar");
    var prev = el("button", "btn-ghost", "◀");
    prev.type = "button";
    var next = el("button", "btn-ghost", "▶");
    next.type = "button";
    var reset = el("button", "btn-ghost", "重置");
    reset.type = "button";
    prev.onclick = function () { if (idx > 0) { idx--; paint(); } };
    next.onclick = function () { if (idx < frames.length - 1) { idx++; paint(); } };
    reset.onclick = function () { idx = 0; paint(); };
    bar.appendChild(prev); bar.appendChild(next); bar.appendChild(reset); bar.appendChild(counter);
    wrap.appendChild(stage);
    wrap.appendChild(bar);
    paint();
    return wrap;
  }

  // ---------- 页面：首页 ----------
  function statCard(label, value, sub, accent) {
    var c = el("div", "stat-card " + (accent || ""));
    c.appendChild(el("div", "stat-value", value));
    c.appendChild(el("div", "stat-label", label));
    if (sub) c.appendChild(el("div", "stat-sub", sub));
    return c;
  }

  function nextLesson() {
    var core = coreLessons();
    for (var i = 0; i < core.length; i++) if (!isLessonDone(core[i].id)) {
      return { lesson: core[i], locked: i > 0 && !isLessonDone(core[i - 1].id), prev: i > 0 ? core[i - 1] : null };
    }
    return { lesson: core[0] || null, locked: false, prev: null };
  }

  function renderHome() {
    clear(view);
    var core = coreLessons();
    var done = doneCount(core);
    var rank = rankFor(progress.xp);
    var next = nextLesson();

    var hero = el("section", "home-hero");
    var titleWrap = el("div", "hero-copy");
    titleWrap.appendChild(el("div", "eyebrow", "CODELEARNING · WEB TERMINAL"));
    titleWrap.appendChild(el("h1", "hero-title", "码上，就是马上。"));
    titleWrap.appendChild(el("p", "hero-sub", "六门语言、共 244 讲，assert 自动判题、变量快照。与手机端同源，打开浏览器就能写。"));
    var actions = el("div", "hero-actions");
    var startBtn = el("a", "btn-primary", next.lesson ? (done === 0 ? "开始第 1 讲" : "继续第 " + next.lesson.order + " 讲") : "回顾课程");
    startBtn.href = next.lesson ? "#/lesson/" + next.lesson.id : "#/lessons";
    actions.appendChild(startBtn);
    var termBtn = el("a", "btn-ghost", "打开终端");
    termBtn.href = "#/terminal";
    actions.appendChild(termBtn);
    titleWrap.appendChild(actions);
    hero.appendChild(titleWrap);

    var ring = el("div", "hero-ring");
    ring.appendChild(el("div", "ring-value", done + "/" + core.length));
    ring.appendChild(el("div", "ring-label", "主线课程"));
    ring.appendChild(el("div", "ring-rank", rank.name + " · " + progress.xp + " XP"));
    hero.appendChild(ring);
    view.appendChild(hero);

    var stats = el("div", "stat-grid");
    stats.appendChild(statCard("总经验", String(progress.xp), "XP", "cyan"));
    stats.appendChild(statCard("已通关", done + " / " + core.length, "主线课程", "green"));
    stats.appendChild(statCard("已解锁", String(Object.keys(progress.solved).length), "练习 / 挑战", "mag"));
    stats.appendChild(statCard("段位", rank.name, "按 XP 进阶", "yel"));
    view.appendChild(stats);

    view.appendChild(el("h2", "section-title", "本日行动"));
    var focus = el("div", "focus-card");
    if (next.lesson) {
      focus.appendChild(el("div", "focus-kicker", next.locked ? "LOCKED" : "NEXT UP"));
      focus.appendChild(el("div", "focus-title", next.lesson.title));
      focus.appendChild(el("div", "focus-sub", next.locked
        ? "先完成「" + (next.prev ? next.prev.title : "上一课") + "」即可解锁。"
        : (next.lesson.subtitle || "")));
      var go = el("a", "btn-primary small", next.locked ? "查看课程列表" : "进入第 " + next.lesson.order + " 讲");
      go.href = next.locked ? "#/lessons" : "#/lesson/" + next.lesson.id;
      focus.appendChild(go);
    }
    view.appendChild(focus);

    view.appendChild(el("h2", "section-title", "能力矩阵"));
    var grid = el("div", "feature-grid");
    [
      ["真离线执行", "Pyodide 把 CPython 3.13 编译成 WebAssembly，在浏览器本地运行。", "cyan"],
      ["assert 判题", "与 App 同一份 runner.py；测试不过不放行。", "green"],
      ["变量快照", "每次运行后展示命名空间里每个变量的类型与值。", "mag"],
      ["进度存档", "localStorage 记录通关与 XP，刷新、关页都不丢。", "yel"]
    ].forEach(function (it) {
      var c = el("div", "feature-card " + it[2]);
      c.appendChild(el("div", "feature-title", it[0]));
      c.appendChild(el("div", "feature-desc", it[1]));
      grid.appendChild(c);
    });
    view.appendChild(grid);
    setActiveNav("home");
  }

  // ---------- 页面：课程列表 ----------
  function renderLessons() {
    clear(view);
    var head = el("div", "page-head");
    head.appendChild(el("div", "eyebrow", "CURRICULUM"));
    head.appendChild(el("h1", "page-title", "课程 // LESSONS"));
    head.appendChild(el("p", "page-sub", "按章节解锁 · 完成判题即通关 · 共 " + lessons.length + " 讲（含内容中心扩展包）"));
    view.appendChild(head);

    var filter = el("div", "chip-row");
    var langs = Array.from(new Set(lessons.map(function (l) { return l.lang || "python"; })));
    var active = "all";
    var filters = [["all", "全部 " + lessons.length]].concat(langs.map(function (lg) {
      var n = lessons.filter(function (l) { return (l.lang || "python") === lg; }).length;
      return [lg, langLabel(lg) + " " + n];
    }));
    filters.forEach(function (f) {
      var b = el("button", "chip" + (f[0] === active ? " on" : ""), f[1]);
      b.type = "button";
      b.dataset.lang = f[0];
      b.onclick = function () {
        active = f[0];
        Array.prototype.forEach.call(filter.children, function (x) { x.classList.toggle("on", x === b); });
        paintList();
      };
      filter.appendChild(b);
    });
    view.appendChild(filter);

    var listWrap = el("div", "lesson-list");
    view.appendChild(listWrap);

    function paintList() {
      clear(listWrap);
      var shown = active === "all" ? lessons : lessons.filter(function (l) { return (l.lang || "python") === active; });
      shown.forEach(function (l, idx) {
        var ch = l.chapterName || "未分组";
        if (idx === 0 || shown[idx - 1].chapterName !== l.chapterName) {
          listWrap.appendChild(el("h2", "chapter-title", ch));
        }
        var lang = l.lang || "python";
        var groupList = shown.filter(function (x) { return (x.lang || "python") === lang; });
        var pos = groupList.indexOf(l);
        var locked = pos > 0 && !isLessonDone(groupList[pos - 1].id);
        var done = isLessonDone(l.id);
        var row = el("a", "lesson-row" + (done ? " done" : "") + (locked ? " locked" : ""));
        row.href = locked ? "#/lessons" : "#/lesson/" + l.id;
        row.setAttribute("aria-label", (locked ? "未解锁：" : "") + l.title);
        row.appendChild(el("div", "lesson-num", pad2(l.order)));
        var main = el("div", "lesson-main");
        main.appendChild(el("div", "lesson-title", l.title));
        main.appendChild(el("div", "lesson-sub", (l.subtitle || "") + (lang !== "python" ? " · " + lang.toUpperCase() : "")));
        row.appendChild(main);
        var side = el("div", "lesson-side");
        side.appendChild(el("span", "lesson-xp", "+" + (l.xp || 0)));
        side.appendChild(el("span", "lesson-icon", done ? icon("check") : locked ? icon("lock") : icon("play")));
        row.appendChild(side);
        if (locked) {
          row.onclick = function (e) { e.preventDefault(); toast("先完成上一讲即可解锁本节。"); };
        }
        listWrap.appendChild(row);
      });
    }
    paintList();
    setActiveNav("lessons");
  }

  // ---------- 页面：课程详情 ----------
  function renderLesson(ls) {
    clear(view);
    var back = el("a", "back-btn", "‹ 返回课程列表");
    back.href = "#/lessons";
    view.appendChild(back);

    var head = el("div", "lesson-head");
    head.appendChild(el("div", "eyebrow", (ls.chapterName || "") + " · " + (ls.lang || "python").toUpperCase()));
    head.appendChild(el("h1", null, ls.title));
    head.appendChild(el("div", "sub", (ls.subtitle || "") + " · +" + (ls.xp || 0) + " XP" + (isLessonDone(ls.id) ? " · 已通关" : "")));
    view.appendChild(head);

    var body = el("article", "lesson-body");
    (ls.blocks || []).forEach(function (b) { body.appendChild(renderBlock(b, ls)); });
    if (ls.exercise) body.appendChild(renderExercise(ls.exercise, ls));
    view.appendChild(body);

    var foot = el("div", "lesson-foot");
    var idx = lessons.indexOf(ls);
    if (idx > 0) {
      var prev = el("a", "btn-ghost", "‹ " + lessons[idx - 1].title);
      prev.href = "#/lesson/" + lessons[idx - 1].id;
      foot.appendChild(prev);
    }
    if (idx >= 0 && idx < lessons.length - 1) {
      var nxt = el("a", "btn-primary", lessons[idx + 1].title + " ›");
      nxt.href = "#/lesson/" + lessons[idx + 1].id;
      foot.appendChild(nxt);
    }
    view.appendChild(foot);
    setActiveNav("lessons");
    window.scrollTo(0, 0);
  }

  // ---------- 页面：终端 ----------
  function renderTerminal() {
    clear(view);
    var head = el("div", "page-head");
    head.appendChild(el("div", "eyebrow", "NEURAL INTERFACE"));
    head.appendChild(el("h1", "page-title", "终端 // TERMINAL"));
    head.appendChild(el("p", "page-sub", "有状态 CPython 会话：变量、函数跨行保留；↑/↓ 历史，Shift+Enter 换行。"));
    view.appendChild(head);

    var term = el("div", "repl-term");
    term.setAttribute("role", "log");
    term.setAttribute("aria-live", "polite");
    view.appendChild(term);

    var row = el("div", "repl-input-row");
    row.appendChild(el("span", "prompt", ">>>"));
    var ta = el("textarea", "repl-input");
    ta.rows = 2;
    ta.placeholder = "输入 Python 代码，Enter 执行，Shift+Enter 换行";
    ta.setAttribute("aria-label", "终端输入");
    row.appendChild(ta);
    var runBtn = el("button", "btn-run", "执行");
    runBtn.type = "button";
    row.appendChild(runBtn);
    view.appendChild(row);

    var bar = el("div", "code-bar");
    var reset = el("button", "btn-ghost", "⟲ 重置会话");
    reset.type = "button";
    bar.appendChild(reset);
    bar.appendChild(el("span", "run-tag", "Ctrl+L 清屏 · ↑/↓ 历史"));
    view.appendChild(bar);

    function print(text, cls) {
      var d = el("div", cls || "", text);
      term.appendChild(d);
      term.scrollTop = term.scrollHeight;
    }
    function exec() {
      var code = ta.value;
      if (!code.trim()) return;
      if (engineMode === "browse") { print("仅浏览模式：请刷新页面并加载引擎后再执行。", "term-out err"); return; }
      replHistory.push(code);
      replHistoryIdx = replHistory.length;
      print(">>> " + code, "repl-line-in");
      ta.value = "";
      replExec(code).then(function (r) {
        if (r.error) print((r.error.type || "Error") + ": " + (r.error.message || ""), "term-out err");
        else if (r.stdout) print(r.stdout.replace(/\n$/, ""), "term-out");
        if (r.variables && r.variables.length) {
          print("// 变量: " + r.variables.map(function (v) { return v.name + ":" + v.type + "=" + v.value; }).join("   "), "dim");
        }
        if (!r.error && !r.stdout && (!r.variables || !r.variables.length)) print("// （无输出）", "dim");
      }).catch(function (e) {
        print(e.message, "term-out err");
      });
    }
    runBtn.onclick = exec;
    ta.addEventListener("keydown", function (e) {
      if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); exec(); return; }
      if (e.key === "Enter" && e.shiftKey) {
        e.preventDefault();
        var start = ta.selectionStart, end = ta.selectionEnd;
        ta.value = ta.value.slice(0, start) + "\n" + ta.value.slice(end);
        ta.selectionStart = ta.selectionEnd = start + 1;
        return;
      }
      if (e.key === "ArrowUp" && replHistory.length) {
        e.preventDefault();
        replHistoryIdx = Math.max(0, replHistoryIdx - 1);
        ta.value = replHistory[replHistoryIdx] || "";
        return;
      }
      if (e.key === "ArrowDown" && replHistory.length) {
        e.preventDefault();
        replHistoryIdx = Math.min(replHistory.length, replHistoryIdx + 1);
        ta.value = replHistory[replHistoryIdx] || "";
        return;
      }
      if (e.ctrlKey && (e.key === "l" || e.key === "L")) {
        e.preventDefault();
        clear(term);
        return;
      }
    });
    reset.onclick = function () {
      replReset().then(function () {
        clear(term);
        print("// 会话已重置", "dim");
      }).catch(function (e) { print(e.message, "term-out err"); });
    };
    print("// 神经接口已连接 · " + (engineMode === "browse" ? "仅浏览模式" : "CPython 3.13 · 浏览器本地执行"), "dim");
    print("// 试试：1 + 1、import math、math.sqrt(2)", "dim");
    setActiveNav("terminal");
  }

  // ---------- 页面：角斗场 ----------
  // 网页端只有 Python 引擎；javascript 挑战展示为「App 内体验」卡片。
  function splitChallenges(list) {
    const python = [];
    const appOnly = [];
    (list || []).forEach(function (ch) {
      if ((ch.lang || "python") === "python") python.push(ch);
      else appOnly.push(ch);
    });
    return { python: python, appOnly: appOnly };
  }

  function renderArena() {
    clear(view);
    var head = el("div", "page-head");
    head.appendChild(el("div", "eyebrow", "ARENA"));
    head.appendChild(el("h1", "page-title", "角斗场 · 编程挑战（" + challenges.length + "）"));
    head.appendChild(el("p", "page-sub", "6 道 Python 挑战可直接判题；2 道 JavaScript 挑战请在 App 内体验。"));
    view.appendChild(head);

    const split = splitChallenges(challenges);
    var grid = el("div", "arena-list");
    split.python.forEach(function (ch) {
      var c = el("a", "arena-card" + (progress.solved["chal:" + ch.id] ? " done" : ""));
      c.href = "#/challenge/" + ch.id;
      c.appendChild(el("div", "at", ch.title));
      c.appendChild(el("div", "ad", "难度 " + (ch.difficulty || "") + " · +" + (ch.xp || 0) + " XP" + (progress.solved["chal:" + ch.id] ? " · 已通过" : "")));
      c.appendChild(el("div", "ab", ch.brief));
      grid.appendChild(c);
    });
    split.appOnly.forEach(function (ch) {
      var c = el("div", "arena-card app-only");
      var title = el("div", "at", ch.title);
      title.appendChild(el("span", "badge", "App 内体验"));
      c.appendChild(title);
      c.appendChild(el("div", "ad", (ch.lang || "javascript") + " · 难度 " + (ch.difficulty || "") + " · +" + (ch.xp || 0) + " XP"));
      c.appendChild(el("div", "ab", ch.brief));
      c.appendChild(el("div", "ab dim", "JavaScript 挑战需要手机端多语言运行时，请在「CodeLearning」App 内打开。"));
      grid.appendChild(c);
    });
    view.appendChild(grid);
    setActiveNav("arena");
  }

  function renderChallenge(ch) {
    clear(view);
    var back = el("a", "back-btn", "‹ 返回角斗场");
    back.href = "#/arena";
    view.appendChild(back);
    if ((ch.lang || "python") !== "python") {
      var notice = el("div", "card");
      notice.appendChild(el("div", "blk-heading", ch.title + " · " + ch.lang));
      notice.appendChild(el("div", "brief", "此挑战为 " + ch.lang + " 语言，网页端仅有 Python 引擎，请在「CodeLearning」App 内体验。"));
      view.appendChild(notice);
      return;
    }
    var c = el("div", "card");
    c.appendChild(el("div", "blk-heading", ch.title + " · 难度 " + (ch.difficulty || "")));
    c.appendChild(el("div", "brief", ch.brief));
    var ta = el("textarea", "code-area");
    ta.value = ch.starterCode || "";
    ta.spellcheck = false;
    ta.style.minHeight = "150px";
    ta.setAttribute("aria-label", "挑战代码");
    c.appendChild(ta);
    var bar = el("div", "code-bar");
    var run = el("button", "btn-run", "▶ 运行判题");
    run.type = "button";
    bar.appendChild(run);
    c.appendChild(bar);
    var out = el("div", "out");
    var live = el("div", "live-region");
    live.setAttribute("aria-live", "polite");
    c.appendChild(out);
    c.appendChild(live);
    if (ch.hint) c.appendChild(el("div", "hint", "💡 " + ch.hint));
    run.onclick = function () {
      if (engineMode === "browse") { clear(out); out.textContent = "仅浏览模式：请刷新页面并加载引擎后再判题。"; return; }
      run.disabled = true;
      run.textContent = "判定中…";
      clear(out);
      out.appendChild(el("div", "run-tag", "判题中…"));
      checkExercise(ta.value, ch.tests || [], []).then(function (r) {
        clear(out);
        out.appendChild(checkResultNode(r));
        if (r.passed) {
          var isNew = awardOnce("chal:" + ch.id, ch.xp || 100);
          toast(isNew ? "挑战通过！+" + (ch.xp || 100) + " XP" : "判题通过（此前已记录）");
        }
        live.textContent = r.passed ? "挑战通过" : "挑战未通过";
      }).catch(function (e) {
        clear(out);
        out.appendChild(el("div", "term-out err", e.message));
      }).finally(function () {
        run.disabled = false;
        run.textContent = "▶ 运行判题";
      });
    };
    view.appendChild(c);
    setActiveNav("arena");
  }

  // ---------- 段位与杂项 ----------
  var RANKS = [
    { name: "脚本小子", min: 0 }, { name: "数据幽灵", min: 150 }, { name: "网络浪人", min: 400 },
    { name: "义体黑客", min: 900 }, { name: "街头传奇", min: 1600 }, { name: "系统架构师", min: 2600 }
  ];
  function rankFor(xp) {
    var r = RANKS[0];
    RANKS.forEach(function (x) { if (xp >= x.min) r = x; });
    return r;
  }
  function pad2(n) { return String(n).padStart(2, "0"); }

  function toast(msg) {
    var t = el("div", "toast", msg);
    t.setAttribute("role", "status");
    document.body.appendChild(t);
    setTimeout(function () { t.classList.add("show"); }, 10);
    setTimeout(function () { t.classList.remove("show"); setTimeout(function () { t.remove(); }, 300); }, 2200);
  }

  function setActiveNav(tab) {
    document.querySelectorAll(".nav-btn[data-tab]").forEach(function (b) {
      b.classList.toggle("active", b.dataset.tab === tab);
    });
  }

  // ---------- 路由 ----------
  function parseRoute() {
    var hash = location.hash || "#/home";
    var m = hash.match(/^#\/([a-z-]+)(?:\/(.+))?$/);
    if (!m) return { name: "home" };
    var name = m[1];
    var arg = m[2] ? decodeURIComponent(m[2]) : "";
    if (name === "lesson") return { name: "lesson", id: arg };
    if (name === "challenge") return { name: "challenge", id: arg };
    if (["home", "lessons", "terminal", "arena"].indexOf(name) >= 0) return { name: name };
    return { name: "home" };
  }

  function route() {
    var r = parseRoute();
    progress.lastRoute = location.hash || "#/home";
    saveProgress();
    closeNav();
    if (r.name === "lesson") {
      var ls = lessons.find(function (l) { return l.id === r.id; });
      if (ls) { renderLesson(ls); return; }
      renderLessons();
      return;
    }
    if (r.name === "challenge") {
      var ch = challenges.find(function (c) { return c.id === r.id; });
      if (ch) { renderChallenge(ch); return; }
      renderArena();
      return;
    }
    if (r.name === "lessons") { renderLessons(); return; }
    if (r.name === "terminal") { renderTerminal(); return; }
    if (r.name === "arena") { renderArena(); return; }
    renderHome();
  }

  function closeNav() {
    var nav = document.getElementById("main-nav");
    var toggle = document.getElementById("nav-toggle");
    if (nav && toggle) {
      nav.classList.remove("open");
      toggle.setAttribute("aria-expanded", "false");
    }
  }

  // ---------- 启动 ----------
  function showBootError(err) {
    engineMode = "failed";
    setStatus("引擎离线", false);
    bootEl.classList.remove("hidden");
    clear(bootLine);
    bootLine.appendChild(document.createTextNode("> 引擎载入失败：" + (err && err.message ? err.message : "未知错误")));
    bootLine.appendChild(document.createElement("br"));
    bootLine.appendChild(document.createTextNode("已尝试 jsdelivr / fastly / unpkg。可重试，或先浏览课程内容。"));
    bootActions.hidden = false;
  }

  function enterApp() {
    bootEl.classList.add("hidden");
    route();
  }

  async function boot() {
    bootActions.hidden = true;
    try {
      await initEngine();
      enterApp();
    } catch (err) {
      console.error(err);
      showBootError(err);
    }
  }

  document.getElementById("boot-retry").onclick = function () {
    bootLine.textContent = "> 重试载入 CPython 内核…";
    bootActions.hidden = true;
    if (worker) { try { worker.terminate(); } catch (_) {} worker = null; }
    engineMode = "loading";
    boot();
  };
  document.getElementById("boot-browse").onclick = function () {
    engineMode = "browse";
    setStatus("仅浏览模式", false);
    enterApp();
  };

  document.getElementById("nav-toggle").onclick = function () {
    var nav = document.getElementById("main-nav");
    var open = nav.classList.toggle("open");
    this.setAttribute("aria-expanded", open ? "true" : "false");
  };

  document.getElementById("theme-toggle").onclick = function () {
    var dark = document.documentElement.getAttribute("data-theme") !== "light";
    document.documentElement.setAttribute("data-theme", dark ? "light" : "dark");
    try { localStorage.setItem(THEME_KEY, dark ? "light" : "dark"); } catch (_) {}
  };
  try {
    if (localStorage.getItem(THEME_KEY) === "light") document.documentElement.setAttribute("data-theme", "light");
  } catch (_) {}

  document.getElementById("reset-progress").onclick = function () {
    if (!window.confirm("确定清空本机保存的全部学习进度（XP、通关记录）？此操作不可撤销。")) return;
    progress = defaultProgress();
    saveProgress();
    toast("本地进度已清空");
    route();
  };

  window.addEventListener("hashchange", route);
  boot();
})();

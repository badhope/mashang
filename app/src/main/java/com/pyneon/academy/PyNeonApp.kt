package com.pyneon.academy

import android.app.Application
import com.pyneon.academy.c.CBridge
import com.pyneon.academy.debug.CrashHandler
import com.pyneon.academy.debug.DebugLog
import com.pyneon.academy.js.JsBridge
import com.pyneon.academy.lang.LanguageRegistry
import com.pyneon.academy.lua.LuaBridge
import com.pyneon.academy.py.PyBridge
import com.pyneon.academy.sql.SqlBridge
import com.pyneon.academy.ts.TsBridge

class PyNeonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 最先安装崩溃处理器：后续任何初始化失败都能留下可导出的诊断信息
        CrashHandler.install(this)
        // 注册所有可用语言运行时（新增语言在此处补一行 register 即可）
        LanguageRegistry.register(PyBridge)
        LanguageRegistry.register(JsBridge)
        LanguageRegistry.register(TsBridge)
        LanguageRegistry.register(CBridge)
        LanguageRegistry.register(LuaBridge)
        LanguageRegistry.register(SqlBridge)
        DebugLog.i("App", "启动 v" + com.pyneon.academy.utils.AppConstants.VERSION_NAME +
            "，已注册运行时 " + LanguageRegistry.ids().joinToString(","))
        // 不在 Application.onCreate 里启动 CPython。
        //
        // 原因：Python.start() 是同步的，会在这里阻塞主线程加载 5.4MB 的
        // libpython3.13.so + 4MB 标准库，直接把冷启动时间拉长 1-2 秒，
        // 且此时还没有界面，用户看到的是白屏。
        // 现在交给 BootScreen 的 LaunchedEffect：那里已经在后台线程执行，
        // 并有启动动画作为视觉缓冲（PyBridge.ensureStarted 内部有幂等保护）。
        DebugLog.i("App", "运行时就绪（延迟到 BootScreen 初始化）")
    }
}

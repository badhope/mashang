package com.pyneon.academy.utils

import android.content.Context
import java.util.Locale

// 简单文案表。中文为主，英文和日文有基础版本。
object I18n {
    
    fun getString(context: Context, key: String): String {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "zh" -> getZhString(key)
            "ja" -> getJaString(key)
            else -> getEnString(key)
        }
    }
    
    private fun getZhString(key: String): String {
        return when (key) {
            "welcome.title" -> "欢迎使用 CodeLearning"
            "welcome.subtitle" -> "码上编程学院"
            "welcome.offline.title" -> "完全离线学习"
            "welcome.offline.desc" -> "内置语言运行时\n地铁、飞机、没网的地方都能学\n随时打开随时练"
            "welcome.gamification.title" -> "游戏化成长"
            "welcome.gamification.desc" -> "XP 和段位\n每日任务、成就徽章\n学一点就有一点反馈"
            "welcome.snapshot.title" -> "变量快照"
            "welcome.snapshot.desc" -> "运行后能看变量\n名字、类型、值都列出来\n对着代码理解执行过程"
            "welcome.plan.title" -> "7天入门计划"
            "welcome.plan.desc" -> "每天两节课\n从基础语法到面向对象\n跟着做完能自己写小项目"
            "button.next" -> "下一步"
            "button.skip" -> "跳过"
            "button.start" -> "开始学习"
            else -> key
        }
    }
    
    private fun getEnString(key: String): String {
        return when (key) {
            "welcome.title" -> "Welcome to CodeLearning"
            "welcome.subtitle" -> "Learn to Code, Offline"
            "welcome.offline.title" -> "Learn Completely Offline"
            "welcome.offline.desc" -> "Built-in language runtimes\nLearn on subway, plane, anywhere\nNo internet needed"
            "welcome.gamification.title" -> "Gamified Progress"
            "welcome.gamification.desc" -> "XP and ranks\nDaily missions and badges\nA little progress every day"
            "welcome.snapshot.title" -> "Variable Snapshot"
            "welcome.snapshot.desc" -> "See variables after running\nNames, types and values\nUnderstand how the code runs"
            "welcome.plan.title" -> "7-Day Starter Plan"
            "welcome.plan.desc" -> "2 lessons per day\nMaster coding step by step\nFrom basics to OOP"
            "button.next" -> "Next"
            "button.skip" -> "Skip"
            "button.start" -> "Start Learning"
            else -> key
        }
    }
    
    private fun getJaString(key: String): String {
        return when (key) {
            "welcome.title" -> "CodeLearning へようこそ"
            "welcome.subtitle" -> "今すぐコードを書こう"
            "button.next" -> "次へ"
            "button.skip" -> "スキップ"
            "button.start" -> "学習開始"
            else -> getEnString(key)
        }
    }
}

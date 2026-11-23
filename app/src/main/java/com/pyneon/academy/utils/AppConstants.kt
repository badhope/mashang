package com.pyneon.academy.utils

import com.pyneon.academy.BuildConfig

object AppConstants {
    // 发版只改 build.gradle.kts，这里从 BuildConfig 取。
    val VERSION_NAME: String = BuildConfig.VERSION_NAME
    val VERSION_CODE: Int = BuildConfig.VERSION_CODE

    const val APP_NAME = "CodeLearning"
    const val APP_NAME_CN = "码上"
    const val APP_NAME_EN = "CodeLearning"

    // 国内优先用 GitCode，GitHub 只做镜像。应用内链接别默认跳 GitHub，
    // 不然没代理的时候「查看源码」很容易打不开。
    const val REPO_URL = "https://gitcode.com/badhope/mashang"
    const val REPO_URL_GITHUB = "https://github.com/badhope/mashang"
    const val REPO_URL_GITEE = "https://gitee.com/badhope/mashang"
    const val SITE_URL = "https://badhope.github.io/mashang/"
    const val USER_GUIDE_URL = "$REPO_URL/blob/main/docs/USER_GUIDE.md"

    const val SHARE_HASHTAG = "#CodeLearning #码上 #编程学习"

    // 上架资料，发版前确认这里和华为后台填的一致。
    const val DEV_NAME = "bigbad"
    const val CONTACT_EMAIL = "bigbad338@outlook.com"

    // 隐私政策也走 GitCode，审核员那边打开更稳。
    const val PRIVACY_POLICY_URL = "$REPO_URL/blob/main/PRIVACY_POLICY.md"
    const val TERMS_URL = "$REPO_URL/blob/main/TERMS_OF_SERVICE.md"
}

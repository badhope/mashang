plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.chaquo.python")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
}

android {
    namespace = "com.pyneon.academy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pyneon.academy"
        // minSdk 29（Android 10）：放弃 Android 7-9。
        // 决策依据（2026-11）：
        //   1. 这几个版本上的备份/证书保存需要 WRITE_EXTERNAL_STORAGE 运行时权限，
        //      与「最小权限」和 Scoped Storage 方向冲突；
        //   2. Android 9 及以下在国内活跃设备占比已很低；
        //   3. 维护两套存储分支的测试成本高于收益。
        // 提升后备份/海报统一走 MediaStore（API 29+ 唯一路径），代码与测试都更简单。
        minSdk = 29
        targetSdk = 35
        versionCode = 24
        versionName = "0.7.5"
        // 版本号单一来源：BuildConfig.VERSION_NAME / VERSION_CODE
        buildConfigField("String", "VERSION_NAME", "\"$versionName\"")
        buildConfigField("int", "VERSION_CODE", "$versionCode")
        // Instrumented 测试（真实 Android 运行时，验证页面能否打开与交互）
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            // F6: 发布版只打 arm64-v8a（真机覆盖 99.9%，APK 体积减半）。
            // 需要装到 x86_64 模拟器验收时加 -PemulatorAbi=true（Chaquopy 只读
            // defaultConfig / productFlavors 的 abiFilters，buildType 里的不生效）。
            abiFilters += listOf("arm64-v8a")
            if ((findProperty("emulatorAbi") as String?)?.toBoolean() == true) {
                abiFilters += "x86_64"
            }
        }
    }

    signingConfigs {
        // D1: 签名凭据改由环境变量注入；任一缺失则不创建 release 签名（不硬编码失败）
        val ksPath = System.getenv("KEYSTORE_PATH")
        val ksPass = System.getenv("KEYSTORE_PASSWORD")
        val keyPass = System.getenv("KEY_PASSWORD")
        val ksAlias = System.getenv("KEY_ALIAS")
        if (!ksPath.isNullOrBlank() && !ksPass.isNullOrBlank() && !keyPass.isNullOrBlank() && !ksAlias.isNullOrBlank()) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            // 生产构建启用 R8 压缩/混淆，缩小 APK 体积
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // D1: 仅当环境变量齐备、release 签名已创建时才挂载，否则保持未签名
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }

        /**
         * 本地可安装的「类正式版」构建。
         *
         * 与 release 的差异只有一处：用 debug keystore 签名。
         * 目的：让你**不需要生成任何密钥**就能装到手机上测真实体验——
         * 开启 R8 与资源压缩（体积 ~25MB，dex ~5MB），debuggable=false
         * （不可被调试器附加、无调试开销），性能与正式版一致。
         *
         * **不要用它发布**：
         *  - 证书主体是 CN=Android Debug，商店会拒收；CI 的签名校验也会失败
         *  - 无法覆盖安装到正式版（签名不同），反之亦然
         *
         * 用法：./gradlew :app:assembleLocalRelease
         * 产物：app/build/outputs/apk/localRelease/app-localRelease.apk
         */
        create("localRelease") {
            initWith(getByName("release"))
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            isDebuggable = false
            // 用 debug 证书签名：本机 ~/.android/debug.keystore，Gradle 自动生成
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
            // Rhino 自带的调试器脚本与示例（我们只用解释器核心，不用 GUI 调试器）
            "org/mozilla/javascript/tools/debugger/**",
            "org/mozilla/javascript/tools/shell/**",
            // Eclipse JDT 注解库把 .java 源码打进了 jar，运行时用不到
            "src/**",
            // Kotlin 协程调试探针（release 无调试器，纯冗余）
            "DebugProbesKt.bin",
            "META-INF/*.kotlin_module",
        )
    }
    lint {
        // 本机 local.properties 的路径转义告警不影响构建（CI 上不生成该文件）
        lintConfig = file("lint.xml")
    }
    testOptions {
        // Instrumented 测试里禁用动画，避免 UI 断言因动画竞态而闪失败
        animationsDisabled = true
        unitTests {
            // Robolectric 需要访问 android 资源与 manifest 才能渲染真实 UI
            isIncludeAndroidResources = true
            all {
                // Robolectric 默认从 repo1.maven.org 拉 android-all 镜像，国内常超时。
                // 统一走项目已在用的阿里云镜像，保证单测可重复执行。
                it.systemProperty(
                    "robolectric.dependency.repo.url",
                    "https://maven.aliyun.com/repository/public",
                )
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

chaquopy {
    defaultConfig {
        version = "3.13"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // 缺失依赖补充：viewModelScope / asLiveData / liveData 来自 lifecycle-ktx
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    // LiveData → Compose State 桥接（observeAsState，来自 androidx.compose.runtime.livedata）
    implementation("androidx.compose.runtime:runtime-livedata:1.7.6")
    // kotlinx-serialization：BackupUtil 备份 JSON 序列化所需
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // QR code for certificate poster
    implementation("com.google.zxing:core:3.5.4")

    // F3: 代码编辑器替换自研 BasicTextField —— 复用开源成熟实现（sora-editor, LGPL-2.1）
    // 提供 Python 增量语法高亮、自动缩进、行号、撤销/重做；不再自研编辑器。
    implementation(platform("io.github.rosemoe:editor-bom:0.24.7"))
    implementation("io.github.rosemoe:editor")
    implementation("io.github.rosemoe:language-textmate")

    // 多语言：JavaScript 运行时后端（Rhino，纯 Java 解释模式，无 NDK；MPL-2.0）
    implementation("org.mozilla:rhino:1.7.15")
    // 多语言：Lua 运行时后端（LuaJ 3.0.1，Lua 5.2 语义，纯 JVM 354 KB，无 NDK；MIT）
    implementation("org.luaj:luaj-jse:3.0.1")

    // JVM 单元测试（纯 JVM，不需要 Android 设备）：JUnit4 + Kotlin 断言
    testImplementation("junit:junit:4.13.2")

    // Robolectric：在 JVM 上跑真实 Android 运行时（不需要模拟器/虚拟化）。
    // 用于验证 Compose 页面能否真正创建、渲染、响应点击——这是本机唯一可行的
    // UI 验证路径（CPU 不支持 VT-x，模拟器无法启动）。
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Instrumented UI 测试：Compose 语义树 + Activity 场景
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // Android 单测里的 org.json 是空桩（方法全抛 "not mocked"），
    // 课程包测试需要真实 JSON 解析，用官方 org.json 实现覆盖测试类路径。
    testImplementation("org.json:json:20240303")
}

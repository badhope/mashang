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
        minSdk = 24
        targetSdk = 35
        versionCode = 15
        versionName = "0.4.0"
        // 版本号单一来源：BuildConfig.VERSION_NAME / VERSION_CODE
        buildConfigField("String", "VERSION_NAME", "\"$versionName\"")
        buildConfigField("int", "VERSION_CODE", "$versionCode")
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
        resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
    }
    lint {
        // 本机 local.properties 的路径转义告警不影响构建（CI 上不生成该文件）
        lintConfig = file("lint.xml")
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
    implementation(platform("io.github.rosemoe:editor-bom:0.24.6"))
    implementation("io.github.rosemoe:editor")
    implementation("io.github.rosemoe:language-textmate")

    // 多语言：JavaScript 运行时后端（Rhino，纯 Java 解释模式，无 NDK；Apache-2.0）
    implementation("org.mozilla:rhino:1.9.1")

    // JVM 单元测试（纯 JVM，不需要 Android 设备）：JUnit4 + Kotlin 断言
    testImplementation("junit:junit:4.13.2")
}

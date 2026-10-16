import com.android.build.api.variant.impl.VariantOutputImpl
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.aazhou.mtforum"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.aazhou.mtforum"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.2"
    }

    signingConfigs {
        val props = Properties()

        // 1) 优先从 local.properties 读（CI 环境写在这个文件里）
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            FileInputStream(localPropsFile).use { props.load(it) }
        }

        // 判断 local.properties 是否提供了有效的签名信息
        val hasLocalSigning = props.getProperty("RELEASE_STORE_FILE")?.isNotBlank() == true

        // 2) CI 环境：用 RELEASE_* 大写键
        if (hasLocalSigning) {
            create("release") {
                storeFile = file(props.getProperty("RELEASE_STORE_FILE"))
                storePassword = props.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = props.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = props.getProperty("RELEASE_KEY_PASSWORD")
            }
        } else {
            // 3) 本地环境：从 ../../.secrets 读小写键
            val keystoreProps = rootProject.file("../../../.secrets/mtf/keystore.properties")
            if (keystoreProps.exists()) {
                val localProps = Properties()
                FileInputStream(keystoreProps).use { localProps.load(it) }
                if (localProps.isNotEmpty()) {
                    create("release") {
                        storeFile = file(localProps.getProperty("storeFile"))
                        storePassword = localProps.getProperty("storePassword")
                        keyAlias = localProps.getProperty("keyAlias")
                        keyPassword = localProps.getProperty("keyPassword")
                    }
                }
            }
        }
    }

    buildTypes {
        release {
            // 优先用正式签名（仓库外 .secrets/mtf/keystore.properties），缺失时回退 debug 签名
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)

    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.backdrop)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            (output as VariantOutputImpl).outputFileName.set("MTF-v${output.versionName.get()}.apk")
        }
    }
}

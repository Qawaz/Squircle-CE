/*
 * Copyright 2023 Squircle CE contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.blacksquircle.gradle.Gradle
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

class ApplicationModulePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("kotlin-android")
                apply("kotlin-kapt")
                apply("dagger.hilt.android.plugin")
            }

            configure<BaseAppModuleExtension> {
                compileSdk = Gradle.Build.compileSdk

                defaultConfig {
                    minSdk = Gradle.Build.minSdk
                    targetSdk = Gradle.Build.targetSdk

                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                setFlavorDimensions(listOf("store"))
                productFlavors {
                    create("googlePlay") { dimension = "store" }
                    create("fdroid") { dimension = "store" }
                }
                val properties = Properties().apply {
                    val localFile = rootProject.file("local.properties")
                    if (localFile.exists()) {
                        load(localFile.inputStream())
                    }
                }
                signingConfigs {
                    create("release") {
                        storeFile = file("${properties["KEYSTORE_PATH"]}")
                        storePassword = "${properties["KEYSTORE_PASSWORD"]}"
                        keyAlias = "${properties["KEY_ALIAS"]}"
                        keyPassword = "${properties["KEY_PASSWORD"]}"
                    }
                }
                buildTypes {
                    release {
                        signingConfig = signingConfigs.getByName("release")
                        isMinifyEnabled = true
                        isShrinkResources = false
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                    }
                    create("benchmark") {
                        signingConfig = signingConfigs.getByName("debug")
                        isDebuggable = false
                        matchingFallbacks += listOf("release")
                        proguardFiles("benchmark-rules.pro")
                    }
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                tasks.withType<KotlinCompile>().configureEach {
                    kotlinOptions {
                        jvmTarget = "17"
                    }
                }
                sourceSets {
                    named("androidTest") {
                        java.srcDir("src/androidTest/kotlin")
                    }
                    named("fdroid") {
                        java.srcDir("src/fdroid/kotlin")
                    }
                    named("googlePlay") {
                        java.srcDir("src/googlePlay/kotlin")
                    }
                    named("main") {
                        java.srcDir("src/main/kotlin")
                    }
                    named("test") {
                        java.srcDir("src/test/kotlin")
                    }
                }
            }
        }
    }
}
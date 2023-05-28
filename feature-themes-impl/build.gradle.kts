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

plugins {
    id("feature-module")
}

android {
    namespace = "com.blacksquircle.ui.feature.themes"

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // Core
    implementation(libs.kotlin)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.timber)

    // UI
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.materialdialogs.core)
    implementation(libs.materialdialogs.color)

    // AAC
    implementation(libs.androidx.viewmodel)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.navigation)

    // Network
    implementation(libs.gson)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // DI
    implementation(libs.hilt)
    implementation(libs.androidx.hilt.navigation)
    kapt(libs.hilt.compiler)

    // Modules
    implementation(project(":feature-settings-api"))
    implementation(project(":feature-themes-api"))
    implementation(project(":common-core"))
    implementation(project(":common-ui"))

    // Tests
    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.test.junit.ext)
    androidTestImplementation(libs.test.runner)
}
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.sqldelight)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

// Error is ok
sqldelight {
    databases {
        register("MapViewerDB") {
            packageName.set("site.cliftbar.mapviewer")
            generateAsync.set(true)
        }
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    js {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(project.projectDir.path + "/src/webMain/resources")
                    }
                }
            }
            val webpackConfigPath = project.projectDir.path + "/webpack.config.d/sqljs-fix.js"
            project.mkdir(project.projectDir.path + "/webpack.config.d")
            val webpackConfig = project.file(webpackConfigPath)
            webpackConfig.writeText("""
                const CopyWebpackPlugin = require('copy-webpack-plugin');
                const path = require('path');

                config.plugins.push(
                    new CopyWebpackPlugin({
                        patterns: [
                            {
                                from: path.resolve(__dirname, '../../node_modules/sql.js/dist/sql-wasm.wasm'),
                                to: 'sql-wasm.wasm'
                            },
                            {
                                from: path.resolve(__dirname, '../../node_modules/sql.js/dist/sql-wasm.wasm'),
                                to: 'sqljs.worker.js/sql-wasm.wasm'
                            }
                        ]
                    })
                );

                config.resolve.fallback = {
                    "fs": false,
                    "path": false,
                    "crypto": false
                };

                // Suppress "Critical dependency: the request of a dependency is an expression"
                // which comes from sql.js dynamic loading
                if (!config.ignoreWarnings) config.ignoreWarnings = [];
                config.ignoreWarnings.push(/Critical dependency: the request of a dependency is an expression/);
            """.trimIndent())
        }
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(project.projectDir.path + "/src/webMain/resources")
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqldelight.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.play.services.location)
            implementation(libs.yamlkt)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
            implementation(compose.materialIconsExtended)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenModel)
            implementation(libs.voyager.tab.navigator)
            implementation(libs.voyager.transitions)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.xmlutil)
            implementation(libs.serialutil)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.sqldelight.desktop)
                implementation(libs.robolectric)
                implementation("androidx.test:core:1.6.1")
                implementation(libs.kotlin.test)
            }
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqldelight.desktop)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.yamlkt)
            implementation(libs.kotlinx.datetime)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.ios)
            implementation(libs.ktor.client.darwin)
            implementation(libs.yamlkt)
        }
        iosMain.get().dependsOn(commonMain.get())

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        listOf(iosX64Main, iosArm64Main, iosSimulatorArm64Main).forEach {
            it.dependsOn(iosMain.get())
        }

        val webMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.sqldelight.web)
                implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.2.1"))
                implementation(npm("sql.js", "1.12.0"))
                implementation(devNpm("copy-webpack-plugin", "11.0.0"))
            }
        }

        jsMain.dependencies {
            implementation(libs.sqldelight.web)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqldelight.web)
        }

        jsMain.get().dependsOn(webMain)
        wasmJsMain.get().dependsOn(webMain)
    }
}

android {
    namespace = "site.cliftbar.mapviewer"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "site.cliftbar.mapviewer"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "site.cliftbar.mapviewer.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "site.cliftbar.mapviewer"
            packageVersion = "1.0.0"
        }
    }
}

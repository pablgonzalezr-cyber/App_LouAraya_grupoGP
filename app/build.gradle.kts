plugins {
    jacoco
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.3.10"

}

fun urlCatalogoLiteral(url: String): String =
    "\"" + url.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val urlCatalogo = providers.gradleProperty("louCatalogoUrl")
val urlCotizaciones = providers.gradleProperty("louCotizacionesUrl")
val urlCatalogoAdmin = providers.gradleProperty("louCatalogoAdminUrl")

android {
    namespace = "com.example.applouaraya_grupogp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.applouaraya_grupogp"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = providers.gradleProperty("louCoberturaSamsung").orNull == "true"
            buildConfigField("String", "COTIZACIONES_URL", urlCatalogoLiteral(urlCotizaciones.orElse("http://127.0.0.1:8096/").get()))
            val publica = urlCatalogo.orElse("http://127.0.0.1:8095/").get()
            buildConfigField("String", "CATALOGO_URL", urlCatalogoLiteral(publica))
            buildConfigField("String", "CATALOGO_ADMIN_URL", urlCatalogoLiteral(urlCatalogoAdmin.orElse(publica).get()))
        }
        release {
            buildConfigField("String", "COTIZACIONES_URL", urlCatalogoLiteral(urlCotizaciones.orElse("").get()))
            val publica = urlCatalogo.orElse("").get()
            buildConfigField("String", "CATALOGO_URL", urlCatalogoLiteral(publica))
            buildConfigField("String", "CATALOGO_ADMIN_URL", urlCatalogoLiteral(urlCatalogoAdmin.orElse(publica).get()))
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

jacoco { toolVersion = "0.8.14" }

// Clase original, antes de instrumentar. Los paquetes forman el denominador
// completo de cada informe; no se filtran ViewModels ni repositorios individuales.
val clasesDebug = layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
for ((nombre, logica) in listOf("coberturaDebug" to false, "coberturaLogicaDebug" to true)) {
    tasks.register<JacocoReport>(nombre) {
        dependsOn("testDebugUnitTest")
        val arbol = fileTree(clasesDebug) {
            exclude("**/*_Impl*.class", "**/ComposableSingletons*.class")
            if (logica) {
                include("**/validation/**", "**/repository/**", "**/viewmodel/**", "**/model/**",
                    "**/navigation/PoliticaNavegacion*.class")
            }
        }
        classDirectories.setFrom(arbol)
        sourceDirectories.setFrom("src/main/java")
        executionData.from(layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"))
        if (providers.gradleProperty("louCoberturaSamsung").orNull == "true") {
            executionData.from(fileTree(layout.buildDirectory.dir("coverage-samsung")) { include("*.ec") })
        }
        reports {
            html.required.set(true)
            xml.required.set(true)
        }
    }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.maplibre.gl:android-sdk-opengl:13.7.1")
    implementation("androidx.camera:camera-camera2:1.5.3")
    implementation("androidx.camera:camera-lifecycle:1.5.3")
    implementation("androidx.camera:camera-view:1.5.3")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.zxing:core:3.5.4")
    // Alinea core/json usados por Navigation y room-testing al leer esquemas históricos.
    implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.8.1"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
    androidTestImplementation("androidx.test:rules:1.5.0")


}

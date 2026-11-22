plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.3.10"

}

fun urlCatalogoLiteral(url: String): String =
    "\"" + url.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val urlCatalogo = providers.gradleProperty("louCatalogoUrl")
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
            val publica = urlCatalogo.orElse("http://127.0.0.1:8095/").get()
            buildConfigField("String", "CATALOGO_URL", urlCatalogoLiteral(publica))
            buildConfigField("String", "CATALOGO_ADMIN_URL", urlCatalogoLiteral(urlCatalogoAdmin.orElse(publica).get()))
        }
        release {
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

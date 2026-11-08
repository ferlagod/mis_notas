import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Firma de release: se lee de keystore.properties (no versionado) o de variables de entorno (CI).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStoreFile = signingValue("storeFile", "MISNOTAS_KEYSTORE_FILE")

android {
    namespace = "com.ferlagod.misnotas"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ferlagod.misnotas"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("storePassword", "MISNOTAS_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "MISNOTAS_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "MISNOTAS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            optimization {
                enable = true
            }
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    androidResources {
        // Declara los idiomas al sistema (Ajustes → Apps → Mis Notas → Idioma en Android 13+).
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    bundle {
        // El idioma se elige dentro de la app: el AAB debe incluir todas las traducciones.
        language {
            enableSplit = false
        }
    }
    dependenciesInfo {
        // Sin bloque de dependencias cifrado por Google (requisito para F-Droid / builds reproducibles).
        includeInApk = false
        includeInBundle = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.navigation.compose)

    // Base de datos local
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Sincronización WebDAV y en segundo plano
    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime.ktx)

    // Imágenes
    implementation(libs.coil.compose)

    // Credenciales cifradas
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

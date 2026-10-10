import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.helinesca.bugunneyemek"
    compileSdk = 36

    defaultConfig {
        // Temporary ID for the owner's test build; the real one is decided before any store release.
        applicationId = "com.helinesca.bugunneyemek.dev"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-test"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

/** Copies the catalog files from ../data into the app's assets at build time (no second copy is kept in git). */
abstract class CopyCatalog : DefaultTask() {
    @get:InputFiles
    abstract val sources: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val target = outputDir.get().asFile.resolve("catalog")
        target.deleteRecursively()
        target.mkdirs()
        sources.files.forEach { it.copyTo(target.resolve(it.name), overwrite = true) }
    }
}

val copyCatalog = tasks.register<CopyCatalog>("copyCatalog") {
    val data = rootProject.layout.projectDirectory.dir("../data")
    sources.from(
        listOf(
            "pantry.tsv", "dishes.tsv", "recipe_ingredients.tsv", "recipe_steps.tsv",
            "alerji_gruplari.tsv", "temel_malzemeler.tsv",
        ).map { data.file(it) }
    )
    outputDir.set(layout.buildDirectory.dir("generated/catalogAssets"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyCatalog, CopyCatalog::outputDir)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core:1.7.8")

    testImplementation("junit:junit:4.13.2")
}

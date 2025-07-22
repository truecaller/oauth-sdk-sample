buildscript {
    val kotlin_version by extra("2.2.0")

    repositories {
        mavenCentral()
        google()
        maven(url = "https://maven.google.com")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.8.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

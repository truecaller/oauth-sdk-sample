buildscript {
    val kotlin_version by extra("2.2.10")

    repositories {
        mavenCentral()
        google()
        maven(url = "https://maven.google.com")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:9.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

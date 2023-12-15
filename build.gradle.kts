plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.intellij") version "1.13.1"
}

group = "com.easy-query"
version = "0.0.11"

repositories {
    maven {
        //setUrl("https://maven.aliyun.com/nexus/content/groups/public/")
        setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2.5")
//    version.set("2022.2.5")
//    version.set("2023.3")
    type.set("IU") // Target
    // IDE Platform

    plugins.set(listOf("com.intellij.java", "org.jetbrains.kotlin", "com.intellij.database"))
}
dependencies {
//    implementation("com.intellij:forms_rt:7.0.3")
    implementation("cn.hutool:hutool-core:5.8.22")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.41")
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        options.encoding = "utf-8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    patchPluginXml {
        sinceBuild.set("202.*")
        untilBuild.set("233.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
    buildSearchableOptions {
        enabled = false
    }
}

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.intellij") version "1.13.1"
}

group = "com.easy-query"
version = "0.0.93"

repositories {
    maven {
        setUrl("https://maven.aliyun.com/repository/central/")
//        setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
//    version.set("2023.2.5")
//    version.set("2022.2.5")
//    version.set("2023.3")
    version.set("2024.1")
    type.set("IU") // Target
    // IDE Platform

    plugins.set(listOf("com.intellij.java", "org.jetbrains.kotlin", "com.intellij.database"))
}
dependencies {
//    implementation("com.intellij:forms_rt:7.0.3")
    implementation("cn.hutool:hutool-core:5.8.25")
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.41")

    implementation ("ch.qos.logback:logback-classic:1.4.12") // Logback 依赖
    implementation ("org.slf4j:slf4j-api:1.7.30") // SLF4J API 依赖

    // 引入 lombok
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")

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
        //插件起始支持版本
        sinceBuild.set("222")
        //插件结束支持版本
        untilBuild.set("243.*")
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

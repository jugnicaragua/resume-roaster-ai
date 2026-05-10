plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "ni.jug"
version = "0.0.1-SNAPSHOT"
description = "resume-roaster-ai"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.webmvc)
    compileOnly(libs.lombok)
    developmentOnly(libs.spring.boot.devtools)
    annotationProcessor(libs.lombok)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testCompileOnly(libs.lombok)
    testRuntimeOnly(libs.junit.platform.launcher)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

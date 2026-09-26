plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("io.freefair.lombok") version "8.13"
}

group = "org.example"
version = "0.0.1-SNAPSHOT"
description = "NoteTaskBackend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Реактивный веб-стек (WebClient + Netty)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Actuator (healthcheck для Docker)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Безопасность
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Валидация DTO
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // DevTools для локальной разработки
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // --- Тесты ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.testcontainers:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}
plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.kotlin"
version = "0.0.1-SNAPSHOT"
description = "spring-event-example"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // WebFlux (реактивный стек)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Reactor Kotlin extensions
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    // Kotlin coroutines поддержка
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Spring Data Reactive (если будете использовать реактивную БД)
    // implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    // или
    // implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    testImplementation("io.projectreactor:reactor-test")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

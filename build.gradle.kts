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

// Определите версии для зависимостей вне BOM
val r2dbcPostgresVersion = "0.8.13.RELEASE"
val postgresqlVersion = "42.7.3"
val flywayVersion = "11.20.0"

dependencies {
    // ============== БАЗОВЫЕ ЗАВИСИМОСТИ ==============
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ============== REACTOR И КОРУТИНЫ ==============
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // ============== R2DBC (РЕАКТИВНЫЙ POSTGRESQL) ==============
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.r2dbc:r2dbc-postgresql:$r2dbcPostgresVersion") // С правильной версией

    // ============== ДЛЯ МИГРАЦИЙ (liquibase) ==============
    implementation("org.liquibase:liquibase-core:4.27.0")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.postgresql:postgresql:$postgresqlVersion")

    // ============== ТЕСТИРОВАНИЕ ==============
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
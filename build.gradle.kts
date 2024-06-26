import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.0.1"
	id("io.spring.dependency-management") version "1.1.0"
	id("com.adarshr.test-logger") version "3.2.0"
	kotlin("jvm") version "1.7.0"
	kotlin("plugin.spring") version "1.7.0"
	id("groovy")
}

group = "io.curity"
version = "0.0.1-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux:3.1.1")
	implementation("org.springframework.session:spring-session-core:3.1.1")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.1")
	implementation("org.bitbucket.b_c:jose4j:0.9.2")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")
	implementation("org.apache.commons:commons-crypto:1.1.0")

	implementation("org.apache.groovy:groovy:4.0.7")
	implementation("org.apache.groovy:groovy-json:4.0.7")
	testImplementation("org.springframework.boot:spring-boot-starter-test:3.1.1")
	testImplementation("org.spockframework:spock-spring:2.3-groovy-4.0")
	testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.0")
	testImplementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
		incremental = false
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	jvmArgs = listOf(
		"-Dsun.net.http.allowRestrictedHeaders=true"
	)
	include("**/*Spec.class")
	testLogging.showStandardStreams = false
}

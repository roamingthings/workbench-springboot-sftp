import org.gradle.api.JavaVersion.VERSION_1_8
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	val kotlinVersion = "1.3.31"

	kotlin("jvm") version kotlinVersion
	id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
	id("org.springframework.boot") version "2.1.4.RELEASE"
	id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
	id("com.avast.gradle.docker-compose") version "0.9.3"
}

apply(plugin="io.spring.dependency-management")
apply(plugin="docker-compose")


group = "de.roamingthings"
version = "0.1-SNAPSHOT"

val developmentOnly: Configuration by configurations.creating

configurations {
	runtimeClasspath {
		extendsFrom(configurations["developmentOnly"])
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(kotlin("stdlib-jdk8"))
	implementation(kotlin("reflect"))

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-batch")
	implementation("org.springframework.boot:spring-boot-starter-integration")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-batch")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.integration:spring-integration-jdbc")
	implementation("org.springframework.integration:spring-integration-sftp")

	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.testcontainers:testcontainers:1.11.2")
	testImplementation("org.testcontainers:junit-jupiter:1.11.2")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
	testImplementation("org.junit.jupiter:junit-jupiter-api")
	testImplementation("org.springframework.batch:spring-batch-test")
	testImplementation("org.assertj:assertj-core")
	testImplementation("org.mockito:mockito-core")
	testImplementation("org.mockito:mockito-junit-jupiter")
	testImplementation("org.apache.sshd:sshd-sftp:2.2.0")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
}

configure<JavaPluginConvention> {
	sourceCompatibility = VERSION_1_8
	targetCompatibility = VERSION_1_8
}

tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "1.8"
	kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
}

dockerCompose {
	dockerComposeWorkingDirectory = "${projectDir}/sftp-server"
}

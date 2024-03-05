val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val hikaricp_version: String by project
val ehcache_version: String by project
val jwt_version: String by project

plugins {
  kotlin("jvm") version "1.9.20"
  id("io.ktor.plugin") version "2.3.7"
  kotlin("plugin.serialization") version "1.9.20"
}

version = "1.0"
group = "com.roughlyunderscore"

application {
  mainClass = "com.roughlyunderscore.CHBackendKt"
  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.register<JavaExec>("runFileScope") {
  mainClass = "com.roughlyunderscore.CHBackendKt"
  classpath = sourceSets["main"].runtimeClasspath
}

repositories {
  mavenCentral()
  mavenLocal()
  google()
}

dependencies {
  // Ktor stuff
  implementation("io.ktor:ktor-server-call-logging-jvm")
  implementation("io.ktor:ktor-server-content-negotiation-jvm")
  implementation("io.ktor:ktor-server-core-jvm")
  implementation("io.ktor:ktor-serialization-gson-jvm")
  implementation("io.ktor:ktor-server-rate-limit")
  implementation("io.ktor:ktor-server-status-pages")

  // Dotenv
  implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

  // Database
  implementation("com.zaxxer:HikariCP:$hikaricp_version")
  implementation("org.ehcache:ehcache:$ehcache_version")
  implementation("org.mariadb.jdbc:mariadb-java-client:3.3.2")

  // JWT
  implementation("io.jsonwebtoken:jjwt-api:$jwt_version")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwt_version")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwt_version")

  // kotlinx serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

  // Email & password validation
  implementation("com.roughlyunderscore:chback-safe:1.0-SNAPSHOT")

  // More ktor stuff
  implementation("io.ktor:ktor-client-core-jvm")
  implementation("io.ktor:ktor-client-apache-jvm")
  implementation("io.ktor:ktor-server-netty-jvm")
  implementation("ch.qos.logback:logback-classic:$logback_version")
  testImplementation("io.ktor:ktor-server-tests-jvm")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

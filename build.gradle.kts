plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "org.learn.minecraftmap"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    
    // JNA for calling compiled native C libraries (Cubiomes)
    implementation("net.java.dev.jna:jna:5.14.0")

    // Apache Commons Pool 2 for native object pooling
    implementation("org.apache.commons:commons-pool2:2.12.0")

    // Caffeine Cache for metatiling cache in memory
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
plugins {
    `maven-publish`
    id("java")
}

group = "com.koshakmine.virtualinventory"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.lumi.su/snapshots")
}

dependencies {
    implementation("com.koshakmine:Lumi:1.6.7-SNAPSHOT")
    implementation("org.jetbrains:annotations:26.0.2")
    implementation("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.withType<ProcessResources> {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name.lowercase()
            version = project.version.toString()
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "lumi"
            url = uri("https://repo.lumi.su/releases")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

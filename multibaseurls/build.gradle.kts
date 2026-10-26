plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(platform(libs.okhttp.bom))
    api(libs.okhttp)
    api(libs.retrofit)

    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver3")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            // Override via gradle.properties: mavenGroupId / mavenVersion
            groupId = providers.gradleProperty("mavenGroupId").getOrElse("io.github.didahzy")
            artifactId = "multibaseurls"
            version = providers.gradleProperty("mavenVersion").getOrElse("0.1.0")

            pom {
                name = "MultiBaseUrls"
                description = "A lightweight baseUrl-switching library for Retrofit, driven by annotations and an OkHttp interceptor."
                url = "https://github.com/didahzy/MultiBaseUrls"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "didahzy"
                        name = "Huang Ziyao"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/didahzy/MultiBaseUrls.git"
                    developerConnection = "scm:git:ssh://github.com/didahzy/MultiBaseUrls.git"
                    url = "https://github.com/didahzy/MultiBaseUrls"
                }
            }
        }
    }

    repositories {
        // Optional remote repository (e.g. Maven Central / Nexus). Credentials live in
        // ~/.gradle/gradle.properties:
        //   mavenReleasesUrl=https://...
        //   mavenUsername=...
        //   mavenPassword=...
        // Without these properties, `publishToMavenLocal` still works.
        if (providers.gradleProperty("mavenReleasesUrl").isPresent) {
            maven {
                name = "remote"
                url = uri(providers.gradleProperty("mavenReleasesUrl").get())
                credentials {
                    username = providers.gradleProperty("mavenUsername").getOrElse("")
                    password = providers.gradleProperty("mavenPassword").getOrElse("")
                }
            }
        }
    }
}

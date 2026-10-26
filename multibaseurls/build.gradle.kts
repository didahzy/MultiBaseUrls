import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.vanniktech.maven.publish)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// Override via gradle.properties: mavenGroupId / mavenVersion
group = providers.gradleProperty("mavenGroupId").getOrElse("io.github.didahzy")
version = providers.gradleProperty("mavenVersion").getOrElse("0.1.0")

dependencies {
    api(platform(libs.okhttp.bom))
    api(libs.okhttp)
    api(libs.retrofit)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver3)
}

mavenPublishing {
    configure(KotlinJvm(javadocJar = JavadocJar.Javadoc(), sourcesJar = SourcesJar.Sources()))

    // Publishes to the Central Portal; credentials come from
    // ~/.gradle/gradle.properties: mavenCentralUsername / mavenCentralPassword (user token).
    // Without them, `publishToMavenLocal` still works. Deployment lands in the Portal
    // for manual release; pass automaticRelease = true to publish without visiting the UI.
    publishToMavenCentral()

    // Requires GPG config in ~/.gradle/gradle.properties:
    //   signing.keyId / signing.password / signing.secretKeyRingFile
    signAllPublications()

    pom {
        name = "MultiBaseUrls"
        description = "A lightweight baseUrl-switching library for Retrofit, driven by annotations and an OkHttp interceptor."
        inceptionYear = "2026"
        url = "https://github.com/didahzy/MultiBaseUrls/"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer {
                id = "didahzy"
                name = "Huang Ziyao"
                url = "https://github.com/didahzy/"
            }
        }
        scm {
            url = "https://github.com/didahzy/MultiBaseUrls/"
            connection = "scm:git:git://github.com/didahzy/MultiBaseUrls.git"
            developerConnection = "scm:git:ssh://git@github.com/didahzy/MultiBaseUrls.git"
        }
    }
}

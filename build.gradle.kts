buildscript {
    configurations.classpath {
        resolutionStrategy.eachDependency {
            when {
                requested.group == "org.bitbucket.b_c" && requested.name == "jose4j" -> useVersion("0.9.6")
                requested.group == "org.bouncycastle" && requested.name.endsWith("-jdk18on") -> useVersion("1.84")
                requested.group == "org.jdom" && requested.name == "jdom2" -> useVersion("2.0.6.1")
            }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.stability.analyzer) apply false
    alias(libs.plugins.owasp.dependency.check) apply false
    alias(libs.plugins.sonarqube)
}

val sonarProjectProperties =
    java.util.Properties().apply {
        val propertiesFile = rootProject.file("sonar-project.properties")
        if (propertiesFile.isFile) {
            propertiesFile.inputStream().use(::load)
        }
    }

sonar {
    properties {
        sonarProjectProperties.forEach { key, value ->
            property(key.toString(), value.toString())
        }
    }
}

project(":app") {
    sonar {
        properties {
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                layout.buildDirectory
                    .file("reports/coverage/test/debug/report.xml")
                    .get()
                    .asFile
                    .absolutePath,
            )
            property(
                "sonar.coverage.exclusions",
                listOf(
                    "src/main/java/com/finnvek/squaretool/MainActivity.kt",
                    "src/main/java/com/finnvek/squaretool/app/**",
                    "src/main/java/com/finnvek/squaretool/export/ProjectPdfExporter.kt",
                    "src/main/java/com/finnvek/squaretool/export/ProjectPngExporter.kt",
                    "src/main/java/com/finnvek/squaretool/render/**",
                    "src/main/java/com/finnvek/squaretool/ui/*.kt",
                    "src/main/java/com/finnvek/squaretool/ui/**/*Screen.kt",
                    "src/main/java/com/finnvek/squaretool/ui/**/*ViewModel*.kt",
                    "src/main/java/com/finnvek/squaretool/ui/navigation/**",
                    "src/main/java/com/finnvek/squaretool/ui/theme/**",
                ),
            )
        }
    }
}

tasks.named("sonar") {
    dependsOn(":app:assembleDebug", ":app:createDebugUnitTestCoverageReport")
}

allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }

    configurations.configureEach {
        resolutionStrategy.eachDependency {
            when {
                requested.group == "ch.qos.logback" -> useVersion("1.5.34")
                requested.group == "io.netty" && requested.version?.startsWith("4.1.") == true ->
                    useVersion("4.1.137.Final")
                requested.group == "org.apache.commons" && requested.name == "commons-lang3" ->
                    useVersion("3.20.0")
                requested.group == "org.apache.httpcomponents" && requested.name == "httpclient" ->
                    useVersion("4.5.14")
                requested.group == "org.bouncycastle" && requested.name.endsWith("-jdk18on") ->
                    useVersion("1.84")
            }
        }
    }
}

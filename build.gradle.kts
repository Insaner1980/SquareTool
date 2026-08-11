plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.owasp.dependency.check) apply false
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
                    useVersion("4.1.136.Final")
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

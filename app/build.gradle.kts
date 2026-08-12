import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.apollo)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

fun saleorProperty(key: String, default: String): String {
    return localProperties.getProperty(key)
        ?: providers.gradleProperty(key).orNull
        ?: default
}

val saleorApiUrl = saleorProperty(
    "saleor.api.url",
    "https://saleor-api.klms.co.kr/graphql/",
)
val saleorChannel = saleorProperty("saleor.channel", "kr")
val saleorLocale = saleorProperty("saleor.locale", "ko")
val saleorCheckoutCountry = saleorProperty("saleor.checkout.country", "KR")
val saleorFeaturedCollection = saleorProperty(
    "saleor.featured.collection",
    "featured-products",
)

android {
    namespace = "com.bdf.saleor"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.bdf.saleor"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SALEOR_API_URL", "\"$saleorApiUrl\"")
        buildConfigField("String", "SALEOR_CHANNEL", "\"$saleorChannel\"")
        buildConfigField("String", "SALEOR_LOCALE", "\"$saleorLocale\"")
        buildConfigField("String", "SALEOR_CHECKOUT_COUNTRY", "\"$saleorCheckoutCountry\"")
        buildConfigField(
            "String",
            "FEATURED_COLLECTION_SLUG",
            "\"$saleorFeaturedCollection\"",
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

apollo {
    service("service") {
        packageName.set("com.bdf.saleor.graphql")
        introspection {
            endpointUrl.set(saleorApiUrl)
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.apollo.runtime)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

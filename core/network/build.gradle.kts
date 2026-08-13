import java.util.Properties

plugins {
    id("saleor.android.library")
    id("saleor.android.hilt")
    alias(libs.plugins.apollo)
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
val saleorStorefrontUrl = saleorProperty(
    "saleor.storefront.url",
    "https://saleor.klms.co.kr",
)

android {
    namespace = "com.bdf.saleor.core.network"
    defaultConfig {
        buildConfigField("String", "SALEOR_API_URL", "\"$saleorApiUrl\"")
        buildConfigField("String", "SALEOR_CHANNEL", "\"$saleorChannel\"")
        buildConfigField("String", "SALEOR_LOCALE", "\"$saleorLocale\"")
        buildConfigField("String", "SALEOR_CHECKOUT_COUNTRY", "\"$saleorCheckoutCountry\"")
        buildConfigField("String", "FEATURED_COLLECTION_SLUG", "\"$saleorFeaturedCollection\"")
        buildConfigField("String", "SALEOR_STOREFRONT_URL", "\"$saleorStorefrontUrl\"")
    }
    buildFeatures {
        buildConfig = true
    }
}

apollo {
    service("service") {
        packageName.set("com.bdf.saleor.graphql")
        mapScalar("JSON", "kotlin.Any")
        introspection {
            endpointUrl.set(saleorApiUrl)
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    api(libs.apollo.runtime)
    api(libs.apollo.normalized.cache)
    api(libs.apollo.normalized.cache.sqlite)
    implementation(libs.kotlinx.coroutines.android)
}

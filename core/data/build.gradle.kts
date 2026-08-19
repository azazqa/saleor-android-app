plugins {
    id("saleor.android.library")
    id("saleor.android.hilt")
}

android {
    namespace = "com.bdf.saleor.core.data"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

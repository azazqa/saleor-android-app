plugins {
    id("saleor.android.library")
    id("saleor.android.hilt")
}

android {
    namespace = "com.bdf.saleor.core.datastore"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}

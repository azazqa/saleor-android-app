plugins {
    id("saleor.android.feature")
}

android {
    namespace = "com.bdf.saleor.feature.catalog"
}

dependencies {
    testImplementation(libs.androidx.lifecycle.runtime.ktx)
}

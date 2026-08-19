plugins {
    id("saleor.android.feature")
}

android {
    namespace = "com.bdf.saleor.feature.checkout"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.webkit)
    implementation(libs.tosspayments.android)
    testImplementation(libs.androidx.lifecycle.runtime.ktx)
}

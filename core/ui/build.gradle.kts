plugins {
    id("saleor.android.library.compose")
}

android {
    namespace = "com.bdf.saleor.core.ui"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)
}

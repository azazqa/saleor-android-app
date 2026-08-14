plugins {
    id("saleor.android.library.compose")
    id("saleor.android.hilt")
}

android {
    namespace = "com.bdf.saleor.feature.checkout"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.webkit)
    implementation(libs.coil.compose)
    implementation(libs.tosspayments.android)
    testImplementation(project(":core:testing"))
    testImplementation(libs.androidx.lifecycle.runtime.ktx)
}

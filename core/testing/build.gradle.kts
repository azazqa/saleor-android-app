plugins {
    id("saleor.android.library")
}

android {
    namespace = "com.bdf.saleor.core.testing"
}

dependencies {
    api(project(":core:data"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}

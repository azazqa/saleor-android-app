plugins {
    `kotlin-dsl`
}

group = "com.bdf.saleor.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "saleor.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "saleor.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "saleor.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidApplication") {
            id = "saleor.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "saleor.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
    }
}

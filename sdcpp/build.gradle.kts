plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.druk.sdcpp"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {
                arguments += "-DCMAKE_BUILD_TYPE=Release"
                arguments += "-DSD_BUILD_EXAMPLES=OFF"
                arguments += "-DSD_BUILD_SHARED_LIBS=ON"
                arguments += "-DSD_BUILD_SHARED_GGML_LIB=OFF"
                arguments += "-DSD_USE_UPSTREAM_GGML=ON"
                arguments += "-DSD_WEBP=OFF"
                arguments += "-DSD_WEBM=OFF"
                arguments += "-DGGML_BACKEND_DL=OFF"
                arguments += "-DGGML_OPENMP=OFF"
                arguments += "-DGGML_VULKAN=OFF"
                arguments += "-DGGML_LLAMAFILE=OFF"
                arguments += "-DGGML_NATIVE=OFF"
                arguments += "-DGGML_CPU_ALL_VARIANTS=OFF"
                arguments += "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
                cFlags += "-fvisibility=hidden"
                cppFlags += "-fvisibility=hidden"
            }
        }
        ndk {
            abiFilters += setOf("arm64-v8a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
}

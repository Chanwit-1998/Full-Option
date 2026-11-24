cmake_minimum_required(VERSION 3.31.6)
project("sdcpp")

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

set(SD_GGML_SOURCE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../core/llama.cpp/ggml" CACHE PATH "upstream ggml dir" FORCE)
set(SD_USE_UPSTREAM_GGML ON CACHE BOOL "" FORCE)
set(SD_BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
set(SD_BUILD_SHARED_GGML_LIB OFF CACHE BOOL "" FORCE)
set(SD_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(SD_WEBP OFF CACHE BOOL "" FORCE)
set(SD_WEBM OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_CPU_ALL_VARIANTS OFF CACHE BOOL "" FORCE)

add_subdirectory(stable-diffusion.cpp)

add_library(sdcpp SHARED sd-jni.cpp)

target_include_directories(sdcpp PRIVATE
    stable-diffusion.cpp/include
    stable-diffusion.cpp/thirdparty
)

target_link_libraries(sdcpp PRIVATE
    stable-diffusion
    android
    log
)

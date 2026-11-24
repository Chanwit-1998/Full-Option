#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <exception>
#include <mutex>
#include <string>

#include "stable-diffusion.h"

#define STB_IMAGE_WRITE_STATIC
#define STB_IMAGE_WRITE_IMPLEMENTATION
#include "stb_image_write.h"

#define LOG_TAG "SDCPP_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

std::mutex g_sd_mutex;
sd_ctx_t* g_sd_ctx = nullptr;
std::string g_loaded_key;
std::string g_last_error;
std::atomic<bool> g_cancel_requested{false};

std::string trim_msg(const char* text) {
    std::string s = text ? text : "";
    while (!s.empty() && (s.back() == '\n' || s.back() == '\r' || s.back() == ' ')) {
        s.pop_back();
    }
    return s;
}

void sd_android_log_cb(enum sd_log_level_t level, const char* text, void* /*data*/) {
    if (!text) return;
    if (level == SD_LOG_ERROR) {
        LOGE("%s", text);
        std::string msg = trim_msg(text);
        if (!msg.empty()) {
            g_last_error = msg;
        }
    } else if (level == SD_LOG_WARN) {
        LOGW("%s", text);
    } else {
        LOGI("%s", text);
    }
}

void free_context_locked() {
    if (g_sd_ctx != nullptr) {
        LOGI("Freeing stable-diffusion context (%s)", g_loaded_key.c_str());
        free_sd_ctx(g_sd_ctx);
        g_sd_ctx = nullptr;
    }
    g_loaded_key.clear();
}

}  // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_druk_sdcpp_StableDiffusionEngine_nativeUnload(JNIEnv* /*env*/, jobject /*thiz*/) {
    g_cancel_requested.store(true);
    std::lock_guard<std::mutex> lock(g_sd_mutex);
    free_context_locked();
}

extern "C" JNIEXPORT void JNICALL
Java_com_druk_sdcpp_StableDiffusionEngine_nativeCancel(JNIEnv* /*env*/, jobject /*thiz*/) {
    g_cancel_requested.store(true);
    if (g_sd_ctx != nullptr) {
        sd_cancel_generation(g_sd_ctx, SD_CANCEL_ALL);
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_druk_sdcpp_StableDiffusionEngine_nativeGeneratePng(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring jModelPath,
    jstring jModelKey,
    jstring jPrompt,
    jstring jNegativePrompt,
    jstring jOutputPngPath,
    jint width,
    jint height,
    jint steps,
    jfloat cfgScale,
    jlong seed,
    jint threads
) {
    if (!jModelPath || !jPrompt || !jOutputPngPath) {
        return env->NewStringUTF("Invalid null arguments passed to nativeGeneratePng");
    }

    const char* cModelPath = env->GetStringUTFChars(jModelPath, nullptr);
    const char* cModelKey = jModelKey ? env->GetStringUTFChars(jModelKey, nullptr) : cModelPath;
    const char* cPrompt = env->GetStringUTFChars(jPrompt, nullptr);
    const char* cNegPrompt = jNegativePrompt ? env->GetStringUTFChars(jNegativePrompt, nullptr) : "";
    const char* cOutPath = env->GetStringUTFChars(jOutputPngPath, nullptr);

    std::string modelPath(cModelPath);
    std::string modelKey(cModelKey);
    std::string prompt(cPrompt);
    std::string negPrompt(cNegPrompt);
    std::string outPath(cOutPath);

    env->ReleaseStringUTFChars(jModelPath, cModelPath);
    if (jModelKey) env->ReleaseStringUTFChars(jModelKey, cModelKey);
    env->ReleaseStringUTFChars(jPrompt, cPrompt);
    if (jNegativePrompt) env->ReleaseStringUTFChars(jNegativePrompt, cNegPrompt);
    env->ReleaseStringUTFChars(jOutputPngPath, cOutPath);

    std::lock_guard<std::mutex> lock(g_sd_mutex);
    g_cancel_requested.store(false);
    g_last_error.clear();
    sd_set_log_callback(sd_android_log_cb, nullptr);

    LOGI("nativeGeneratePng start: path=%s key=%s w=%d h=%d steps=%d cfg=%.2f threads=%d out=%s",
         modelPath.c_str(), modelKey.c_str(), width, height, steps, cfgScale, threads, outPath.c_str());

    try {
        if (g_sd_ctx == nullptr || g_loaded_key != modelKey) {
            free_context_locked();
            sd_ctx_params_t ctx_params;
            sd_ctx_params_init(&ctx_params);
            ctx_params.model_path = modelPath.c_str();
            ctx_params.n_threads = threads > 0 ? threads : 4;
            ctx_params.enable_mmap = true;
            ctx_params.vae_conv_direct = true;
            ctx_params.diffusion_conv_direct = true;

            g_sd_ctx = new_sd_ctx(&ctx_params);
            if (g_sd_ctx == nullptr) {
                std::string err = g_last_error.empty() ? "new_sd_ctx returned null" : g_last_error;
                LOGE("new_sd_ctx failed: %s", err.c_str());
                return env->NewStringUTF(err.c_str());
            }
            g_loaded_key = modelKey;
        }

        if (g_cancel_requested.load()) {
            return env->NewStringUTF("Generation cancelled");
        }
        sd_cancel_generation(g_sd_ctx, SD_CANCEL_RESET);

        sd_img_gen_params_t gen_params;
        sd_img_gen_params_init(&gen_params);
        gen_params.prompt = prompt.c_str();
        gen_params.negative_prompt = negPrompt.c_str();
        gen_params.width = width > 0 ? width : 512;
        gen_params.height = height > 0 ? height : 512;
        gen_params.batch_count = 1;
        gen_params.seed = seed >= 0 ? static_cast<int64_t>(seed) : static_cast<int64_t>(std::time(nullptr));
        gen_params.sample_params.sample_steps = steps > 0 ? steps : 1;
        gen_params.sample_params.guidance.txt_cfg = cfgScale >= 0.0f ? cfgScale : 1.0f;
        gen_params.sample_params.sample_method = sd_get_default_sample_method(g_sd_ctx);
        gen_params.sample_params.scheduler = sd_get_default_scheduler(g_sd_ctx, gen_params.sample_params.sample_method);

        sd_image_generation_result_t* result = nullptr;
        bool ok = generate_image(g_sd_ctx, &gen_params, &result);
        if (!ok || result == nullptr || result->count <= 0 || result->items[0].image.data == nullptr) {
            std::string err = g_last_error.empty() ? "generate_image returned false" : g_last_error;
            LOGE("generate_image failed: %s", err.c_str());
            if (result != nullptr) free_sd_image_generation_result(result);
            return env->NewStringUTF(err.c_str());
        }

        const sd_image_t& img = result->items[0].image;
        LOGI("Image generated in memory: %ux%u ch=%u -> writing %s",
             img.width, img.height, img.channel, outPath.c_str());

        int wrote = stbi_write_png(
            outPath.c_str(),
            static_cast<int>(img.width),
            static_cast<int>(img.height),
            static_cast<int>(img.channel),
            img.data,
            static_cast<int>(img.width * img.channel)
        );
        free_sd_image_generation_result(result);
        if (wrote == 0) {
            return env->NewStringUTF("stbi_write_png failed");
        }
        return nullptr;
    } catch (const std::exception& e) {
        LOGE("Exception in nativeGeneratePng: %s", e.what());
        return env->NewStringUTF(e.what());
    } catch (...) {
        LOGE("Unknown exception in nativeGeneratePng");
        return env->NewStringUTF("Unknown native exception");
    }
}

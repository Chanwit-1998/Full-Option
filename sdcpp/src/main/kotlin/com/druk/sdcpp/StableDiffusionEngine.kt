package com.druk.sdcpp

import java.io.File

object StableDiffusionEngine {

    init {
        System.loadLibrary("sdcpp")
    }

    data class GenPreset(
        val width: Int,
        val height: Int,
        val steps: Int,
        val cfgScale: Float,
    )

    fun presetForModel(filename: String): GenPreset {
        val lower = filename.lowercase()
        val isFastDistilled = lower.contains("sdxs") ||
            lower.contains("turbo") ||
            lower.contains("lightning") ||
            lower.contains("lcm") ||
            lower.contains("hyper")
        return if (isFastDistilled) {
            GenPreset(width = 512, height = 512, steps = 1, cfgScale = 1.0f)
        } else {
            GenPreset(width = 512, height = 512, steps = 12, cfgScale = 7.0f)
        }
    }

    /**
     * Generates a PNG image using a real filesystem [modelFile].
     * Returns `null` on success, or an error description on failure.
     */
    fun generatePng(
        modelFile: File,
        modelKey: String,
        prompt: String,
        outputFile: File,
        negativePrompt: String = "blurry, low quality, distorted, watermark, text",
        preset: GenPreset = presetForModel(modelKey),
        seed: Long = -1L,
        threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(2, 6),
    ): String? {
        outputFile.parentFile?.mkdirs()
        return nativeGeneratePng(
            modelPath = modelFile.absolutePath,
            modelKey = modelKey,
            prompt = prompt,
            negativePrompt = negativePrompt,
            outputPngPath = outputFile.absolutePath,
            width = preset.width,
            height = preset.height,
            steps = preset.steps,
            cfgScale = preset.cfgScale,
            seed = seed,
            threads = threads,
        )
    }

    fun cancel() = nativeCancel()
    fun unload() = nativeUnload()

    private external fun nativeGeneratePng(
        modelPath: String,
        modelKey: String,
        prompt: String,
        negativePrompt: String,
        outputPngPath: String,
        width: Int,
        height: Int,
        steps: Int,
        cfgScale: Float,
        seed: Long,
        threads: Int,
    ): String?

    private external fun nativeCancel()
    private external fun nativeUnload()
}

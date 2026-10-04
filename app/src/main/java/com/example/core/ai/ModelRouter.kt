package com.example.core.ai

import com.example.core.model.ModelConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized Model Router.
 * All model names and parameters are managed here.
 * Absolutely NO hardcoded model names scattered across other classes.
 */
class ModelRouter {
    companion object {
        const val DEFAULT_MAIN_MODEL = "openai/gpt-oss-120b"
        const val DEFAULT_VISION_MODEL = "qwen/qwen3.8-27b"
        const val DEFAULT_STT_MODEL = "whisper-large-v3-turbo"
        const val DEFAULT_TTS_VOICE = "en-US-Neural2-F"

        val AVAILABLE_MAIN_MODELS = listOf(
            "openai/gpt-oss-120b",
            "llama-3.3-70b-versatile",
            "mixtral-8x7b-32768"
        )

        val AVAILABLE_VISION_MODELS = listOf(
            "qwen/qwen3.8-27b",
            "llama-3.2-11b-vision-preview"
        )

        val AVAILABLE_STT_MODELS = listOf(
            "whisper-large-v3-turbo",
            "whisper-large-v3"
        )
    }

    private val _config = MutableStateFlow(ModelConfiguration())
    val config: StateFlow<ModelConfiguration> = _config.asStateFlow()

    fun updateMainModel(model: String) {
        _config.value = _config.value.copy(mainModel = model)
    }

    fun updateVisionModel(model: String) {
        _config.value = _config.value.copy(visionModel = model)
    }

    fun updateSttModel(model: String) {
        _config.value = _config.value.copy(sttModel = model)
    }

    fun updateTtsVoice(voice: String) {
        _config.value = _config.value.copy(ttsVoice = voice)
    }

    fun setAutoVerification(enabled: Boolean) {
        _config.value = _config.value.copy(isAutoVerificationEnabled = enabled)
    }

    fun setMemoryEnabled(enabled: Boolean) {
        _config.value = _config.value.copy(isMemoryEnabled = enabled)
    }

    fun getMainModel(): String = _config.value.mainModel
    fun getVisionModel(): String = _config.value.visionModel
    fun getSttModel(): String = _config.value.sttModel
}

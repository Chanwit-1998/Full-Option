package com.druk.ai_offline_llm.assistant

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AssistantBridge {
    private val _pendingPrompt = MutableStateFlow<String?>(null)
    val pendingPrompt: StateFlow<String?> = _pendingPrompt.asStateFlow()

    fun submitPrompt(prompt: String) {
        val clean = prompt.trim()
        if (clean.isNotEmpty()) {
            _pendingPrompt.value = clean
        }
    }

    fun consumePrompt(): String? {
        val current = _pendingPrompt.value
        _pendingPrompt.value = null
        return current
    }
}

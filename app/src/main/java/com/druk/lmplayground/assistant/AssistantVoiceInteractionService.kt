package com.druk.ai_offline_llm.assistant

import android.service.voice.VoiceInteractionService

class AssistantVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
    }

    override fun onGetSupportedVoiceActions(voiceActions: Set<String>): Set<String> {
        return voiceActions
    }
}

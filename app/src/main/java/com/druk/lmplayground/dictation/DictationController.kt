package com.druk.ai_offline_llm.dictation

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.druk.ai_offline_llm.R
import com.druk.ai_offline_llm.download.DownloadRepository
import com.druk.ai_offline_llm.storage.StorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DictationState {
    data object Idle : DictationState
    data class Listening(val text: String) : DictationState
    data object Finishing : DictationState
}

class DictationController(
    private val app: Application,
    private val manager: DictationManager,
    private val storageRepository: StorageRepository,
    private val scope: CoroutineScope,
) {
    private val downloads by lazy { DownloadRepository(app) }

    val downloadProgress: LiveData<Float?> by lazy {
        downloads.observeDownloads().map { it[modelInfo.name]?.progress }
    }

    private val _state = MutableLiveData<DictationState>(DictationState.Idle)
    val state: LiveData<DictationState> = _state

    private val _isModelReady = MutableLiveData<Boolean?>(true)
    val isModelReady: LiveData<Boolean?> = _isModelReady

    private val _transcript = MutableLiveData<String?>(null)
    val transcript: LiveData<String?> = _transcript

    private val _micOn = MutableLiveData(false)
    val micOn: LiveData<Boolean> = _micOn

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _recording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    val modelInfo = manager.modelInfo

    private val main = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var lastPartialText: String = ""
    private var sessionStartMs: Long = 0L
    private var isRestarting: Boolean = false

    @Volatile var continuousMode: Boolean = false
    @Volatile private var held = false

    fun refreshModelAvailability() {
        scope.launch { isModelAvailable() }
    }

    suspend fun isModelAvailable(): Boolean {
        val available = withContext(Dispatchers.Main) {
            SpeechRecognizer.isRecognitionAvailable(app)
        } || withContext(Dispatchers.IO) {
            manager.isModelOnDisk()
        }
        _isModelReady.postValue(available)
        return available
    }

    fun downloadModel() {
        val storageUri = storageRepository.getStorageUri()
        if (storageUri == null) {
            _error.value = app.getString(R.string.dictation_storage_not_configured)
            return
        }
        downloads.startDownload(manager.modelInfo, storageUri)
    }

    val isMicOn: Boolean get() = _micOn.value == true

    fun onMicPressed() {
        held = true
        sessionStartMs = SystemClock.elapsedRealtime()
        isRestarting = false
        _micOn.value = true
    }

    fun onMicReleased() {
        held = false
        isRestarting = false
        _micOn.value = false
        stopListening()
    }

    private fun shouldKeepListeningOnSilence(): Boolean {
        if (!held) return false
        if (continuousMode) return true
        val elapsed = SystemClock.elapsedRealtime() - sessionStartMs
        return elapsed < NORMAL_IDLE_WINDOW_MS
    }

    private fun scheduleRestartListening() {
        isRestarting = true
        destroyRecognizerLocked()
        _recording.value = false
        _level.value = 0f
        if (held) {
            _micOn.value = true
            _state.value = DictationState.Listening(lastPartialText)
            main.postDelayed({
                if (held) startListening()
            }, 120L)
        }
    }

    fun startListening() {
        runOnMain {
            if (!held) return@runOnMain

            val playStartTone = !isRestarting
            destroyRecognizerLocked()
            if (!isRestarting) {
                lastPartialText = ""
            }
            _state.value = DictationState.Listening(lastPartialText)

            try {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(app)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _recording.value = true
                        isRestarting = false
                        if (playStartTone) {
                            DictationEarcon.playStart()
                        }
                    }

                    override fun onBeginningOfSpeech() = Unit

                    override fun onRmsChanged(rmsdB: Float) {
                        _level.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        _recording.value = false
                        _level.value = 0f
                        if (held && lastPartialText.isNotBlank()) {
                            _state.value = DictationState.Finishing
                        }
                    }

                    override fun onError(errorCode: Int) {
                        Log.w(TAG, "SpeechRecognizer error: $errorCode")
                        val rescuedText = lastPartialText.trim()

                        if (rescuedText.isNotEmpty()) {
                            lastPartialText = ""
                            isRestarting = false
                            destroyRecognizerLocked()
                            _recording.value = false
                            _level.value = 0f
                            _micOn.value = false
                            held = false
                            DictationEarcon.playStop()
                            _transcript.value = rescuedText
                            _state.value = DictationState.Idle
                            return
                        }

                        val isSilenceOrTimeout =
                            errorCode == SpeechRecognizer.ERROR_NO_MATCH ||
                            errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                            errorCode == SpeechRecognizer.ERROR_CLIENT

                        if (isSilenceOrTimeout && shouldKeepListeningOnSilence()) {
                            scheduleRestartListening()
                            return
                        }

                        lastPartialText = ""
                        isRestarting = false
                        destroyRecognizerLocked()
                        _recording.value = false
                        _level.value = 0f
                        _micOn.value = false
                        held = false
                        DictationEarcon.playStop()
                        _state.value = DictationState.Idle

                        if (!continuousMode) {
                            when (errorCode) {
                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                    _error.value = "ไม่ได้ยินเสียงพูด ลองใหม่อีกครั้ง"
                                }
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                    _error.value = app.getString(R.string.dictation_permission_denied)
                                }
                                else -> {
                                    _error.value = "ไม่สามารถเชื่อมต่อบริการแปลงเสียงได้ (รหัส $errorCode)"
                                }
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val finalText = matches?.firstOrNull()?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: lastPartialText.trim()

                        if (finalText.isEmpty() && shouldKeepListeningOnSilence()) {
                            scheduleRestartListening()
                            return
                        }

                        lastPartialText = ""
                        isRestarting = false
                        destroyRecognizerLocked()
                        _recording.value = false
                        _level.value = 0f
                        _micOn.value = false
                        held = false
                        DictationEarcon.playStop()

                        if (finalText.isNotEmpty()) {
                            _transcript.value = finalText
                        }
                        _state.value = DictationState.Idle
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        if (partial.isNotBlank()) {
                            lastPartialText = partial
                            _state.value = DictationState.Listening(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "th-TH")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 6000L)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, app.packageName)
                }
                recognizer.startListening(intent)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to start SpeechRecognizer", t)
                destroyRecognizerLocked()
                held = false
                isRestarting = false
                _micOn.value = false
                _recording.value = false
                _level.value = 0f
                _state.value = DictationState.Idle
                if (!continuousMode) {
                    _error.value = app.getString(R.string.dictation_failed)
                }
            }
        }
    }

    fun stopListening() {
        runOnMain {
            held = false
            isRestarting = false
            val recognizer = speechRecognizer
            if (recognizer != null) {
                _state.value = DictationState.Finishing
                runCatching { recognizer.stopListening() }
            } else {
                _recording.value = false
                _level.value = 0f
                _state.value = DictationState.Idle
            }
        }
    }

    fun abandon() {
        runOnMain {
            held = false
            isRestarting = false
            lastPartialText = ""
            _micOn.value = false
            _recording.value = false
            _level.value = 0f
            destroyRecognizerLocked()
            _state.value = DictationState.Idle
        }
    }

    private fun destroyRecognizerLocked() {
        val r = speechRecognizer
        speechRecognizer = null
        if (r != null) {
            runCatching { r.cancel() }
            runCatching { r.destroy() }
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            main.post(block)
        }
    }

    fun consumeTranscript() {
        _transcript.value = null
    }

    fun consumeError() {
        _error.value = null
    }

    companion object {
        private const val TAG = "DictationController"
        private const val NORMAL_IDLE_WINDOW_MS = 25_000L
    }
}

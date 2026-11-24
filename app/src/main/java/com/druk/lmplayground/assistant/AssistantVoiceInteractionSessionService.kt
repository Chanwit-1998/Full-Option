package com.druk.ai_offline_llm.assistant

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.druk.ai_offline_llm.MainActivity
import com.druk.ai_offline_llm.cartridge.CartridgeManager

class AssistantVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return AssistantVoiceInteractionSession(this)
    }
}

class AssistantVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var pulseAnimator: ObjectAnimator? = null
    private var isListening = false
    private var isTextMode = false

    private var transcriptLabel: TextView? = null
    private var orbButton: TextView? = null
    private var inputRow: LinearLayout? = null
    private var promptInput: EditText? = null

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()

    override fun onCreateContentView(): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { hide() }
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F2111318"))
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), Color.parseColor("#33FFFFFF"))
            }
            setPadding(dp(18), dp(14), dp(18), dp(16))
        }

        val activeCart = CartridgeManager.getActiveCartridge(context)
        val badgeText = TextView(context).apply {
            text = if (activeCart != null) {
                "● 100% OFFLINE  ·  ตลับ: ${activeCart.name}"
            } else {
                "● 100% OFFLINE ASSISTANT"
            }
            setTextColor(if (activeCart != null) Color.parseColor("#4ADE80") else Color.parseColor("#93C5FD"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        card.addView(badgeText)

        val transcriptView = TextView(context).apply {
            text = "แตะวงกลมเพื่อพูด หรือกด + เพื่อพิมพ์ถาม..."
            setTextColor(Color.parseColor("#E5E7EB"))
            textSize = 14f
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(8), 0, dp(10))
        }
        transcriptLabel = transcriptView
        card.addView(transcriptView)

        val textInputContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(0, 0, 0, dp(10))
        }

        val edit = EditText(context).apply {
            hint = "พิมพ์คำถามแบบออฟไลน์..."
            setHintTextColor(Color.parseColor("#9CA3AF"))
            setTextColor(Color.WHITE)
            textSize = 14f
            imeOptions = EditorInfo.IME_ACTION_SEND
            setSingleLine(true)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#22FFFFFF"))
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendToChatApp(text.toString())
                    true
                } else false
            }
        }
        promptInput = edit

        val sendBtn = TextView(context).apply {
            text = "ส่ง"
            setTextColor(Color.BLACK)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#60A5FA"))
                cornerRadius = dp(14).toFloat()
            }
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener { sendToChatApp(edit.text.toString()) }
        }

        textInputContainer.addView(edit, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        })
        textInputContainer.addView(sendBtn)
        inputRow = textInputContainer
        card.addView(textInputContainer)

        val controlsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val plusBtn = TextView(context).apply {
            text = "+"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1FFFFFFF"))
            }
            setOnClickListener {
                isTextMode = !isTextMode
                inputRow?.visibility = if (isTextMode) View.VISIBLE else View.GONE
                if (isTextMode) {
                    stopOfflineListening()
                    promptInput?.requestFocus()
                } else {
                    startOfflineListening()
                }
            }
            setOnLongClickListener {
                openFullChatApp(null)
                true
            }
        }

        val centerWrapper = FrameLayout(context)
        val orb = TextView(context).apply {
            text = "🎙"
            textSize = 24f
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#93C5FD"), Color.parseColor("#3B82F6"), Color.parseColor("#1D4ED8"))
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), Color.parseColor("#80FFFFFF"))
            }
            setOnClickListener {
                if (isListening) stopOfflineListening() else {
                    isTextMode = false
                    inputRow?.visibility = View.GONE
                    startOfflineListening()
                }
            }
        }
        orbButton = orb
        centerWrapper.addView(orb, FrameLayout.LayoutParams(dp(64), dp(64), Gravity.CENTER))

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1FFFFFFF"))
            }
            setOnClickListener { hide() }
        }

        controlsRow.addView(plusBtn, LinearLayout.LayoutParams(dp(44), dp(44)))
        controlsRow.addView(centerWrapper, LinearLayout.LayoutParams(0, dp(68), 1f))
        controlsRow.addView(closeBtn, LinearLayout.LayoutParams(dp(44), dp(44)))
        card.addView(controlsRow)

        val statusView = TextView(context).apply {
            text = "ChatLLM Offline · กดค้างปุ่ม + เพื่อเปิดหน้าแชทเต็ม"
            setTextColor(Color.parseColor("#9CA3AF"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        card.addView(statusView)

        val cardParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        ).apply { setMargins(dp(12), dp(12), dp(12), dp(18)) }
        root.addView(card, cardParams)
        return root
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        isTextMode = false
        inputRow?.visibility = View.GONE
        promptInput?.setText("")
        startOfflineListening()
    }

    override fun onHide() {
        stopOfflineListening()
        super.onHide()
    }

    override fun onDestroy() {
        stopOfflineListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    private fun startOfflineListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            transcriptLabel?.text = "ยังไม่ได้อนุญาตไมโครโฟน — กดปุ่ม + เพื่อพิมพ์ถามได้เลย"
            isTextMode = true
            inputRow?.visibility = View.VISIBLE
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            transcriptLabel?.text = "กดปุ่ม + เพื่อพิมพ์คำถามแบบออฟไลน์"
            isTextMode = true
            inputRow?.visibility = View.VISIBLE
            return
        }
        runCatching {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    transcriptLabel?.text = "กำลังฟังเสียงของคุณ (ออฟไลน์)..."
                    startOrbPulse()
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { stopOrbPulse() }
                override fun onError(error: Int) {
                    isListening = false
                    stopOrbPulse()
                    transcriptLabel?.text = "แตะวงกลมเพื่อพูดใหม่ หรือกด + เพื่อพิมพ์ข้อความ"
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    stopOrbPulse()
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (spoken.isNotBlank()) {
                        transcriptLabel?.text = "\"$spoken\""
                        sendToChatApp(spoken)
                    } else {
                        transcriptLabel?.text = "ไม่ได้ยินเสียงพูด แตะวงกลมเพื่อลองอีกครั้ง"
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (partial.isNotBlank()) transcriptLabel?.text = partial
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            recognizer.startListening(intent)
        }.onFailure {
            isListening = false
            stopOrbPulse()
            transcriptLabel?.text = "กดปุ่ม + เพื่อพิมพ์คำถามแบบออฟไลน์"
        }
    }

    private fun stopOfflineListening() {
        isListening = false
        stopOrbPulse()
        runCatching { speechRecognizer?.stopListening() }
    }

    private fun startOrbPulse() {
        val target = orbButton ?: return
        pulseAnimator?.cancel()
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            target,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f, 1f)
        ).apply {
            duration = 1100
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopOrbPulse() {
        pulseAnimator?.cancel()
        orbButton?.scaleX = 1f
        orbButton?.scaleY = 1f
    }

    private fun sendToChatApp(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) {
            Toast.makeText(context, "กรุณาพูดหรือพิมพ์ข้อความก่อนส่ง", Toast.LENGTH_SHORT).show()
            return
        }
        AssistantBridge.submitPrompt(text)
        openFullChatApp(text)
    }

    private fun openFullChatApp(prompt: String?) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (!prompt.isNullOrBlank()) putExtra("assistant_prompt", prompt)
        }
        context.startActivity(launchIntent)
        hide()
    }
}

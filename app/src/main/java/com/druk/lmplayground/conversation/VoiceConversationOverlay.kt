package com.druk.ai_offline_llm.conversation

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import coil.compose.AsyncImage
import com.druk.ai_offline_llm.R
import com.druk.ai_offline_llm.dictation.DictationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

@Composable
fun VoiceConversationOverlay(
    visible: Boolean, isModelReady: Boolean, modelName: String?, isGenerating: Boolean,
    messages: List<Message>, micOn: Boolean, micRecording: StateFlow<Boolean>, micLevel: StateFlow<Float>,
    dictationState: DictationState, pendingTranscript: String?, onTranscriptConsumed: () -> Unit,
    supportsVision: Boolean, cameraAvailable: Boolean, attachedImageUri: Uri?,
    onAttachImage: () -> Unit, onTakePhoto: () -> Unit, onAttachDocument: () -> Unit,
    onClearImage: () -> Unit, onStartListening: () -> Unit, onStopListening: () -> Unit,
    onSendMessage: (String) -> Unit, onCancelGeneration: () -> Unit,
    onSelectModelClicked: () -> Unit, onClose: () -> Unit,
) {
    if (!visible) return
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var userMuted by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var awaitingReply by remember { mutableStateOf(false) }
    var wasGenerating by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var attachMenuExpanded by remember { mutableStateOf(false) }
    val isRecording by micRecording.collectAsState()
    val currentLevel by micLevel.collectAsState()

    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = engine?.setLanguage(Locale("th", "TH"))
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine?.setLanguage(Locale.getDefault())
                }
                ttsReady = true
            }
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { mainHandler.post { isSpeaking = true } }
            override fun onDone(id: String?) {
                mainHandler.post {
                    isSpeaking = false
                    if (visible && isModelReady && !userMuted && !isGenerating) {
                        mainHandler.postDelayed({ if (visible && isModelReady && !userMuted && !isGenerating && !isSpeaking) onStartListening() }, 350L)
                    }
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                mainHandler.post {
                    isSpeaking = false
                    if (visible && isModelReady && !userMuted && !isGenerating) onStartListening()
                }
            }
        })
        ttsInstance = engine
        onDispose { runCatching { engine?.stop(); engine?.shutdown() }; ttsInstance = null }
    }

    LaunchedEffect(visible, isModelReady) {
        if (visible && isModelReady && !userMuted && !isGenerating && !isSpeaking && !micOn) { delay(250L); onStartListening() }
    }
    LaunchedEffect(pendingTranscript, visible) {
        if (!visible) return@LaunchedEffect
        val text = pendingTranscript?.trim().orEmpty()
        if (text.isNotEmpty()) {
            onTranscriptConsumed()
            if (isModelReady) { awaitingReply = true; onSendMessage(text) }
        } else if (pendingTranscript != null) onTranscriptConsumed()
    }
    LaunchedEffect(micOn, isGenerating, isSpeaking, userMuted, isModelReady) {
        if (visible && isModelReady && !micOn && !isGenerating && !isSpeaking && !userMuted && !awaitingReply) {
            delay(650L)
            if (visible && isModelReady && !micOn && !isGenerating && !isSpeaking && !userMuted && !awaitingReply) onStartListening()
        }
    }
    LaunchedEffect(isGenerating) {
        if (wasGenerating && !isGenerating && awaitingReply) {
            awaitingReply = false
            val reply = messages.lastOrNull { it.author != "User" }?.content.orEmpty()
            val clean = cleanTextForSpeech(reply)
            if (clean.isNotEmpty() && ttsReady && ttsInstance != null) {
                isSpeaking = true
                ttsInstance?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "vr_${System.currentTimeMillis()}")
            } else if (!userMuted && isModelReady) onStartListening()
        }
        wasGenerating = isGenerating
    }

    val animVoice by animateFloatAsState(if (micOn && isRecording) currentLevel else if (isSpeaking) 0.45f else 0f, spring(stiffness = Spring.StiffnessMedium), label = "vp")
    val idlePulse by rememberInfiniteTransition(label = "vi").animateFloat(0.96f, 1.06f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "vis")
    val listeningText = (dictationState as? DictationState.Listening)?.text.orEmpty()
    val latestReply = messages.lastOrNull { it.author != "User" }?.content.orEmpty()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0x226BA6FF), Color(0x446BA6FF))))) {
            Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 18.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isModelReady) {
                    Surface(onClick = onSelectModelClicked, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
                        Text("โปรดเลือกโมเดล เพื่อสนทนา", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp))
                    }
                } else {
                    Text(when { isSpeaking -> "กำลังพูดตอบกลับ..."; isGenerating -> "กำลังคิดคำตอบ..."; micOn -> "กำลังฟังเสียงคุณ..."; else -> "ปิดไมค์อยู่ • แตะไมค์เพื่อพูด" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!modelName.isNullOrBlank()) Text(modelName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
                }
            }

            Column(modifier = Modifier.align(Alignment.Center).padding(horizontal = 28.dp).padding(bottom = 110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (attachedImageUri != null) {
                    Box(modifier = Modifier.padding(bottom = 16.dp)) {
                        AsyncImage(model = attachedImageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)).border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)))
                        Box(modifier = Modifier.align(Alignment.TopEnd).size(24.dp).clip(CircleShape).background(Color(0xFF111111)).clickable(onClick = onClearImage), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                val showTxt = when { listeningText.isNotBlank() -> listeningText; isSpeaking && latestReply.isNotBlank() -> latestReply; else -> "" }
                if (showTxt.isNotBlank()) Text(showTxt, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), textAlign = TextAlign.Center, maxLines = 6, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 420.dp))
            }

            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseR = 40.dp.toPx()
                        if (micOn || isSpeaking) {
                            drawCircle(Color(0xFF2B7FFF).copy(alpha = 0.16f), baseR + (18.dp.toPx() * animVoice) + (4.dp.toPx() * (idlePulse - 0.96f) * 10f))
                            drawCircle(Color(0xFF2B7FFF).copy(alpha = 0.25f), baseR + (9.dp.toPx() * animVoice))
                        }
                    }
                    Surface(
                        onClick = {
                            when {
                                isSpeaking -> { runCatching { ttsInstance?.stop() }; isSpeaking = false; userMuted = false; if (isModelReady) onStartListening() }
                                isGenerating -> { onCancelGeneration(); awaitingReply = false; userMuted = false; if (isModelReady) onStartListening() }
                                micOn -> { userMuted = true; onStopListening() }
                                else -> { userMuted = false; if (isModelReady) onStartListening() else onSelectModelClicked() }
                            }
                        },
                        shape = CircleShape, color = if (micOn) Color.White else MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 6.dp,
                        border = BorderStroke(if (micOn) 2.dp else 1.dp, if (micOn) Color(0xFF2B7FFF) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (micOn || isSpeaking || isGenerating) Icons.Filled.Mic else Icons.Filled.MicOff, "เปิด/ปิดไมค์", tint = if (micOn) Color(0xFF111111) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val density = LocalDensity.current
                    Box {
                        Surface(onClick = { if (supportsVision) attachMenuExpanded = true else onAttachDocument() }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 3.dp, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), modifier = Modifier.size(54.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, "แนบไฟล์หรือรูปภาพ", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp)) }
                        }
                        if (attachMenuExpanded) {
                            val gapPx = with(density) { 14.dp.roundToPx() }
                            val pos = remember(gapPx) { object : PopupPositionProvider { override fun calculatePosition(a: IntRect, w: IntSize, l: LayoutDirection, p: IntSize) = IntOffset(a.left.coerceIn(0, (w.width - p.width).coerceAtLeast(0)), (a.top - gapPx - p.height).coerceAtLeast(0)) } }
                            Popup(popupPositionProvider = pos, onDismissRequest = { attachMenuExpanded = false }, properties = PopupProperties(focusable = true)) {
                                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 4.dp) {
                                    Column(Modifier.width(IntrinsicSize.Max).padding(vertical = 6.dp)) {
                                        if (cameraAvailable) DropdownMenuItem(text = { Text(stringResource(R.string.take_photo)) }, leadingIcon = { Icon(Icons.Outlined.PhotoCamera, null) }, onClick = { attachMenuExpanded = false; onTakePhoto() })
                                        DropdownMenuItem(text = { Text(stringResource(R.string.choose_from_library)) }, leadingIcon = { Icon(Icons.Outlined.Image, null) }, onClick = { attachMenuExpanded = false; onAttachImage() })
                                        DropdownMenuItem(text = { Text(stringResource(R.string.attach_document)) }, leadingIcon = { Icon(Icons.Outlined.Description, null) }, onClick = { attachMenuExpanded = false; onAttachDocument() })
                                    }
                                }
                            }
                        }
                    }
                    Surface(onClick = { runCatching { ttsInstance?.stop() }; isSpeaking = false; onClose() }, shape = CircleShape, color = Color(0xFF111111), shadowElevation = 3.dp, modifier = Modifier.size(54.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, "ปิดโหมดสนทนาด้วยเสียง", tint = Color.White, modifier = Modifier.size(22.dp)) }
                    }
                }
            }
        }
    }
}
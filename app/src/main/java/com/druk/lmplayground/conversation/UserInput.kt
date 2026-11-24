package com.druk.ai_offline_llm.conversation
import androidx.compose.foundation.layout.height
import kotlin.random.Random
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.druk.ai_offline_llm.R
import com.druk.ai_offline_llm.dictation.DictationState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

enum class UserInputStatus {
    IDLE,
    NOT_LOADED,
    GENERATING
}

@Preview
@Composable
fun UserInputPreview() {
    UserInput(onMessageSent = {})
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserInput(
    integrateWithSurface: Boolean = false,
    showTopDivider: Boolean = integrateWithSurface,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    hazeStyle: HazeStyle = HazeStyle.Unspecified,
    status: UserInputStatus = UserInputStatus.IDLE,
    focusRequester: FocusRequester = remember { FocusRequester() },
    supportsThinking: Boolean = false,
    thinkingEnabled: Boolean = true,
    onThinkingToggle: () -> Unit = {},
    supportsVision: Boolean = false,
    cameraAvailable: Boolean = false,
    attachedImageUri: Uri? = null,
    onAttachImage: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onAttachDocument: () -> Unit = {},
    onClearImage: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onMessageSent: (String) -> Unit,
    onCancelClicked: () -> Unit = {},
    resetScroll: () -> Unit = {},
    dictationState: DictationState = DictationState.Idle,
    dictationDownloadProgress: Float? = null,
    micOn: Boolean = false,
    micRecording: StateFlow<Boolean> = IdleMicRecording,
    micLevel: StateFlow<Float> = IdleMicLevel,
    onMicClick: () -> Unit = {},
    onDictationAbandoned: () -> Unit = {},
    pendingTranscript: String? = null,
    onTranscriptConsumed: () -> Unit = {},
    onVoiceModeClicked: () -> Unit = {},
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    var showThinkingBadge by remember { mutableStateOf(false) }
    var prevThinkingEnabled by remember { mutableStateOf(thinkingEnabled) }
    var thinkingBadgeTrigger by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    LaunchedEffect(thinkingEnabled, supportsThinking) {
        if (supportsThinking && thinkingEnabled && !prevThinkingEnabled) {
            thinkingBadgeTrigger += 1
            showThinkingBadge = true
        } else if (!thinkingEnabled || !supportsThinking) {
            showThinkingBadge = false
        }
        prevThinkingEnabled = thinkingEnabled
    }

    LaunchedEffect(thinkingBadgeTrigger, showThinkingBadge) {
        if (showThinkingBadge && thinkingBadgeTrigger > 0) {
            delay(2000L)
            showThinkingBadge = false
        }
    }

    var dictationBase by remember { mutableStateOf<TextFieldValue?>(null) }
    var autoSendAfterDictation by remember { mutableStateOf(false) }

    LaunchedEffect(dictationState) {
        val listening = dictationState as? DictationState.Listening ?: return@LaunchedEffect
        val base = dictationBase ?: textState.also { dictationBase = it }
        textState = base.insertAtCursor(listening.text)
    }

    LaunchedEffect(pendingTranscript) {
        val transcript = pendingTranscript ?: return@LaunchedEffect
        val composed = (dictationBase ?: textState).insertAtCursor(transcript)
        dictationBase = null
        onTranscriptConsumed()
        if (autoSendAfterDictation) {
            autoSendAfterDictation = false
            val finalMsg = composed.text.trim()
            textState = TextFieldValue()
            if (finalMsg.isNotEmpty() && status == UserInputStatus.IDLE) { onMessageSent(finalMsg); resetScroll() }
        } else textState = composed
    }

    LaunchedEffect(micOn) {
        if (!micOn && autoSendAfterDictation && pendingTranscript == null) {
            delay(350L)
            if (!micOn && autoSendAfterDictation && pendingTranscript == null) {
                autoSendAfterDictation = false
                val fb = textState.text.trim()
                textState = TextFieldValue(); dictationBase = null
                if (fb.isNotEmpty() && status == UserInputStatus.IDLE) { onMessageSent(fb); resetScroll() }
            }
        }
    }

    fun releaseFromDictation() {
        if (dictationBase == null) return
        dictationBase = null
        onDictationAbandoned()
    }

    var textFieldFocusState by remember { mutableStateOf(false) }

    var dragAccumulator by remember { mutableStateOf(0f) }
    val swipeThreshold = -150f
    val draggableState = rememberDraggableState { delta ->
        dragAccumulator += delta
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStarted = { dragAccumulator = 0f },
                    onDragStopped = {
                        if (dragAccumulator < swipeThreshold) {
                            onSwipeUp()
                        }
                        dragAccumulator = 0f
                    }
                )
        ) {

            if (attachedImageUri != null) {
                Box(
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                ) {
                    AsyncImage(
                        model = attachedImageUri,
                        contentDescription = "Attached image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(top = 6.dp, end = 6.dp)
                            .size(76.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(14.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.inverseSurface)
                            .clickable(onClick = onClearImage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove image",
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showThinkingBadge && thinkingEnabled && supportsThinking,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    ),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(id = R.string.thinking_mode_enabled),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Gemini-style floating rounded pill composer container
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 3.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                UserInputText(
                    status = status,
                    focusRequester = focusRequester,
                    supportsThinking = supportsThinking,
                    thinkingEnabled = thinkingEnabled,
                    onThinkingToggle = onThinkingToggle,
                    supportsVision = supportsVision,
                    cameraAvailable = cameraAvailable,
                    onAttachImage = onAttachImage,
                    onTakePhoto = onTakePhoto,
                    onAttachDocument = onAttachDocument,
                    dictationState = dictationState, micOn = micOn, micRecording = micRecording, micLevel = micLevel,
                    onMicClick = onMicClick,
                    onDictationCancel = { autoSendAfterDictation = false; textState = dictationBase ?: TextFieldValue(); dictationBase = null; onDictationAbandoned() },
                    onMicSendImmediately = { autoSendAfterDictation = true; onMicClick() },
                    onVoiceModeClicked = onVoiceModeClicked,
                    dictationDownloadProgress = dictationDownloadProgress,
                    textFieldValue = textState,
                    onTextChanged = {
                        if (it.text != textState.text) releaseFromDictation()
                        textState = it
                    },
                    keyboardShown = textFieldFocusState,
                    onTextFieldFocused = { focused ->
                        if (focused) {
                            resetScroll()
                        }
                        textFieldFocusState = focused
                    },
                    sendMessageEnabled = textState.text.isNotBlank(),
                    onMessageSent = {
                        releaseFromDictation()
                        onMessageSent(textState.text)
                        textState = TextFieldValue()
                        resetScroll()
                    },
                    onCancelClicked = onCancelClicked,
                    compact = integrateWithSurface
                )
            }
        }
    }
}

private fun TextFieldValue.insertAtCursor(text: String): TextFieldValue {
    if (text.isEmpty()) return this
    val start = selection.min.coerceIn(0, this.text.length)
    val end = selection.max.coerceIn(0, this.text.length)
    val before = this.text.substring(0, start)
    val after = this.text.substring(end)
    val separator = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
    val inserted = separator + text
    return TextFieldValue(
        text = before + inserted + after,
        selection = TextRange(start + inserted.length),
    )
}

@OptIn(ExperimentalLayoutApi::class)
fun Modifier.clearFocusOnKeyboardDismiss(): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    var keyboardAppearedSinceLastFocused by remember { mutableStateOf(false) }
    if (isFocused) {
        val imeIsVisible = WindowInsets.isImeVisible
        val focusManager = LocalFocusManager.current
        LaunchedEffect(imeIsVisible) {
            if (imeIsVisible) {
                keyboardAppearedSinceLastFocused = true
            } else if (keyboardAppearedSinceLastFocused) {
                focusManager.clearFocus()
            }
        }
    }
    onFocusEvent {
        if (isFocused != it.isFocused) {
            isFocused = it.isFocused
            if (isFocused) {
                keyboardAppearedSinceLastFocused = false
            }
        }
    }
}

val KeyboardShownKey = SemanticsPropertyKey<Boolean>("KeyboardShownKey")
var SemanticsPropertyReceiver.keyboardShownProperty by KeyboardShownKey

private val IdleMicLevel: StateFlow<Float> = MutableStateFlow(0f)
private val IdleMicRecording: StateFlow<Boolean> = MutableStateFlow(false)

@ExperimentalFoundationApi
@Composable
private fun UserInputText(
    status: UserInputStatus,
    focusRequester: FocusRequester,
    supportsThinking: Boolean = false,
    thinkingEnabled: Boolean = true,
    onThinkingToggle: () -> Unit = {},
    supportsVision: Boolean = false,
    cameraAvailable: Boolean = false,
    onAttachImage: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onAttachDocument: () -> Unit = {},
    dictationState: DictationState = DictationState.Idle, micOn: Boolean = false,
    micRecording: StateFlow<Boolean> = IdleMicRecording, micLevel: StateFlow<Float> = IdleMicLevel,
    onMicClick: () -> Unit = {}, onDictationCancel: () -> Unit = {},
    onMicSendImmediately: () -> Unit = {}, onVoiceModeClicked: () -> Unit = {},
    dictationDownloadProgress: Float? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    onTextChanged: (TextFieldValue) -> Unit,
    textFieldValue: TextFieldValue,
    keyboardShown: Boolean,
    onTextFieldFocused: (Boolean) -> Unit,
    sendMessageEnabled: Boolean,
    onMessageSent: () -> Unit,
    onCancelClicked: () -> Unit,
    compact: Boolean = false
) {
    val a11ylabel = stringResource(id = R.string.textfield_desc)
    val showAttach = true
    val hasLeftButtons = supportsThinking || showAttach
    val textStartPadding = if (hasLeftButtons) 4.dp else 18.dp

    val isDictating = micOn || dictationState is DictationState.Listening || dictationState is DictationState.Finishing
    if (isDictating) {
        val isRec by micRecording.collectAsState()
        val liveLevel by micLevel.collectAsState()
        val waveBars = remember { mutableStateListOf<Float>().apply { repeat(28) { add(0.08f) } } }
        LaunchedEffect(isDictating, isRec) {
            while (isDictating) {
                val target = if (isRec) (liveLevel * 0.9f + Random.nextFloat() * 0.15f).coerceIn(0.08f, 1f) else 0.08f
                if (waveBars.isNotEmpty()) waveBars.removeAt(0)
                waveBars.add(target)
                delay(65L)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = if (compact) 3.dp else 6.dp).heightIn(min = if (compact) 42.dp else 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onDictationCancel, shape = CircleShape, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, "ยกเลิก", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(19.dp)) }
            }
            Canvas(modifier = Modifier.weight(1f).height(34.dp).padding(horizontal = 10.dp)) {
                val step = size.width / waveBars.size
                val dotW = (step * 0.42f).coerceIn(2.dp.toPx(), 3.5.dp.toPx())
                val maxH = size.height * 0.78f
                waveBars.forEachIndexed { i, amp ->
                    val barH = (dotW + (maxH - dotW) * amp).coerceIn(dotW, maxH)
                    drawRoundRect(Color(0xFF1A1A1A), Offset(i * step + (step - dotW) / 2f, (size.height - barH) / 2f), Size(dotW, barH), CornerRadius(dotW / 2f, dotW / 2f))
                }
            }
            Surface(onClick = onMicClick, shape = CircleShape, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.onSurface)) }
            }
            Spacer(Modifier.width(6.dp))
            Surface(onClick = onMicSendImmediately, shape = CircleShape, color = Color(0xFFB85D3B), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.ArrowUpward, stringResource(id = R.string.send), tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
        }
        return
    }
        Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = if (compact) 3.dp else 6.dp)
            .heightIn(min = if (compact) 42.dp else 48.dp, max = 320.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAttach) {
            val isDisabled = status == UserInputStatus.GENERATING
            var menuExpanded by remember { mutableStateOf(false) }
            val density = LocalDensity.current
            Box {
                Surface(
                    onClick = {
                        if (supportsVision) menuExpanded = true else onAttachDocument()
                    },
                    enabled = !isDisabled,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Attach",
                            modifier = if (isDisabled) Modifier.alpha(0.5f) else Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (menuExpanded) {
                    val gapPx = with(density) { 16.dp.roundToPx() }
                    val positionProvider = remember(gapPx) {
                        object : PopupPositionProvider {
                            override fun calculatePosition(
                                anchorBounds: IntRect,
                                windowSize: IntSize,
                                layoutDirection: LayoutDirection,
                                popupContentSize: IntSize
                            ): IntOffset {
                                val x = anchorBounds.left
                                    .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                                val y = (anchorBounds.top - gapPx - popupContentSize.height)
                                    .coerceAtLeast(0)
                                return IntOffset(x, y)
                            }
                        }
                    }
                    Popup(
                        popupPositionProvider = positionProvider,
                        onDismissRequest = { menuExpanded = false },
                        properties = PopupProperties(focusable = true)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.width(IntrinsicSize.Max).padding(vertical = 6.dp)) {
                                if (cameraAvailable) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.take_photo)) },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Outlined.PhotoCamera, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onTakePhoto()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.choose_from_library)) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Outlined.Image, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onAttachImage()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.attach_document)) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Outlined.Description, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onAttachDocument()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (supportsThinking) {
            val isDisabled = status == UserInputStatus.GENERATING
            IconButton(
                onClick = onThinkingToggle,
                enabled = !isDisabled,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (thinkingEnabled) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                    contentDescription = if (thinkingEnabled) "Disable thinking" else "Enable thinking",
                    modifier = if (isDisabled) Modifier.alpha(0.5f) else Modifier,
                    tint = if (thinkingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(Modifier.weight(1f)) {
            UserInputTextField(
                isEnabled = true,
                focusRequester = focusRequester,
                textFieldValue = textFieldValue,
                onTextChanged = onTextChanged,
                onTextFieldFocused = onTextFieldFocused,
                keyboardType = keyboardType,
                startPadding = textStartPadding,
                modifier = Modifier.semantics {
                    contentDescription = a11ylabel
                    keyboardShownProperty = keyboardShown
                }
            )
        }

        MicButton(
            micOn = micOn,
            enabled = status != UserInputStatus.GENERATING,
            downloadProgress = dictationDownloadProgress,
            recording = micRecording,
            level = micLevel,
            onClick = onMicClick,
            modifier = Modifier.padding(horizontal = 2.dp),
        )

        Spacer(modifier = Modifier.width(2.dp))

        // ChatGPT-style circular Voice Waveform / Send / Stop button
        when (status) {
            UserInputStatus.IDLE, UserInputStatus.NOT_LOADED -> {
                val canSend = sendMessageEnabled && status == UserInputStatus.IDLE
                Surface(
                    onClick = { if (canSend) onMessageSent() else onVoiceModeClicked() },
                    shape = CircleShape,
                    color = Color(0xFF111111),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (canSend) {
                            Icon(
                                imageVector = Icons.Filled.ArrowUpward,
                                contentDescription = stringResource(id = R.string.send),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Canvas(modifier = Modifier.size(18.dp)) {
                                val heights = listOf(0.35f, 0.65f, 1.0f, 0.65f, 0.35f)
                                val barW = size.width / 9f
                                val gap = barW
                                val totalW = heights.size * barW + (heights.size - 1) * gap
                                val startX = (size.width - totalW) / 2f
                                heights.forEachIndexed { idx, ratio ->
                                    val barH = size.height * ratio
                                    val topY = (size.height - barH) / 2f
                                    drawRoundRect(
                                        color = Color.White,
                                        topLeft = Offset(startX + idx * (barW + gap), topY),
                                        size = Size(barW, barH),
                                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            UserInputStatus.GENERATING -> {
                Surface(
                    onClick = onCancelClicked,
                    shape = CircleShape,
                    color = Color(0xFF111111),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = stringResource(id = R.string.stop),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.UserInputTextField(
    isEnabled: Boolean,
    focusRequester: FocusRequester,
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onTextFieldFocused: (Boolean) -> Unit,
    keyboardType: KeyboardType,
    startPadding: Dp,
    modifier: Modifier = Modifier
) {
    var lastFocusState by remember { mutableStateOf(false) }
    BasicTextField(
        value = textFieldValue,
        onValueChange = { onTextChanged(it) },
        modifier = modifier
            .padding(start = startPadding, end = 6.dp)
            .align(Alignment.CenterStart)
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .clearFocusOnKeyboardDismiss()
            .onFocusChanged { state ->
                if (lastFocusState != state.isFocused) {
                    onTextFieldFocused(state.isFocused)
                }
                lastFocusState = state.isFocused
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.None
        ),
        enabled = isEnabled,
        maxLines = 4,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
    )

    if (textFieldValue.text.isEmpty()) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = startPadding),
            text = stringResource(R.string.textfield_hint),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

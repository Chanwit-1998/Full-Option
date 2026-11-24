package com.druk.ai_offline_llm.conversation

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.druk.ai_offline_llm.R
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay

@Composable
fun ChatItemBubble(
    message: Message,
    showActions: Boolean = true,
    onTokenCountClicked: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onBranch: (() -> Unit)? = null,
) {
    var showRatingSheet by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Text-to-Speech (อ่านออกเสียง) state and lifecycle management
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val thaiLocale = Locale("th", "TH")
                val result = engine?.setLanguage(thaiLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine?.language = Locale.getDefault()
                }
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        isSpeaking = false
                    }
                })
                ttsInstance = engine
            }
        }
        onDispose {
            engine?.stop()
            engine?.shutdown()
            ttsInstance = null
            isSpeaking = false
        }
    }

    val hasToolCalls = !message.toolCalls.isNullOrEmpty()
    val isWaitingForResponse = !showActions
        && message.content.isEmpty()
        && !hasToolCalls
    val isGenerating = !showActions

    val split = remember(message.content) { splitThinking(message.content) }

    Column {
        if (isWaitingForResponse) {
            ThinkingIndicator()
        } else {
            // Tool rounds: render each round's thinking immediately before its
            // tool call(s), in chronological order.
            if (hasToolCalls) {
                val thinkingText = stringResource(R.string.thinking)
                val inputLabel = stringResource(R.string.tool_call_input)
                val outputLabel = stringResource(R.string.tool_call_output)
                for (toolCall in message.toolCalls!!) {
                    if (toolCall.precedingThinking.isNotEmpty()) {
                        val pre = remember(toolCall.precedingThinking) {
                            splitThinking(toolCall.precedingThinking)
                        }
                        if (pre.thinkingContent.isNotEmpty()) {
                            CollapsibleSection(
                                label = buildString {
                                    append("$thinkingText \u00B7 ${formatDuration(toolCall.precedingThinkingDurationSeconds)}")
                                    if (toolCall.precedingThinkingTokens > 0) {
                                        append(" \u00B7 ${toolCall.precedingThinkingTokens} tokens")
                                    }
                                },
                                content = pre.thinkingContent
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    val durationSec = (toolCall.durationMs / 1000).toInt().coerceAtLeast(
                        if (toolCall.durationMs > 0) 1 else 0
                    )
                    CollapsibleSection(
                        label = "${toolDisplayName(toolCall.name)} \u00B7 ${formatDuration(durationSec)}",
                        content = buildString {
                            if (toolCall.arguments.isNotBlank()) {
                                append(inputLabel).append('\n')
                                append(prettyJson(toolCall.arguments))
                                append("\n\n")
                            }
                            append(outputLabel).append('\n')
                            append(prettyJson(toolCall.result))
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // If still generating after tool calls, show thinking indicator
            if (isGenerating && hasToolCalls && message.content.isEmpty()) {
                ThinkingIndicator()
            }

            // Post-tool thinking + Response (split into normal text & styled code blocks)
            val hasThinking = split.thinkingContent.isNotEmpty()

            if (hasThinking) {
                val thinkingText = stringResource(R.string.thinking)
                CollapsibleSection(
                    label = buildString {
                        append("$thinkingText \u00B7 ${formatDuration(message.thinkingDurationSeconds)}")
                        if (message.thinkingTokens > 0) {
                            append(" \u00B7 ${message.thinkingTokens} tokens")
                        }
                    },
                    content = split.thinkingContent
                )
                if (split.responseContent.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (split.responseContent.isNotEmpty()) {
                val blocks = remember(split.responseContent) {
                    parseMarkdownBlocks(split.responseContent)
                }
                blocks.forEachIndexed { index, block ->
                    when (block) {
                        is MessageBlock.TextBlock -> {
                            if (block.text.isNotBlank()) {
                                val styledMessage = messageFormatter(
                                    text = block.text,
                                    primary = false
                                )
                                SelectionContainer {
                                    Text(
                                        text = styledMessage,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = LocalContentColor.current
                                        )
                                    )
                                }
                            }
                        }
                        is MessageBlock.CodeBlock -> {
                            CodeBlockCard(
                                language = block.language,
                                code = block.code
                            )
                        }
                    }
                    if (index < blocks.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        message.image?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Image(
                painter = painterResource(it),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(160.dp),
                contentDescription = stringResource(id = R.string.attached_image)
            )
        }

        message.imageUri?.let { uri ->
            Spacer(modifier = Modifier.height(4.dp))
            coil.compose.AsyncImage(
                model = uri,
                contentDescription = stringResource(id = R.string.attached_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(160.dp)
            )
        }

        if (showActions) {
            if (message.responseTokens + message.thinkingTokens > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatResponseStats(message),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .then(
                            if (onTokenCountClicked != null) {
                                Modifier.clickable(onClick = onTokenCountClicked)
                            } else {
                                Modifier
                            }
                        )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom action bar: left group (Copy, Upvote, Downvote, Read Aloud, Share, Branch)
            // + far-right Reset/Regenerate button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Copy full message
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(stripThinkTags(message.content)))
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(id = R.string.copy),
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // 4. Read Aloud (ปุ่มอ่านออกเสียง)
                    IconButton(
                        onClick = {
                            val tts = ttsInstance ?: return@IconButton
                            if (isSpeaking) {
                                tts.stop()
                                isSpeaking = false
                            } else {
                                val textToRead = cleanTextForSpeech(
                                    split.responseContent.ifBlank { stripThinkTags(message.content) }
                                )
                                if (textToRead.isNotBlank()) {
                                    isSpeaking = true
                                    val hasThai = textToRead.any { ch -> ch in '\u0E00'..'\u0E7F' }
                                    val targetLocale = if (hasThai) Locale("th", "TH") else Locale.getDefault()
                                    configureNaturalSpeech(tts, targetLocale)
                                    val chunks = splitSpeechChunks(textToRead)
                                    val params = Bundle()
                                    chunks.forEachIndexed { index, chunk ->
                                        val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                                        val utteranceId = UUID.randomUUID().toString()
                                        tts.speak(chunk, queueMode, params, utteranceId)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Outlined.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = if (isSpeaking) "หยุดอ่าน" else "อ่านออกเสียง",
                            modifier = Modifier.size(18.dp),
                            tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    // 5. Share
                    val shareLabel = stringResource(id = R.string.share)
                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, stripThinkTags(message.content))
                            }
                            context.startActivity(Intent.createChooser(sendIntent, shareLabel))
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = shareLabel,
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Optional Branch option
                    if (onBranch != null) {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(38.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = stringResource(id = R.string.more_options),
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.branch_from_here)) },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Outlined.CallSplit, contentDescription = null)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        onBranch()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 6. Reset / Regenerate button pinned to the far right
                if (onRegenerate != null) {
                    IconButton(
                        onClick = {
                            ttsInstance?.stop()
                            isSpeaking = false
                            onRegenerate()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(id = R.string.regenerate),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }

    if (showRatingSheet) {
        RatingBottomSheet(onDismiss = { showRatingSheet = false })
    }
}

/**
 * Represents either a normal prose block or a fenced code block inside an AI response.
 */
private sealed interface MessageBlock {
    data class TextBlock(val text: String) : MessageBlock
    data class CodeBlock(val language: String, val code: String) : MessageBlock
}

/**
 * Parses Markdown content into alternating [MessageBlock.TextBlock] and [MessageBlock.CodeBlock].
 * Also supports unclosed code blocks while streaming responses.
 */
private fun parseMarkdownBlocks(markdown: String): List<MessageBlock> {
    val result = mutableListOf<MessageBlock>()
    val lines = markdown.lines()
    val textBuffer = StringBuilder()
    val codeBuffer = StringBuilder()
    var inCodeBlock = false
    var currentLang = ""
    var fenceMarker = "```"

    for (line in lines) {
        val trimmedStart = line.trimStart()
        if (!inCodeBlock) {
            if (trimmedStart.startsWith("```") || trimmedStart.startsWith("~~~")) {
                if (textBuffer.isNotEmpty()) {
                    result.add(MessageBlock.TextBlock(textBuffer.toString().trimEnd()))
                    textBuffer.clear()
                }
                fenceMarker = if (trimmedStart.startsWith("~~~")) "~~~" else "```"
                currentLang = trimmedStart.removePrefix(fenceMarker).trim().substringBefore(" ")
                inCodeBlock = true
            } else {
                if (textBuffer.isNotEmpty()) textBuffer.append('\n')
                textBuffer.append(line)
            }
        } else {
            if (trimmedStart.startsWith(fenceMarker)) {
                result.add(
                    MessageBlock.CodeBlock(
                        language = currentLang.ifBlank { "code" },
                        code = codeBuffer.toString()
                    )
                )
                codeBuffer.clear()
                currentLang = ""
                inCodeBlock = false
            } else {
                if (codeBuffer.isNotEmpty()) codeBuffer.append('\n')
                codeBuffer.append(line)
            }
        }
    }

    if (inCodeBlock) {
        result.add(
            MessageBlock.CodeBlock(
                language = currentLang.ifBlank { "code" },
                code = codeBuffer.toString()
            )
        )
    } else if (textBuffer.isNotEmpty()) {
        result.add(MessageBlock.TextBlock(textBuffer.toString().trimEnd()))
    }

    return result
}

/**
 * Styled code block card with rounded border, language label header,
 * "คัดลอก" (Copy code only) button, and multi-language syntax highlighting.
 */
@Composable
private fun CodeBlockCard(
    language: String,
    code: String
) {
    val clipboardManager = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    val cardBackground = if (isDark) Color(0xFF1E1F22) else Color(0xFFF7F8FA)
    val headerBackground = if (isDark) Color(0xFF26282C) else Color(0xFFF1F3F5)
    val borderColor = if (isDark) Color(0xFF383A40) else Color(0xFFE2E5E9)

    val highlightedCode = remember(code, language, isDark) {
        highlightCodeSyntax(code = code, isDark = isDark)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = cardBackground,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column {
            // Header bar: language name on left, "คัดลอก" button on right
            Surface(
                color = headerBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = language.lowercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(code))
                                copied = true
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = "คัดลอกโค้ด",
                            modifier = Modifier.size(15.dp),
                            tint = if (copied) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (copied) "คัดลอกแล้ว" else "คัดลอก",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (copied) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = borderColor)

            // Syntax-highlighted scrollable code body
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text(
                        text = highlightedCode,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            color = if (isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Lightweight multi-language syntax highlighter for Python, Bash/Shell, Kotlin, Java,
 * JavaScript/TypeScript, C/C++, JSON, SQL, etc.
 */
private fun highlightCodeSyntax(code: String, isDark: Boolean): AnnotatedString {
    val keywordColor = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
    val stringColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
    val commentColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val functionColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
    val numberColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val decoratorColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)

    val tripleDouble = "\"\"\"[\\s\\S]*?\"\"\""
    val tripleSingle = "\'\'\'[\\s\\S]*?\'\'\'"
    val singleOrDouble = "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`"
    val comments = "#[^\\n]*|//[^\\n]*|/\\*[\\s\\S]*?\\*/"
    val decorators = "@[A-Za-z_][A-Za-z0-9_]*"
    val keywords = "\\b(def|class|if|elif|else|for|while|return|import|from|try|except|finally|with|as|pass|break|continue|in|is|not|and|or|lambda|yield|raise|global|nonlocal|assert|del|True|False|None|self|cls|fun|val|var|when|object|interface|enum|sealed|data|package|public|private|protected|internal|override|suspend|companion|init|this|super|null|true|false|function|const|let|export|default|async|await|new|typeof|instanceof|void|int|float|double|boolean|char|String|echo|sudo|pkg|pip|pip3|apt|npm|npx|yarn|git|cd|ls|mkdir|rm|cp|mv|chmod|chown|cat|grep|curl|wget|python|python3|node|java|javac|kotlin|gradle)\\b"
    val funcCalls = "\\b([A-Za-z_][A-Za-z0-9_]*)(?=\\s*\\()"
    val numbers = "\\b(\\d+(?:\\.\\d+)?)\\b"

    val tokenRegex = Regex(
        "($tripleDouble|$tripleSingle)|($singleOrDouble)|($comments)|($decorators)|$keywords|$funcCalls|$numbers"
    )

    return buildAnnotatedString {
        append(code)
        for (match in tokenRegex.findAll(code)) {
            val style = when {
                match.groups[1] != null || match.groups[2] != null ->
                    SpanStyle(color = stringColor)
                match.groups[3] != null ->
                    SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)
                match.groups[4] != null ->
                    SpanStyle(color = decoratorColor)
                match.groups[5] != null ->
                    SpanStyle(color = keywordColor, fontWeight = FontWeight.Medium)
                match.groups[6] != null ->
                    SpanStyle(color = functionColor)
                match.groups[7] != null ->
                    SpanStyle(color = numberColor)
                else -> null
            }
            if (style != null) {
                addStyle(style = style, start = match.range.first, end = match.range.last + 1)
            }
        }
    }
}

private fun configureNaturalSpeech(tts: TextToSpeech, locale: Locale) {
    val chosenLocale = if (locale.language == "th") Locale("th", "TH") else locale
    val availability = tts.isLanguageAvailable(chosenLocale)
    if (availability >= TextToSpeech.LANG_AVAILABLE) {
        tts.language = chosenLocale
    } else if (chosenLocale.language == "th") {
        val fallbackTh = Locale.forLanguageTag("th")
        if (tts.isLanguageAvailable(fallbackTh) >= TextToSpeech.LANG_AVAILABLE) {
            tts.language = fallbackTh
        }
    }

    val voices = runCatching { tts.voices }.getOrNull().orEmpty()
    val matchingVoices = voices.filter { voice ->
        voice.locale.language.equals(chosenLocale.language, ignoreCase = true) &&
            !voice.isNetworkConnectionRequired
    }.ifEmpty {
        voices.filter { voice ->
            voice.locale.language.equals(chosenLocale.language, ignoreCase = true)
        }
    }
    val preferredVoice = matchingVoices.firstOrNull { voice ->
        val name = voice.name.lowercase()
        "th-th" in name || "neural" in name || "wavenet" in name || "high" in name || "default" in name
    } ?: matchingVoices.firstOrNull()
    if (preferredVoice != null) {
        runCatching { tts.voice = preferredVoice }
    }

    tts.setPitch(if (chosenLocale.language == "th") 1.02f else 1.0f)
    tts.setSpeechRate(if (chosenLocale.language == "th") 1.04f else 1.0f)
}

private fun stripEmojisAndSymbolsForSpeech(input: String): String {
    val sb = StringBuilder(input.length)
    var i = 0
    while (i < input.length) {
        val codePoint = Character.codePointAt(input, i)
        val charCount = Character.charCount(codePoint)
        val isVariationOrZwj = codePoint == 0xFE0E || codePoint == 0xFE0F || codePoint == 0x200D || codePoint == 0x20E3
        val isRegionalIndicator = codePoint in 0x1F1E6..0x1F1FF
        val isEmojiRange = codePoint in 0x1F300..0x1FAFF ||
            codePoint in 0x1F000..0x1F2FF ||
            codePoint in 0x2600..0x27BF ||
            codePoint in 0x2300..0x23FF ||
            codePoint in 0x2B00..0x2BFF ||
            codePoint in 0xE000..0xF8FF ||
            codePoint in 0xE0020..0xE007F
        val type = Character.getType(codePoint)
        val isSymbol = type == Character.OTHER_SYMBOL.toInt() ||
            type == Character.SURROGATE.toInt() ||
            type == Character.PRIVATE_USE.toInt()

        if (!isVariationOrZwj && !isRegionalIndicator && !isEmojiRange && !isSymbol) {
            sb.appendCodePoint(codePoint)
        } else if (isEmojiRange || isSymbol) {
            sb.append(' ')
        }
        i += charCount
    }
    return sb.toString()
}

private fun splitSpeechChunks(text: String, maxLength: Int = 220): List<String> {
    if (text.length <= maxLength) return listOf(text)
    val sentences = text.split(Regex("(?<=[.!?\\n])\\s+"))
    val chunks = mutableListOf<String>()
    val current = StringBuilder()

    for (part in sentences) {
        val cleanPart = part.trim()
        if (cleanPart.isEmpty()) continue
        if (current.isEmpty()) {
            if (cleanPart.length <= maxLength) {
                current.append(cleanPart)
            } else {
                var start = 0
                while (start < cleanPart.length) {
                    val end = minOf(start + maxLength, cleanPart.length)
                    chunks.add(cleanPart.substring(start, end).trim())
                    start = end
                }
            }
        } else if (current.length + 1 + cleanPart.length <= maxLength) {
            current.append(' ').append(cleanPart)
        } else {
            chunks.add(current.toString())
            current.clear()
            current.append(cleanPart)
        }
    }
    if (current.isNotEmpty()) {
        chunks.add(current.toString())
    }
    return chunks.ifEmpty { listOf(text) }
}

/**
 * Strips emojis, Markdown symbols, and code fences so Text-to-Speech reads naturally.
 */
internal fun cleanTextForSpeech(raw: String): String {
    val withoutThink = stripThinkTags(raw)
        .replace(Regex("```[a-zA-Z0-9_-]*"), " ")
        .replace("```", " ")
        .replace("`", "")
        .replace(Regex("[#*_~>|]+"), " ")
    return stripEmojisAndSymbolsForSpeech(withoutThink)
        .replace(Regex("\\s+"), " ")
        .trim()
}

/**
 * Reusable collapsible section with arrow icon, label, and expandable content.
 * Used for both thinking blocks and tool call results.
 */
@Composable
private fun CollapsibleSection(
    label: String,
    content: String
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clickable { expanded = !expanded }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
            contentDescription = if (expanded) stringResource(R.string.collapse_thinking) else stringResource(R.string.expand_thinking),
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
    AnimatedVisibility(visible = expanded) {
        Surface {
            SelectionContainer {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}

private fun formatResponseStats(message: Message): String {
    val totalTokens = message.responseTokens + message.thinkingTokens
    return buildString {
        append("$totalTokens tokens")
        val duration = message.responseDurationSeconds
        if (duration > 0f) {
            append(" \u00B7 ${formatDuration(duration.toInt())}")
            val speed = totalTokens / duration
            append(" \u00B7 ${"%.1f".format(speed)} tok/s")
        }
    }
}

private fun formatDuration(seconds: Int): String {
    return if (seconds < 60) {
        "${seconds}s"
    } else {
        val m = seconds / 60
        val s = seconds % 60
        "${m}m ${s}s"
    }
}

/** Friendly display name for a tool (falls back to the raw name for unknowns). */
@Composable
private fun toolDisplayName(name: String): String = when (name) {
    "run_javascript" -> stringResource(R.string.tool_run_javascript_title)
    "web_search" -> stringResource(R.string.tool_web_search_title)
    "web_fetch" -> stringResource(R.string.tool_web_fetch_title)
    else -> name
}

/** Pretty-print a JSON object/array for the tool input/output view; raw on failure. */
private fun prettyJson(raw: String): String {
    val trimmed = raw.trim()
    return try {
        when {
            trimmed.startsWith("{") -> org.json.JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> org.json.JSONArray(trimmed).toString(2)
            else -> raw
        }
    } catch (_: Exception) {
        raw
    }
}

@Composable
private fun ThinkingIndicator() {
    val transition = rememberInfiniteTransition(label = "thinking")
    val dotCount by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )
    val dots = ".".repeat(dotCount.toInt().coerceIn(0, 3))
    val thinkingText = stringResource(R.string.thinking)
    Text(
        text = "$thinkingText$dots",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        fontStyle = FontStyle.Italic
    )
}

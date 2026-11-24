package com.fulloption.app.editor

import android.os.*
import android.view.*
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.druk.ai_offline_llm.R
import com.druk.ai_offline_llm.assistant.AssistantBridge
import com.druk.ai_offline_llm.theme.PlaygroundTheme
import java.io.File

class CodeEditorFragment : Fragment() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext()).apply {
        val path = arguments?.getString("file_path")
        setContent {
            PlaygroundTheme {
                val ctx = LocalContext.current
                val file = remember { path?.let { File(it) } ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "scratch.kt") }
                var code by remember { mutableStateOf(runCatching { if (file.exists()) file.readText() else "// Full Option Code Editor\n" }.getOrDefault("")) }
                var fontSp by remember { mutableIntStateOf(14) }
                val lines = maxOf(1, code.count { it == '\n' } + 1)
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(file.name, maxLines = 1) },
                            navigationIcon = { IconButton({ findNavController().popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                            actions = {
                                IconButton({ if (fontSp > 10) fontSp -= 2 }) { Icon(Icons.Outlined.TextDecrease, null) }
                                IconButton({ if (fontSp < 24) fontSp += 2 }) { Icon(Icons.Outlined.TextIncrease, null) }
                                IconButton({ AssistantBridge.submitPrompt("ช่วยตรวจโค้ดไฟล์ ${file.name} นี้:\n```\n${code.take(5000)}\n```"); findNavController().popBackStack(R.id.nav_home, false) }) { Icon(Icons.Outlined.AutoAwesome, null) }
                                IconButton({ runCatching { file.parentFile?.mkdirs(); file.writeText(code); Toast.makeText(ctx, "บันทึกแล้ว", Toast.LENGTH_SHORT).show() } }) { Icon(Icons.Outlined.Save, null) }
                            }
                        )
                    },
                    bottomBar = {
                        Row(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().horizontalScroll(rememberScrollState()).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("    " to "TAB", "{" to "{", "}" to "}", "(" to "(", ")" to ")", ";" to ";", "=" to "=", "\"" to "\"").forEach { (ins, lbl) ->
                                FilledTonalButton({ code += ins }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text(lbl, fontFamily = FontFamily.Monospace) }
                            }
                        }
                    }
                ) { pad ->
                    Row(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
                        Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(0.5f)).padding(10.dp), horizontalAlignment = Alignment.End) {
                            for (i in 1..lines) Text("$i", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSp.sp, lineHeight = (fontSp + 6).sp))
                        }
                        Box(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(10.dp)) {
                            BasicTextField(value = code, onValueChange = { code = it }, textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSp.sp, lineHeight = (fontSp + 6).sp, color = MaterialTheme.colorScheme.onSurface), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.defaultMinSize(minWidth = 360.dp, minHeight = 500.dp))
                        }
                    }
                }
            }
        }
    }
}

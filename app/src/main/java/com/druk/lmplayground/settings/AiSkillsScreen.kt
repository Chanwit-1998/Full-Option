@file:OptIn(ExperimentalMaterial3Api::class)

package com.druk.ai_offline_llm.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.druk.ai_offline_llm.R
import com.druk.ai_offline_llm.data.SystemPromptEntity
import com.druk.llamacpp.tools.Tool

private data class PersonaPreset(
    val title: String,
    val promptText: String
)

private val DEFAULT_PRESETS = listOf(
    PersonaPreset(
        title = "ผู้ช่วยอัจฉริยะทั่วไป",
        promptText = "คุณคือ ChatLLM ผู้ช่วย AI อัจฉริยะ ตอบคำถามอย่างสุภาพ กระชับ ชัดเจน และเป็นภาษาไทยที่อ่านเข้าใจง่าย"
    ),
    PersonaPreset(
        title = "นักเขียนโค้ดและวิศวกรซอฟต์แวร์",
        promptText = "คุณคือวิศวกรซอฟต์แวร์ระดับอาวุโส ช่วยเขียนโค้ดที่สะอาด ปลอดภัย อธิบายเหตุผลสั้นๆ ตรงประเด็น และชี้จุดที่ควรระวัง"
    ),
    PersonaPreset(
        title = "นักแปลภาษาธรรมชาติ",
        promptText = "คุณคือนักแปลภาษามืออาชีพ แปลข้อความที่ส่งให้เป็นภาษาไทยหรือภาษาอังกฤษอย่างเป็นธรรมชาติ สละสลวย และรักษาความหมายเดิมครบถ้วน"
    ),
    PersonaPreset(
        title = "ติวเตอร์สรุปเข้าใจง่าย",
        promptText = "คุณคือติวเตอร์ที่อธิบายเรื่องยากให้กลายเป็นเรื่องง่าย ยกตัวอย่างในชีวิตประจำวันประกอบเสมอ"
    )
)

@Composable
fun AiSkillsScreen(
    prompts: List<SystemPromptEntity>,
    tools: List<Tool>,
    toolEnabledStates: Map<String, Boolean>,
    onBackClick: () -> Unit,
    onAddPrompt: (String) -> Unit,
    onUpdatePrompt: (id: String, text: String) -> Unit,
    onDeletePrompt: (id: String) -> Unit,
    onApplyPromptToChat: (SystemPromptEntity) -> Unit,
    onToolEnabledChanged: (String, Boolean) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.menu_ai_skills)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.tab_ai_persona)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.tab_ai_tools)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Build,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { editorTarget = EditorTarget.New }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                }
            }
        }
    ) { padding ->
        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "header_saved") {
                    Text(
                        text = stringResource(R.string.saved_personas_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (prompts.isEmpty()) {
                    item(key = "empty_saved") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.system_prompts_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(prompts, key = { it.id }) { prompt ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editorTarget = EditorTarget.Edit(prompt) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = prompt.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { editorTarget = EditorTarget.Edit(prompt) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.edit_message))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = { onApplyPromptToChat(prompt) },
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                    ) {
                                        Text(stringResource(R.string.use_in_chat))
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "header_presets") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.preset_personas_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(DEFAULT_PRESETS, key = { it.title }) { preset ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.promptText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            FilledTonalButton(
                                onClick = { onAddPrompt(preset.promptText) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.add))
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text(
                    text = stringResource(R.string.tools_section_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                ToolsContent(
                    tools = tools,
                    enabledStates = toolEnabledStates,
                    onToolEnabledChanged = onToolEnabledChanged,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    EditorBottomSheet(
        target = editorTarget,
        onAdd = onAddPrompt,
        onUpdate = onUpdatePrompt,
        onDelete = onDeletePrompt,
        onDismiss = { editorTarget = null }
    )
}

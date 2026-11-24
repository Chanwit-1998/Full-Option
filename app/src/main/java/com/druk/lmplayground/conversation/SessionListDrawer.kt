package com.druk.ai_offline_llm.conversation
import androidx.compose.foundation.shape.CircleShape

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.druk.ai_offline_llm.R
import com.druk.ai_offline_llm.data.ChatSessionEntity

@Composable
fun SessionListDrawer(
    sessions: List<ChatSessionEntity>,
    currentSessionId: String?,
    onSessionSelected: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onPinSession: (String, Boolean) -> Unit,
    onSettingsClicked: () -> Unit,
    onModelsClicked: () -> Unit = {},
    onModelStoreClicked: () -> Unit = {},
    onCartridgeStoreClicked: () -> Unit = {},
    onAiSkillsClicked: () -> Unit = {},
    onNewChatClicked: () -> Unit = {}, onFiles: () -> Unit = {}, onMedia: () -> Unit = {}, onCode: () -> Unit = {}
) {
    ModalDrawerSheet(
        modifier = Modifier.width(304.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        SessionListContent(
            sessions = sessions,
            currentSessionId = currentSessionId,
            onSessionSelected = onSessionSelected,
            onDeleteSession = onDeleteSession,
            onRenameSession = onRenameSession,
            onPinSession = onPinSession,
            onModelsClicked = onModelsClicked,
            onAiSkillsClicked = onAiSkillsClicked,
            onSettingsClicked = onSettingsClicked,
            onNewChatClicked = onNewChatClicked, onFiles = onFiles, onMedia = onMedia, onCode = onCode)
    }
}

@Composable
fun PermanentSessionList(
    sessions: List<ChatSessionEntity>,
    currentSessionId: String?,
    onSessionSelected: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onPinSession: (String, Boolean) -> Unit,
    onSettingsClicked: () -> Unit,
    onModelsClicked: () -> Unit = {},
    onModelStoreClicked: () -> Unit = {},
    onCartridgeStoreClicked: () -> Unit = {},
    onAiSkillsClicked: () -> Unit = {},
    onNewChatClicked: () -> Unit = {}, onFiles: () -> Unit = {}, onMedia: () -> Unit = {}, onCode: () -> Unit = {},
    width: Dp = 320.dp
) {
    PermanentDrawerSheet(
        modifier = Modifier.width(width),
        drawerContainerColor = Color.Transparent,
        drawerTonalElevation = 0.dp,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 12.dp, top = 4.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
        ) {
            SessionListContent(
                sessions = sessions,
                currentSessionId = currentSessionId,
                onSessionSelected = onSessionSelected,
                onDeleteSession = onDeleteSession,
                onRenameSession = onRenameSession,
                onPinSession = onPinSession,
                onModelsClicked = onModelsClicked,
                onAiSkillsClicked = onAiSkillsClicked,
                onSettingsClicked = onSettingsClicked,
                onNewChatClicked = onNewChatClicked, onFiles = onFiles, onMedia = onMedia, onCode = onCode, compact = true,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionListContent(
    sessions: List<ChatSessionEntity>,
    currentSessionId: String?,
    onSessionSelected: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onPinSession: (String, Boolean) -> Unit,
    onModelsClicked: () -> Unit,
    onAiSkillsClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onNewChatClicked: () -> Unit, onFiles: () -> Unit = {}, onMedia: () -> Unit = {}, onCode: () -> Unit = {},
    compact: Boolean = false
) {
    var contextMenuSessionId by remember { mutableStateOf<String?>(null) }
    var renameDialogSession by remember { mutableStateOf<ChatSessionEntity?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(currentSessionId, sessions.firstOrNull()?.id) {
        val index = sessions.indexOfFirst { it.id == currentSessionId }
        if (index < 0) return@LaunchedEffect
        val layout = listState.layoutInfo
        val row = layout.visibleItemsInfo.firstOrNull { it.index == index }
        val fullyVisible = row != null &&
            row.offset >= layout.viewportStartOffset &&
            row.offset + row.size <= layout.viewportEndOffset
        if (!fullyVisible) listState.animateScrollToItem(index)
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 46.dp else 56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Full Option",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DrawerNavMenuItem(
                icon = Icons.Outlined.WorkOutline,
                title = "เลือกโมเดล AI", onClick = onModelsClicked)
            DrawerNavMenuItem(icon = Icons.Outlined.Folder, title = "ตัวจัดการไฟล์", onClick = onFiles)
            DrawerNavMenuItem(icon = Icons.Outlined.MusicNote, title = "เครื่องเล่นเพลง & วิดีโอ", onClick = onMedia)
            DrawerNavMenuItem(icon = Icons.Outlined.Code, title = "เครื่องมือแก้ไขโค้ด", onClick = onCode
            DrawerNavMenuItem(
                icon = Icons.Outlined.AutoAwesome,
                title = stringResource(R.string.menu_ai_skills),
                onClick = onAiSkillsClicked
            )
            DrawerNavMenuItem(
                icon = Icons.Outlined.Settings,
                title = stringResource(R.string.settings),
                onClick = onSettingsClicked
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_chat_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        } else {
            Text(
                text = stringResource(R.string.chat_history_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
            ) {
                items(sessions, key = { it.id }) { session ->
                    val isSelected = session.id == currentSessionId
                    val showMenu = contextMenuSessionId == session.id

                    Surface(
                        color = if (isSelected)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = { onSessionSelected(session.id) },
                                onLongClick = { contextMenuSessionId = session.id }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (session.pinned) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = stringResource(R.string.pinned_label),
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { contextMenuSessionId = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (session.pinned) stringResource(R.string.unpin) else stringResource(R.string.pin)) },
                                onClick = {
                                    onPinSession(session.id, !session.pinned)
                                    contextMenuSessionId = null
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PushPin,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename)) },
                                onClick = {
                                    renameDialogSession = session
                                    contextMenuSessionId = null
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    onDeleteSession(session.id)
                                    contextMenuSessionId = null
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onNewChatClicked,
                shape = CircleShape,
                color = Color(0xFF1A68E8),
                modifier = Modifier.height(46.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.drawer_new_chat_button),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                onClick = { context.findActivity()?.finishAffinity() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.size(46.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                        contentDescription = stringResource(R.string.exit_app),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }

    renameDialogSession?.let { session ->
        var text by remember(session.id) { mutableStateOf(session.title) }
        AlertDialog(
            onDismissRequest = { renameDialogSession = null },
            title = { Text(stringResource(R.string.rename_conversation)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameSession(session.id, text.trim())
                        renameDialogSession = null
                    },
                    enabled = text.isNotBlank()
                ) {
                    Text(stringResource(R.string.rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogSession = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun DrawerNavMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@file:OptIn(ExperimentalMaterial3Api::class)

package com.druk.ai_offline_llm.cartridge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun CartridgeSlotBar(
    activeCartridge: AiCartridge?,
    onInsertCartridge: (AiCartridge) -> Unit,
    onEjectCartridge: () -> Unit,
    onOpenCartridgeStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSlotSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val accentColor = activeCartridge?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = { showSlotSheet = true },
            shape = RoundedCornerShape(16.dp),
            color = if (activeCartridge != null) accentColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            border = BorderStroke(
                width = if (activeCartridge != null) 1.5.dp else 1.dp,
                color = if (activeCartridge != null) accentColor.copy(alpha = 0.75f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (activeCartridge != null) accentColor else MaterialTheme.colorScheme.outline))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Outlined.Extension, null, tint = if (activeCartridge != null) accentColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (activeCartridge != null) "เสียบตลับ: ${activeCartridge.name}" else "ช่องเสียบตลับ: ว่าง (แตะเพื่อเสียบตลับ)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (activeCartridge != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onEjectCartridge, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Outlined.Close, "ถอดตลับ", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Surface(
            onClick = onOpenCartridgeStore,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Download, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ร้านค้าตลับ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }

    if (showSlotSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val installedList = remember(showSlotSheet) { CartridgeManager.getInstalledCartridges(context) }

        ModalBottomSheet(onDismissRequest = { showSlotSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("ช่องเสียบตลับ AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("เลือกตลับเพื่อเสียบใช้งานกับ AI ทันที", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { showSlotSheet = false; onOpenCartridgeStore() }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ร้านค้าตลับ")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (activeCartridge != null) {
                    OutlinedButton(
                        onClick = { onEjectCartridge(); showSlotSheet = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ถอดตลับปัจจุบัน (${activeCartridge.name})")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(installedList, key = { it.id }) { cart ->
                        val isInserted = activeCartridge?.id == cart.id
                        val cardAccent = Color(cart.colorHex)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isInserted) cardAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow),
                            border = BorderStroke(if (isInserted) 2.dp else 1.dp, if (isInserted) cardAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth().clickable { onInsertCartridge(cart); showSlotSheet = false }
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(cardAccent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Extension, null, tint = cardAccent)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${cart.name} (${cart.version})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(cart.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isInserted) Icon(Icons.Default.Check, "กำลังเสียบอยู่", tint = cardAccent)
                                else FilledTonalButton(onClick = { onInsertCartridge(cart); showSlotSheet = false }) { Text("เสียบตลับ") }
                            }
                        }
                    }
                }
            }
        }
    }
}

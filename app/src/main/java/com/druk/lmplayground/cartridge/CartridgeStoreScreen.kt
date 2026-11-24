@file:OptIn(ExperimentalMaterial3Api::class)

package com.druk.ai_offline_llm.cartridge

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
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
fun CartridgeStoreScreen(onBackClick: () -> Unit, onInsertAndReturnToChat: (AiCartridge) -> Unit) {
    val context = LocalContext.current
    var installedCartridges by remember { mutableStateOf(CartridgeManager.getInstalledCartridges(context)) }
    val installedIds = remember(installedCartridges) { installedCartridges.map { it.id }.toSet() }
    var inspectingCartridge by remember { mutableStateOf<AiCartridge?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ร้านค้าแพ็กข้อมูล RAG (${CartridgeManager.storeCatalog.size})") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "กลับ") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(CartridgeManager.storeCatalog, key = { it.id }) { item ->
                val isInstalled = installedIds.contains(item.id)
                val accent = Color(item.colorHex)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Extension, null, tint = accent, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${item.category} • ${item.version} • โดย ${item.author}", style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("คลังความรู้ RAG: บรรจุ ${item.knowledge.size} หัวข้ออ้างอิงเฉพาะทาง", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { inspectingCartridge = item },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.Info, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("รายละเอียด")
                            }

                            if (isInstalled) {
                                OutlinedButton(onClick = {}, enabled = false) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("รับเข้ากระเป๋าแล้ว")
                                }
                            } else {
                                FilledTonalButton(onClick = {
                                    installedCartridges = CartridgeManager.installCartridge(context, item)
                                    Toast.makeText(context, "รับตลับ \"${item.name}\" เข้ากระเป๋าแล้ว", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Outlined.Download, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("รับตลับเข้ากระเป๋า")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inspectingCartridge?.let { cart ->
        val isInstalled = installedIds.contains(cart.id)
        CartridgeDetailDialog(
            cartridge = cart,
            onDismiss = { inspectingCartridge = null },
            primaryActionLabel = if (isInstalled) null else "รับตลับเข้ากระเป๋า",
            onPrimaryAction = if (isInstalled) null else {
                {
                    installedCartridges = CartridgeManager.installCartridge(context, cart)
                    Toast.makeText(context, "รับตลับ \"${cart.name}\" เข้ากระเป๋าแล้ว", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

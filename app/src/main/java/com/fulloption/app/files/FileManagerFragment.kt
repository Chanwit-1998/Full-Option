package com.fulloption.app.files

import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.format.Formatter
import android.view.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.druk.ai_offline_llm.R
import com.druk.ai_offline_llm.theme.PlaygroundTheme
import java.io.File

class FileManagerFragment : Fragment() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext()).apply {
        setContent {
            PlaygroundTheme {
                val ctx = LocalContext.current
                val root = remember { Environment.getExternalStorageDirectory() }
                var dir by remember { mutableStateOf(root) }
                var q by remember { mutableStateOf("") }
                var tick by remember { mutableIntStateOf(0) }
                var createFolder by remember { mutableStateOf<Boolean?>(null) }
                var renameFile by remember { mutableStateOf<File?>(null) }
                val files = remember(dir, q, tick) {
                    dir.listFiles()?.filter { q.isBlank() || it.name.contains(q, true) }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })).orEmpty()
                }
                Scaffold(topBar = {
                    TopAppBar(
                        title = { Column { Text("ตัวจัดการไฟล์"); Text(dir.absolutePath, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                        navigationIcon = { IconButton({ if (dir != root && dir.parentFile != null) dir = dir.parentFile!! else findNavController().popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                        actions = {
                            IconButton({ createFolder = true }) { Icon(Icons.Outlined.CreateNewFolder, null) }
                            IconButton({ createFolder = false }) { Icon(Icons.Outlined.NoteAdd, null) }
                        }
                    )
                }) { pad ->
                    Column(Modifier.fillMaxSize().padding(pad)) {
                        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                            FilledTonalButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${ctx.packageName}"))) }, modifier = Modifier.fillMaxWidth().padding(12.dp)) { Text("อนุญาตเข้าถึงไฟล์ทั้งหมดในเครื่อง") }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("เครื่อง" to root, "Download" to File(root, "Download"), "Documents" to File(root, "Documents"), "Music" to File(root, "Music"), "Movies" to File(root, "Movies")).forEach { (t, f) ->
                                FilterChip(selected = dir == f, onClick = { if (f.exists()) dir = f }, label = { Text(t) })
                            }
                        }
                        OutlinedTextField(value = q, onValueChange = { q = it }, placeholder = { Text("ค้นหาไฟล์...") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(12.dp))
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(files, key = { it.absolutePath }) { f ->
                                var menu by remember { mutableStateOf(false) }
                                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { if (f.isDirectory) { dir = f; q = "" } else findNavController().navigate(R.id.nav_code_editor, bundleOf("file_path" to f.absolutePath)) }) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (f.isDirectory) Icons.Filled.Folder else Icons.Outlined.Description, null, tint = if (f.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(f.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(if (f.isDirectory) "${f.listFiles()?.size ?: 0} รายการ" else Formatter.formatShortFileSize(ctx, f.length()), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Box {
                                            IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, null) }
                                            DropdownMenu(menu, { menu = false }) {
                                                DropdownMenuItem(text = { Text("เปลี่ยนชื่อ") }, onClick = { menu = false; renameFile = f })
                                                DropdownMenuItem(text = { Text("ลบ") }, onClick = { menu = false; if (f.deleteRecursively()) tick++ })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                createFolder?.let { isDir ->
                    var name by remember { mutableStateOf("") }
                    AlertDialog(onDismissRequest = { createFolder = null }, title = { Text(if (isDir) "สร้างโฟลเดอร์" else "สร้างไฟล์") }, text = { OutlinedTextField(name, { name = it }, singleLine = true) },
                        confirmButton = { TextButton({ val t = File(dir, name.trim()); if (if (isDir) t.mkdirs() else t.createNewFile()) tick++; createFolder = null }) { Text("สร้าง") } })
                }
                renameFile?.let { target ->
                    var name by remember { mutableStateOf(target.name) }
                    AlertDialog(onDismissRequest = { renameFile = null }, title = { Text("เปลี่ยนชื่อ") }, text = { OutlinedTextField(name, { name = it }, singleLine = true) },
                        confirmButton = { TextButton({ if (target.renameTo(File(target.parentFile, name.trim()))) tick++; renameFile = null }) { Text("บันทึก") } })
                }
            }
        }
    }
}

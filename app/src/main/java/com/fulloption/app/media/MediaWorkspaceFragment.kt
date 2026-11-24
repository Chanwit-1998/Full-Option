package com.fulloption.app.media

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.view.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.fulloption.app.media.ui.components.LyricsEditorDialog
import com.fulloption.app.media.ui.screens.*
import com.fulloption.app.media.ui.theme.*
import com.fulloption.app.media.viewmodel.PlayerViewModel

class MediaWorkspaceFragment : Fragment() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext()).apply {
        setContent {
            GramoMediaTheme {
                Column(Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text("เครื่องเล่นเพลง & วิดีโอ") },
                        navigationIcon = { IconButton({ findNavController().popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
                    )
                    Box(Modifier.weight(1f)) { GramoMediaWorkspace() }
                }
            }
        }
    }
}

@Composable
private fun GramoMediaWorkspace(viewModel: PlayerViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val perms = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    var hasPermission by remember { mutableStateOf(perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasPermission = res.values.any { it }
        if (hasPermission) viewModel.refreshLibrary()
    }
    LaunchedEffect(hasPermission) { if (hasPermission) viewModel.refreshLibrary() else launcher.launch(perms) }

    if (!hasPermission) {
        Surface(Modifier.fillMaxSize(), color = AppBackground) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("ต้องการสิทธิ์เข้าถึงไฟล์เพลงและวิดีโอ", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = DarkIconColor, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { launcher.launch(perms) }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueAccent)) {
                    Text("อนุญาตการเข้าถึงสื่อ")
                }
            }
        }
        return
    }

    BackHandler(enabled = uiState.detailsItem != null || uiState.isFullPlayerOpen || uiState.openedAlbum != null) {
        when {
            uiState.detailsItem != null -> viewModel.closeDetails()
            uiState.isFullPlayerOpen -> viewModel.setFullPlayerOpen(false)
            uiState.openedAlbum != null -> viewModel.closeAlbum()
        }
    }

    Box(Modifier.fillMaxSize()) {
        MainLibraryScreen(
            uiState = uiState,
            onSelectTab = viewModel::selectTab,
            onToggleSearch = viewModel::toggleSearch,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSetHighlightsFilter = viewModel::setHighlightsFilter,
            onRefresh = viewModel::refreshLibrary,
            onPlayMedia = { item, list -> viewModel.playMedia(item, list, openFullPlayer = true) },
            onTogglePlayPause = viewModel::togglePlayPause,
            onOpenFullPlayer = { viewModel.setFullPlayerOpen(true) },
            onOpenDetails = viewModel::openDetails,
            onOpenAlbum = viewModel::openAlbum,
            onCloseAlbum = viewModel::closeAlbum,
            onSetCustomCoverForAlbum = viewModel::setCustomCoverForAlbum
        )
        uiState.currentItem?.let { currentItem ->
            if (uiState.isFullPlayerOpen) {
                FullPlayerScreen(
                    item = currentItem, queue = uiState.queue, exoPlayer = viewModel.exoPlayer,
                    isPlaying = uiState.isPlaying, currentPositionMs = uiState.currentPositionMs, durationMs = uiState.durationMs,
                    repeatMode = uiState.repeatMode, shuffleEnabled = uiState.shuffleEnabled, playbackSpeed = uiState.playbackSpeed,
                    sleepTimerRemainingSec = uiState.sleepTimerRemainingSec, lyricsVisible = uiState.lyricsVisible,
                    activeLyricText = uiState.activeLyricText, hasLyrics = currentItem.hasLyrics || uiState.parsedLyrics.isNotEmpty(),
                    onCollapse = { viewModel.setFullPlayerOpen(false) }, onTogglePlayPause = viewModel::togglePlayPause,
                    onSeekTo = viewModel::seekTo, onPrevious = viewModel::playPrevious, onNext = { viewModel.playNext(autoAdvance = false) },
                    onCycleRepeat = viewModel::cycleRepeatMode, onToggleShuffle = viewModel::toggleShuffle,
                    onToggleHighlight = { viewModel.toggleHighlight(currentItem) }, onToggleLyricsVisibility = viewModel::toggleLyricsVisibility,
                    onOpenDetails = { viewModel.openDetails(currentItem) }, onSelectSpeed = viewModel::setPlaybackSpeed,
                    onSelectSleepTimer = viewModel::setSleepTimerMinutes,
                    onSelectQueueItem = { qItem -> viewModel.playMedia(qItem, uiState.queue, openFullPlayer = true) }
                )
            }
        }
        uiState.detailsItem?.let { detailsItem ->
            DetailsScreen(
                item = detailsItem, details = uiState.detailsMetadata, isLoading = uiState.isLoadingDetails,
                onBack = viewModel::closeDetails, onPickCoverImage = { uri -> viewModel.setCustomCoverForItem(detailsItem, uri) },
                onRemoveCustomCover = { viewModel.removeCustomCoverForItem(detailsItem) },
                onToggleHighlight = { viewModel.toggleHighlight(detailsItem) }, onOpenLyricsEditor = { viewModel.openLyricsEditor(detailsItem) }
            )
        }
        uiState.lyricsEditorItem?.let { editorItem ->
            LyricsEditorDialog(
                item = editorItem, rawText = uiState.lyricsEditorRawText, isPlaying = uiState.isPlaying,
                currentPositionMs = uiState.currentPositionMs, onTextChange = viewModel::updateLyricsEditorText,
                onImportFileUri = viewModel::importLyricsFromUri, onStartPlaybackForSync = { viewModel.startPlaybackForSync(editorItem) },
                onTogglePlayPause = viewModel::togglePlayPause, onSave = { content -> viewModel.saveLyricsForItem(editorItem, content) },
                onDismiss = viewModel::closeLyricsEditor
            )
        }
    }
}

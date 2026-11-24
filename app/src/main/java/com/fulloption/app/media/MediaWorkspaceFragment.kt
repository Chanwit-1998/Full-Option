package com.fulloption.app.media

import android.Manifest
import android.os.*
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.druk.ai_offline_llm.theme.PlaygroundTheme
import com.fulloption.app.media.ui.GramoMediaPlayerApp

class MediaWorkspaceFragment : Fragment() {
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        permLauncher.launch(if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        return ComposeView(requireContext()).apply {
            setContent {
                PlaygroundTheme {
                    Column(Modifier.fillMaxSize()) {
                        TopAppBar(title = { Text("เครื่องเล่นเพลง & วิดีโอ") }, navigationIcon = { IconButton({ findNavController().popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
                        Box(Modifier.weight(1f)) { GramoMediaPlayerApp() }
                    }
                }
            }
        }
    }
}

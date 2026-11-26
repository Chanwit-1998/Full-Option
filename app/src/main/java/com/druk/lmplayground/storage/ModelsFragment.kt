package com.druk.ai_offline_llm.storage

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.druk.ai_offline_llm.theme.PlaygroundTheme

class ModelsFragment : Fragment() {

    private val viewModel: StorageViewModel by viewModels()

    private var pendingDownload: (() -> Unit)? = null

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.requestStorageFolderChange(uri)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingDownload?.invoke()
        pendingDownload = null
    }

    private fun withNotificationPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            action()
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted ||
            (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) &&
                    hasAskedNotificationPermission())
        ) {
            action()
        } else {
            pendingDownload = action
            setAskedNotificationPermission()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasAskedNotificationPermission(): Boolean {
        return requireContext()
            .getSharedPreferences("download_prefs", 0)
            .getBoolean("asked_notification_permission", false)
    }

    private fun setAskedNotificationPermission() {
        requireContext()
            .getSharedPreferences("download_prefs", 0)
            .edit()
            .putBoolean("asked_notification_permission", true)
            .apply()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setContent {
                PlaygroundTheme {
                    val uiState by viewModel.uiState.observeAsState()
                    // UI ของ Models (ส่วน Cartridge ถูกลบออกแล้ว)
                }
            }
        }
    }
}

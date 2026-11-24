package com.druk.ai_offline_llm.storage

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.druk.ai_offline_llm.R
import com.druk.ai_offline_llm.cartridge.CartridgeManager
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

    private var onCartridgeFolderChangedCallback: (() -> Unit)? = null
    private val cartridgeFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val count = CartridgeManager.setCustomCartridgeFolderUri(requireContext(), uri)
            Toast.makeText(
                requireContext(),
                if (count > 0) "เปลี่ยนโฟลเดอร์ตลับและนำเข้า $count ตลับแล้ว" else "เปลี่ยนโฟลเดอร์เก็บตลับแล้ว",
                Toast.LENGTH_SHORT
            ).show()
            onCartridgeFolderChangedCallback?.invoke()
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

        if (granted || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS).not()
            && hasAskedNotificationPermission()
        ) {
            action()
        } else {
            pendingDownload = action
            setAskedNotificationPermission()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasAskedNotificationPermission(): Boolean {
        return requireContext().getSharedPreferences("download_prefs", 0)
            .getBoolean("asked_notification_permission", false)
    }

    private fun setAskedNotificationPermission() {
        requireContext().getSharedPreferences("download_prefs", 0)
            .edit().putBoolean("asked_notification_permission", true).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadStorageInfo()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        val mode = when (arguments?.getString("mode")) {
            "downloaded" -> ModelsScreenMode.DOWNLOADED_ONLY
            "store" -> ModelsScreenMode.STORE_ONLY
            else -> ModelsScreenMode.ALL
        }
        val activeModelFilename = arguments?.getString("active_model_filename")

        setContent {
            val storageInfo by viewModel.storageInfo.observeAsState()
            val allModels by viewModel.allModels.observeAsState(emptyList())
            val embeddingModel by viewModel.embeddingModel.observeAsState()
            val dictationModel by viewModel.dictationModel.observeAsState()
            val downloadingProgress by viewModel.downloadingModels.observeAsState(emptyMap())
            val snackbarMessage by viewModel.snackbarMessage.observeAsState()
            val pendingMigration by viewModel.pendingMigration.observeAsState()
            val migrationProgress by viewModel.migrationProgress.observeAsState()
            var currentActiveFilename by remember { mutableStateOf(activeModelFilename) }
            var installedCartridges by remember { mutableStateOf(CartridgeManager.getInstalledCartridges(requireContext())) }
            var activeCartridgeId by remember { mutableStateOf(CartridgeManager.getActiveCartridgeId(requireContext())) }
            var cartridgeFolderPath by remember { mutableStateOf(CartridgeManager.getCartridgeFolderPath(requireContext())) }
            onCartridgeFolderChangedCallback = {
                cartridgeFolderPath = CartridgeManager.getCartridgeFolderPath(requireContext())
                installedCartridges = CartridgeManager.getInstalledCartridges(requireContext())
            }

            val prevDownloadCount = remember { androidx.compose.runtime.mutableIntStateOf(downloadingProgress.size) }
            LaunchedEffect(downloadingProgress.size) {
                if (downloadingProgress.size < prevDownloadCount.intValue) {
                    viewModel.loadStorageInfo()
                }
                prevDownloadCount.intValue = downloadingProgress.size
            }

            PlaygroundTheme {
                ModelsScreen(
                    storageInfo = storageInfo,
                    allModels = allModels,
                    downloadingModels = downloadingProgress,
                    snackbarMessage = snackbarMessage,
                    pendingMigration = pendingMigration,
                    migrationProgress = migrationProgress,
                    deviceLanguage = viewModel.deviceLanguage,
                    mode = mode,
                    activeModelFilename = currentActiveFilename,
                    installedCartridges = installedCartridges,
                    activeCartridgeId = activeCartridgeId,
                    cartridgeFolderPath = cartridgeFolderPath,
                    onChangeCartridgeFolderClick = {
                        runCatching { cartridgeFolderPickerLauncher.launch(null) }
                            .onFailure { Toast.makeText(requireContext(), R.string.folder_picker_unavailable, Toast.LENGTH_SHORT).show() }
                    },
                    onInsertCartridge = { cart ->
                        CartridgeManager.setActiveCartridgeId(requireContext(), cart.id)
                        activeCartridgeId = cart.id
                        val nav = findNavController()
                        nav.previousBackStackEntry?.savedStateHandle?.set("insert_cartridge_id", cart.id)
                        nav.popBackStack()
                    },
                    onEjectCartridge = {
                        CartridgeManager.setActiveCartridgeId(requireContext(), null)
                        activeCartridgeId = null
                        val nav = findNavController()
                        nav.previousBackStackEntry?.savedStateHandle?.set("eject_cartridge_requested", true)
                        Toast.makeText(requireContext(), "ถอดตลับออกแล้ว", Toast.LENGTH_SHORT).show()
                    },
                    onInstallCustomCartridge = { newCart ->
                        installedCartridges = CartridgeManager.installCartridge(requireContext(), newCart)
                    },
                    onRemoveCartridge = { cartId ->
                        installedCartridges = CartridgeManager.removeInstalledCartridge(requireContext(), cartId)
                        if (activeCartridgeId == cartId) {
                            activeCartridgeId = null
                            findNavController().previousBackStackEntry?.savedStateHandle?.set("eject_cartridge_requested", true)
                        }
                    },
                    onOpenCartridgeStoreClick = {
                        val nav = findNavController()
                        if (nav.currentDestination?.id == R.id.nav_models) {
                            nav.navigate(R.id.action_models_to_cartridge_store)
                        }
                    },
                    onSelectModel = { model ->
                        val nav = findNavController()
                        nav.previousBackStackEntry?.savedStateHandle?.set(
                            "selected_model_filename",
                            model.filename
                        )
                        nav.popBackStack()
                    },
                    onUnloadModel = {
                        currentActiveFilename = null
                        viewModel.unloadModel()
                        val nav = findNavController()
                        nav.previousBackStackEntry?.savedStateHandle?.set(
                            "unload_model_requested",
                            true
                        )
                        runCatching {
                            nav.getBackStackEntry(R.id.nav_home).savedStateHandle.set(
                                "unload_model_requested",
                                true
                            )
                        }
                    },
                    onOpenStoreClick = {
                        val nav = findNavController()
                        if (nav.currentDestination?.id == R.id.nav_models) {
                            nav.navigate(R.id.nav_models, bundleOf("mode" to "store"))
                        }
                    },
                    onBackClick = { findNavController().popBackStack() },
                    onChangeFolderClick = {
                        try {
                            folderPickerLauncher.launch(null)
                        } catch (_: ActivityNotFoundException) {
                            val fallbackUri = StorageDocuments.appStorageFallbackUri(requireContext())
                            if (fallbackUri != null) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.storage_fallback_used,
                                    Toast.LENGTH_LONG,
                                ).show()
                                viewModel.requestStorageFolderChange(fallbackUri)
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.folder_picker_unavailable,
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                    onDeleteModel = { model ->
                        viewModel.deleteModel(model)
                    },
                    onDownloadModel = { model, includeMmproj ->
                        withNotificationPermission { viewModel.downloadModel(model, includeMmproj) }
                    },
                    onDownloadMmproj = { model ->
                        withNotificationPermission { viewModel.downloadMmprojOnly(model) }
                    },
                    onCancelDownload = { model ->
                        viewModel.cancelDownload(model)
                    },
                    onSnackbarDismiss = {
                        viewModel.clearSnackbar()
                    },
                    onConfirmMigration = {
                        viewModel.confirmMigration()
                    },
                    onSkipMigration = {
                        viewModel.skipMigration()
                    },
                    onCancelMigration = {
                        viewModel.cancelMigration()
                    },
                    embeddingModel = embeddingModel,
                    dictationModel = dictationModel
                )
            }
        }
    }
}

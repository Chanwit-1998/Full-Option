package com.druk.ai_offline_llm.cartridge

import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.druk.ai_offline_llm.theme.PlaygroundTheme

class CartridgeStoreFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setContent {
                PlaygroundTheme {
                    CartridgeStoreScreen(
                        onBackClick = { findNavController().navigateUp() },
                        onInsertAndReturnToChat = { cartridge ->
                            findNavController().previousBackStackEntry?.savedStateHandle?.set("insert_cartridge_id", cartridge.id)
                            findNavController().navigateUp()
                        }
                    )
                }
            }
        }
}

package com.druk.ai_offline_llm.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.druk.ai_offline_llm.theme.PlaygroundTheme

class AiSkillsFragment : Fragment() {

    private val systemPromptsViewModel: SystemPromptsViewModel by viewModels()
    private val toolsViewModel: ToolsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        setContent {
            PlaygroundTheme {
                val prompts by systemPromptsViewModel.prompts.observeAsState(emptyList())
                val toolsEnabled by toolsViewModel.enabled.collectAsStateWithLifecycle()

                AiSkillsScreen(
                    prompts = prompts,
                    tools = toolsViewModel.tools,
                    toolEnabledStates = toolsEnabled,
                    onBackClick = { findNavController().popBackStack() },
                    onAddPrompt = systemPromptsViewModel::addPrompt,
                    onUpdatePrompt = systemPromptsViewModel::updatePrompt,
                    onDeletePrompt = systemPromptsViewModel::deletePrompt,
                    onApplyPromptToChat = { prompt ->
                        val nav = findNavController()
                        nav.previousBackStackEntry?.savedStateHandle?.set(
                            "apply_system_prompt_id",
                            prompt.id
                        )
                        nav.previousBackStackEntry?.savedStateHandle?.set(
                            "apply_system_prompt_text",
                            prompt.text
                        )
                        nav.popBackStack()
                    },
                    onToolEnabledChanged = { name, enabled ->
                        toolsViewModel.setEnabled(name, enabled)
                    }
                )
            }
        }
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

package com.druk.ai_offline_llm.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.druk.ai_offline_llm.R
import com.druk.ai_offline_llm.theme.PlaygroundTheme
import java.util.Locale

private enum class SettingsDetail { Models, Prompts, Language, Tools, SoundHaptic, Advanced, PrivacyPolicy, OpenSourceLicenses, Faq }

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onModelsClick: () -> Unit,
    onSystemPromptsClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onOpenSourceLicensesClick: () -> Unit = {},
    onFaqClick: () -> Unit,
    onToolsClick: () -> Unit,
    onSoundHapticClick: () -> Unit,
    onAdvancedClick: () -> Unit,
    onSendFeedbackClick: () -> Unit,
    appVersion: String,
    modelsDetailContent: (@Composable () -> Unit)? = null,
    promptsDetailContent: (@Composable () -> Unit)? = null,
    languageDetailContent: (@Composable () -> Unit)? = null,
    toolsDetailContent: (@Composable () -> Unit)? = null,
    soundHapticDetailContent: (@Composable () -> Unit)? = null,
    advancedDetailContent: (@Composable () -> Unit)? = null,
    initialDetailKey: String? = null,
    masterWidth: androidx.compose.ui.unit.Dp = 320.dp,
    onCrashEngineClick: (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val twoPane = configuration.screenWidthDp >= 840

    var detail by remember {
        mutableStateOf<SettingsDetail?>(
            when (initialDetailKey) {
                "tools" -> SettingsDetail.Tools
                else -> null
            }
        )
    }

    if (twoPane) {
        Row(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.width(masterWidth),
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.settings)) },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        }
                    )
                }
            ) { masterPadding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(masterPadding)
                        .padding(start = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                ) {
                    SettingsList(
                        appVersion = appVersion,
                        selectedDetail = detail,
                        onModelsClick = {
                            if (modelsDetailContent != null) {
                                detail = SettingsDetail.Models
                            } else {
                                onModelsClick()
                            }
                        },
                        onSystemPromptsClick = {
                            if (promptsDetailContent != null) {
                                detail = SettingsDetail.Prompts
                            } else {
                                onSystemPromptsClick()
                            }
                        },
                        onLanguageClick = {
                            if (languageDetailContent != null) {
                                detail = SettingsDetail.Language
                            } else {
                                onLanguageClick()
                            }
                        },
                        onToolsClick = {
                            if (toolsDetailContent != null) {
                                detail = SettingsDetail.Tools
                            } else {
                                onToolsClick()
                            }
                        },
                        onSoundHapticClick = {
                            if (soundHapticDetailContent != null) {
                                detail = SettingsDetail.SoundHaptic
                            } else {
                                onSoundHapticClick()
                            }
                        },
                        onAdvancedClick = {
                            if (advancedDetailContent != null) {
                                detail = SettingsDetail.Advanced
                            } else {
                                onAdvancedClick()
                            }
                        },
                        onPrivacyPolicyClick = { detail = SettingsDetail.PrivacyPolicy },
                        onOpenSourceLicensesClick = { detail = SettingsDetail.OpenSourceLicenses },
                        onFaqClick = { detail = SettingsDetail.Faq },
                        onCrashEngineClick = onCrashEngineClick
                    )
                }
            }

            val detailTitle = when (detail) {
                SettingsDetail.Models -> stringResource(R.string.models)
                SettingsDetail.Prompts -> stringResource(R.string.system_prompts)
                SettingsDetail.Language -> stringResource(R.string.language)
                SettingsDetail.Tools -> stringResource(R.string.tools)
                SettingsDetail.SoundHaptic -> stringResource(R.string.sound_and_haptic)
                SettingsDetail.Advanced -> stringResource(R.string.advanced)
                SettingsDetail.PrivacyPolicy -> stringResource(R.string.privacy_policy)
                SettingsDetail.OpenSourceLicenses -> "ใบอนุญาตโอเพนซอร์ส"
                SettingsDetail.Faq -> stringResource(R.string.faq)
                null -> ""
            }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(title = { Text(detailTitle) })
                }
            ) { detailPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(detailPadding)
                ) {
                    when (detail) {
                        SettingsDetail.Models -> modelsDetailContent?.invoke()
                        SettingsDetail.Prompts -> promptsDetailContent?.invoke()
                        SettingsDetail.Language -> languageDetailContent?.invoke()
                        SettingsDetail.Tools -> toolsDetailContent?.invoke()
                        SettingsDetail.SoundHaptic -> soundHapticDetailContent?.invoke()
                        SettingsDetail.Advanced -> advancedDetailContent?.invoke()
                        SettingsDetail.PrivacyPolicy ->
                            PrivacyPolicyContent(onSendFeedbackClick = onSendFeedbackClick)
                        SettingsDetail.OpenSourceLicenses -> OpenSourceLicensesContent()
                        SettingsDetail.Faq -> FaqContent()
                        null -> Unit
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            SettingsList(
                appVersion = appVersion,
                selectedDetail = null,
                onModelsClick = onModelsClick,
                onSystemPromptsClick = onSystemPromptsClick,
                onLanguageClick = onLanguageClick,
                onToolsClick = onToolsClick,
                onSoundHapticClick = onSoundHapticClick,
                onAdvancedClick = onAdvancedClick,
                onPrivacyPolicyClick = onPrivacyPolicyClick,
                onOpenSourceLicensesClick = onOpenSourceLicensesClick,
                onFaqClick = onFaqClick,
                onCrashEngineClick = onCrashEngineClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

@Composable
private fun SettingsList(
    appVersion: String,
    selectedDetail: SettingsDetail?,
    onModelsClick: () -> Unit,
    onSystemPromptsClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onToolsClick: () -> Unit,
    onSoundHapticClick: () -> Unit,
    onAdvancedClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onOpenSourceLicensesClick: () -> Unit,
    onFaqClick: () -> Unit,
    onCrashEngineClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val usageBlueTint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val usageBlueBg = usageBlueTint.copy(alpha = 0.14f)

    val infoNeutralTint = MaterialTheme.colorScheme.onSurfaceVariant
    val infoNeutralBg = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)

    val debugOrangeTint = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
    val debugOrangeBg = debugOrangeTint.copy(alpha = 0.14f)

    val currentTag = currentLanguageTag()
    val isThai = remember(currentTag) {
        val lang = currentTag ?: Locale.getDefault().language
        lang.startsWith("th", ignoreCase = true)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. การใช้งานและประสิทธิภาพ (Usage & Performance)
        SettingsGroupCard(
            title = if (isThai) "การใช้งานและประสิทธิภาพ" else "Usage & Performance"
        ) {
            SettingsRow(
                icon = Icons.Outlined.VolumeUp,
                title = stringResource(R.string.sound_and_haptic),
                subtitle = stringResource(R.string.sound_and_haptic_subtitle),
                selected = selectedDetail == SettingsDetail.SoundHaptic,
                iconTint = usageBlueTint,
                iconBgColor = usageBlueBg,
                onClick = onSoundHapticClick
            )
            CardItemDivider()
            SettingsRow(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.advanced),
                subtitle = stringResource(R.string.advanced_subtitle),
                selected = selectedDetail == SettingsDetail.Advanced,
                iconTint = usageBlueTint,
                iconBgColor = usageBlueBg,
                onClick = onAdvancedClick
            )
            CardItemDivider()
            val languageSubtitle = if (currentTag == null) {
                stringResource(R.string.language_system_default)
            } else {
                Locale.forLanguageTag(currentTag).let { locale ->
                    locale.getDisplayName(locale)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                }
            }
            SettingsRow(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.language),
                subtitle = languageSubtitle,
                selected = selectedDetail == SettingsDetail.Language,
                iconTint = usageBlueTint,
                iconBgColor = usageBlueBg,
                onClick = onLanguageClick
            )
        }

        // 2. ข้อมูลและความช่วยเหลือ (Information & Help)
        SettingsGroupCard(
            title = if (isThai) "ข้อมูลและความช่วยเหลือ" else "Information & Help"
        ) {
            SettingsRow(
                icon = Icons.Outlined.HelpOutline,
                title = stringResource(R.string.faq),
                subtitle = stringResource(R.string.faq_subtitle),
                selected = selectedDetail == SettingsDetail.Faq,
                iconTint = infoNeutralTint,
                iconBgColor = infoNeutralBg,
                onClick = onFaqClick
            )
            CardItemDivider()
            SettingsRow(
                icon = Icons.Outlined.Policy,
                title = stringResource(R.string.privacy_policy),
                selected = selectedDetail == SettingsDetail.PrivacyPolicy,
                iconTint = infoNeutralTint,
                iconBgColor = infoNeutralBg,
                onClick = onPrivacyPolicyClick
            )
            CardItemDivider()
            SettingsRow(
                icon = Icons.Outlined.Description,
                title = "ใบอนุญาตโอเพนซอร์ส",
                selected = selectedDetail == SettingsDetail.OpenSourceLicenses,
                iconTint = infoNeutralTint,
                iconBgColor = infoNeutralBg,
                onClick = onOpenSourceLicensesClick
            )
        }

        // 3. สำหรับนักพัฒนา (Debug)
        if (onCrashEngineClick != null) {
            SettingsGroupCard(
                title = if (isThai) "สำหรับนักพัฒนา (Debug)" else "Developer (Debug)"
            ) {
                SettingsRow(
                    icon = Icons.Outlined.BugReport,
                    title = stringResource(R.string.debug_crash_engine_title),
                    subtitle = stringResource(R.string.debug_crash_engine_subtitle),
                    iconTint = debugOrangeTint,
                    iconBgColor = debugOrangeBg,
                    onClick = onCrashEngineClick
                )
            }
        }

        // 4. Footer Version & Developer Credit (รูป D3)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.version, appVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = "©︎ 2026 Chanwit™",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun CardItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    selected: Boolean = false,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconBgColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    PlaygroundTheme {
        SettingsScreen(
            onBackClick = {},
            onModelsClick = {},
            onSystemPromptsClick = {},
            onLanguageClick = {},
            onToolsClick = {},
            onSoundHapticClick = {},
            onAdvancedClick = {},
            onPrivacyPolicyClick = {},
            onFaqClick = {},
            onSendFeedbackClick = {},
            appVersion = "1.0.0"
        )
    }
}

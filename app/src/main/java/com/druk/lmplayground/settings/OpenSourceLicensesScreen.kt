@file:OptIn(ExperimentalMaterial3Api::class)

package com.druk.ai_offline_llm.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.druk.ai_offline_llm.R

private data class OpenSourceLibraryItem(
    val name: String,
    val licenseBadge: String,
    val subtitle: String,
    val licenseDetail: String,
)

private val aiEngineLibraries = listOf(
    OpenSourceLibraryItem(
        name = "llama.cpp",
        licenseBadge = "MIT",
        subtitle = "เอนจินประมวลผลข้อความ LLM",
        licenseDetail = "MIT License\nCopyright (c) 2023-2026 Georgi Gerganov and llama.cpp contributors\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files, to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software."
    ),
    OpenSourceLibraryItem(
        name = "stable-diffusion.cpp",
        licenseBadge = "MIT",
        subtitle = "เอนจินสร้างรูปภาพแบบออฟไลน์",
        licenseDetail = "MIT License\nCopyright (c) 2023-2026 leejet and stable-diffusion.cpp contributors\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files, to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software."
    ),
    OpenSourceLibraryItem(
        name = "Speech / Audio Engine",
        licenseBadge = "MIT",
        subtitle = "ระบบแปลงเสียงพูดเป็นข้อความ",
        licenseDetail = "MIT License\nCopyright (c) 2026 ChatLLM Audio & Speech Integration\n\nOn-device speech recognition, PCM audio capture, and text-to-speech processing module operating offline without network transmission."
    ),
)

private val uiFrameworkLibraries = listOf(
    OpenSourceLibraryItem(
        name = "LM Playground",
        licenseBadge = "Apache 2.0",
        subtitle = "โครงสร้างพื้นฐานแอปพลิเคชัน",
        licenseDetail = "Apache License, Version 2.0\nCopyright (c) Andrii Druk and LM Playground contributors\n\nLicensed under the Apache License, Version 2.0. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0"
    ),
    OpenSourceLibraryItem(
        name = "Jetpack Compose",
        licenseBadge = "Apache 2.0",
        subtitle = "ระบบสร้างหน้าจอ Material 3",
        licenseDetail = "Apache License, Version 2.0\nCopyright (c) The Android Open Source Project\n\nLicensed under the Apache License, Version 2.0. Modern declarative UI toolkit for Android Material Design 3."
    ),
    OpenSourceLibraryItem(
        name = "Coil & Haze",
        licenseBadge = "Apache 2.0",
        subtitle = "จัดการรูปภาพและเอฟเฟกต์เบลอ",
        licenseDetail = "Apache License, Version 2.0\nCopyright (c) Coil Contributors & Chris Banes\n\nLicensed under the Apache License, Version 2.0. Local image loading and glassmorphism visual effects for Jetpack Compose."
    ),
)

@Composable
fun OpenSourceLicensesScreen(
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ใบอนุญาตโอเพนซอร์ส") },
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
        OpenSourceLicensesContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
fun OpenSourceLicensesContent(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ข้อมูลลิขสิทธิ์แอปพลิเคชัน ChatLLM",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "พัฒนาแอปพลิเคชันโดย © 2026 Chanwit™",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "แอปนี้ทำงานแบบออฟไลน์ 100% โดยไม่มีการเข้าถึงอินเทอร์เน็ต และพัฒนาต่อยอดโดยใช้ไลบรารีโอเพนซอร์สตามรายการด้านล่าง",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LicenseSectionCard(
            sectionTitle = "เอนจินประมวลผล AI",
            libraries = aiEngineLibraries,
            cardColor = cardColor,
            borderColor = borderColor
        )

        LicenseSectionCard(
            sectionTitle = "โครงสร้างแอปและส่วนติดต่อผู้ใช้",
            libraries = uiFrameworkLibraries,
            cardColor = cardColor,
            borderColor = borderColor
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun LicenseSectionCard(
    sectionTitle: String,
    libraries: List<OpenSourceLibraryItem>,
    cardColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = sectionTitle,
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
            Column {
                libraries.forEachIndexed { index, item ->
                    ExpandableLicenseRow(item = item)
                    if (index < libraries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = borderColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableLicenseRow(item: OpenSourceLibraryItem) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "license_arrow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = item.licenseBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.rotate(arrowRotation)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
            ) {
                Text(
                    text = item.licenseDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

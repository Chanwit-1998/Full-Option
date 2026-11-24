@file:OptIn(ExperimentalMaterial3Api::class)

package com.druk.ai_offline_llm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.druk.ai_offline_llm.theme.PlaygroundTheme

@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    colors: TopAppBarColors? = null,
    showNavIcon: Boolean = true,
    expandedHeight: Dp = Dp.Unspecified,
    onNavIconPressed: () -> Unit = { },
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        actions = actions,
        title = title,
        scrollBehavior = scrollBehavior,
        colors = colors ?: TopAppBarDefaults.centerAlignedTopAppBarColors(),
        expandedHeight = if (expandedHeight != Dp.Unspecified) {
            expandedHeight
        } else {
            TopAppBarDefaults.TopAppBarExpandedHeight
        },
        navigationIcon = {
            if (showNavIcon) {
                val desc = stringResource(id = R.string.navigation_drawer_open)
                val lineColor = MaterialTheme.colorScheme.onSurface
                IconButton(
                    onClick = onNavIconPressed,
                    modifier = Modifier.semantics { contentDescription = desc }
                ) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        val stroke = 2.2.dp.toPx()
                        val startX = 2.dp.toPx()
                        val topY = size.height * 0.34f
                        val bottomY = size.height * 0.66f
                        drawLine(
                            color = lineColor,
                            start = Offset(startX, topY),
                            end = Offset(size.width - 2.dp.toPx(), topY),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = lineColor,
                            start = Offset(startX, bottomY),
                            end = Offset(size.width * 0.62f, bottomY),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AppBarPreview() {
    PlaygroundTheme {
        AppBar(title = { Text("Preview!") })
    }
}

@Preview
@Composable
fun AppBarPreviewDark() {
    PlaygroundTheme(isDarkTheme = true) {
        AppBar(title = { Text("Preview!") })
    }
}

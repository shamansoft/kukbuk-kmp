package net.shamansoft.kukbuk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific debug menu for switching data sources.
 * Only shown in debug builds on supported platforms (Android).
 */
@Composable
expect fun DebugDataSourceSection(modifier: Modifier = Modifier)

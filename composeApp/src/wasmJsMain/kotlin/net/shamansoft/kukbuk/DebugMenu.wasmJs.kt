package net.shamansoft.kukbuk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * WASM implementation - debug menu not available on web.
 */
@Composable
actual fun DebugDataSourceSection(modifier: Modifier) {
    // No debug menu on WASM
}

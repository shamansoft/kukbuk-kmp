package net.shamansoft.kukbuk

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.shamansoft.kukbuk.settings.SettingsViewModel

/**
 * Settings screen - app configuration and cache management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel
) {
    val settingsState by viewModel.settingsState.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("◀", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Offline Cache Section
            item {
                CacheSectionHeader()
            }

            item {
                CacheInfoCard(
                    cachedRecipeCount = settingsState.cachedRecipeCount,
                    isLoading = settingsState.isLoading
                )
            }

            item {
                ClearCacheButton(
                    enabled = settingsState.cachedRecipeCount > 0 && !settingsState.isClearing,
                    isClearing = settingsState.isClearing,
                    onClick = { showClearConfirmDialog = true }
                )
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                AboutSection()
            }
        }

        // Success Snackbar
        if (settingsState.showClearSuccess) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                viewModel.dismissSuccessMessage()
            }
        }

        // Error Snackbar
        settingsState.error?.let { error ->
            LaunchedEffect(error) {
                kotlinx.coroutines.delay(3000)
                viewModel.dismissError()
            }
        }
    }

    // Confirmation Dialog
    if (showClearConfirmDialog) {
        ClearCacheConfirmDialog(
            onConfirm = {
                viewModel.clearCache()
                showClearConfirmDialog = false
            },
            onDismiss = {
                showClearConfirmDialog = false
            }
        )
    }
}

@Composable
private fun CacheSectionHeader() {
    Text(
        text = "Offline Cache",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun CacheInfoCard(
    cachedRecipeCount: Int,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📦",
                    fontSize = 24.sp
                )
                Text(
                    text = "Cached Recipes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "$cachedRecipeCount recipe${if (cachedRecipeCount != 1) "s" else ""} available offline",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Up to 100 most recently viewed recipes are cached for offline access",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ClearCacheButton(
    enabled: Boolean,
    isClearing: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        if (isClearing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onError
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (isClearing) "Clearing..." else "Clear Offline Cache",
            color = MaterialTheme.colorScheme.onError
        )
    }
}

@Composable
private fun ClearCacheConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Clear Offline Cache?")
        },
        text = {
            Text("This will remove all cached recipes from your device. You'll need internet connection to view them again.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AboutSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "About",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Kukbuk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Version 1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "A recipe management app with offline support",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

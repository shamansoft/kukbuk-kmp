package net.shamansoft.kukbuk

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.shamansoft.kukbuk.cache.RecipeCache
import net.shamansoft.kukbuk.recipe.RecipeRepository
import net.shamansoft.kukbuk.util.Logger
import org.koin.compose.koinInject

/**
 * Android implementation of the debug data source section.
 * Only shown in debug builds.
 */
@Composable
actual fun DebugDataSourceSection(modifier: Modifier) {
    if (!isDebugBuild()) return

    val recipeCache: RecipeCache = koinInject()
    val recipeRepository: RecipeRepository = koinInject()
    val scope = rememberCoroutineScope()
    var currentMode by remember { mutableStateOf(DataSourceConfig.mode) }
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🔧 Debug: Data Source",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                // Clear both persistent and in-memory caches before switching
                                Logger.d("DebugMenu", "Clearing caches before switching to LOCAL")
                                recipeCache.clearCache()
                                recipeRepository.clearCache()

                                DataSourceConfig.mode = DataSourceMode.LOCAL
                                currentMode = DataSourceMode.LOCAL

                                // Restart the app to load from new data source
                                (context as? Activity)?.recreate()
                            } catch (e: Exception) {
                                Logger.e("DebugMenu", "Failed to clear caches: ${e.message}")
                            }
                        }
                    },
                    enabled = currentMode != DataSourceMode.LOCAL,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Local Files", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                // Clear both persistent and in-memory caches before switching
                                Logger.d("DebugMenu", "Clearing caches before switching to PRODUCTION")
                                recipeCache.clearCache()
                                recipeRepository.clearCache()

                                DataSourceConfig.mode = DataSourceMode.PRODUCTION
                                currentMode = DataSourceMode.PRODUCTION

                                // Restart the app to load from new data source
                                (context as? Activity)?.recreate()
                            } catch (e: Exception) {
                                Logger.e("DebugMenu", "Failed to clear caches: ${e.message}")
                            }
                        }
                    },
                    enabled = currentMode != DataSourceMode.PRODUCTION,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Google Drive", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Current: ${currentMode.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Note: Switching will clear all caches",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

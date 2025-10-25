package net.shamansoft.kukbuk

/**
 * Configuration for selecting the data source.
 *
 * To switch between local and production modes:
 * 1. Update gradle.properties: kukbuk.dataSource=local or kukbuk.dataSource=production
 * 2. Or set DataSourceConfig.mode directly in App.kt for testing
 */
object DataSourceConfig {
    /**
     * Current data source mode.
     * - LOCAL: Use local file system for recipes (development)
     * - PRODUCTION: Use Google Drive for recipes (default)
     */
    var mode: DataSourceMode = getDefaultDataSourceMode()

    fun isLocalMode(): Boolean = mode == DataSourceMode.LOCAL
    fun isProductionMode(): Boolean = mode == DataSourceMode.PRODUCTION
}

enum class DataSourceMode {
    LOCAL,
    PRODUCTION
}

/**
 * Returns the default data source mode.
 * For local development, this returns LOCAL.
 * For production builds, this returns PRODUCTION.
 */
private fun getDefaultDataSourceMode(): DataSourceMode {
    // In debug builds with local recipes path available, default to LOCAL
    // Otherwise, use PRODUCTION
    return if (isDebugBuild() && getLocalRecipesPath() != null) {
        DataSourceMode.LOCAL
    } else {
        DataSourceMode.PRODUCTION
    }
}

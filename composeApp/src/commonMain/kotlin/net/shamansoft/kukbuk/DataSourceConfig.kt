package net.shamansoft.kukbuk

/**
 * Configuration for data source.
 *
 * Currently only PRODUCTION (Google Drive) is supported.
 * Local file development mode has been disabled.
 */
object DataSourceConfig {
    /**
     * Current data source mode - always PRODUCTION (Google Drive).
     */
    val mode: DataSourceMode = DataSourceMode.PRODUCTION

    fun isLocalMode(): Boolean = false
    fun isProductionMode(): Boolean = true
}

enum class DataSourceMode {
    PRODUCTION
}

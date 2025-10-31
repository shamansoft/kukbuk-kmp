package net.shamansoft.kukbuk.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific database driver factory.
 * Each platform provides its own implementation using the appropriate SQLite driver.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

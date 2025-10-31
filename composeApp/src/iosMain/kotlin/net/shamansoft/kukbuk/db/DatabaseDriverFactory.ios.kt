package net.shamansoft.kukbuk.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS implementation of DatabaseDriverFactory.
 * Uses NativeSqliteDriver for native SQLite access on iOS.
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = RecipeDatabase.Schema,
            name = "recipe_cache.db"
        )
    }
}

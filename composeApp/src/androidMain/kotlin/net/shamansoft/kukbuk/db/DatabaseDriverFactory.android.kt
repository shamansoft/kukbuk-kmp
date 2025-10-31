package net.shamansoft.kukbuk.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android implementation of DatabaseDriverFactory.
 * Uses AndroidSqliteDriver for native SQLite access on Android.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = RecipeDatabase.Schema,
            context = context,
            name = "recipe_cache.db"
        )
    }
}

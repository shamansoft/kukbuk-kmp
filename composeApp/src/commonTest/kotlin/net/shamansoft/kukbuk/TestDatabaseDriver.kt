package net.shamansoft.kukbuk

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import net.shamansoft.kukbuk.db.RecipeDatabase

/**
 * Creates an in-memory SQLite database for testing.
 */
fun createTestDatabaseDriver(): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    RecipeDatabase.Schema.create(driver)
    return driver
}

/**
 * Creates a test RecipeDatabase instance.
 */
fun createTestDatabase(): RecipeDatabase {
    return RecipeDatabase(createTestDatabaseDriver())
}

/**
 * Test database wrapper that allows closing the driver.
 */
class TestDatabase {
    val driver: SqlDriver = createTestDatabaseDriver()
    val database: RecipeDatabase = RecipeDatabase(driver)

    fun close() {
        driver.close()
    }
}

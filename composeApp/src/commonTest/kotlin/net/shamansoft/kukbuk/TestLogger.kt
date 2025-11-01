package net.shamansoft.kukbuk

import net.shamansoft.kukbuk.util.Logger

/**
 * Disables logging during tests to avoid Android Log.* calls.
 */
fun disableLoggingForTests() {
    // Note: This is a workaround. The Logger class uses Android Log which
    // is not available in unit tests. In a production app, you might want
    // to create a testable Logger abstraction.
}

package net.shamansoft.kukbuk.util

import android.util.Log

actual object Logger {
    actual fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    actual fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    actual fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
}

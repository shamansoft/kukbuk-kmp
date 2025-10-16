package net.shamansoft.kukbuk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {

    // Register for activity result BEFORE onCreate
    private val signInLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("Kukbuk", "Sign-in activity result: ${result.resultCode}")
        // Result will be handled by the callback set by AuthenticationService
        signInResultCallback?.invoke(result.data)
    }

    // Callback to be set by AuthenticationService
    var signInResultCallback: ((Intent?) -> Unit)? = null

    fun getSignInLauncher(): ActivityResultLauncher<Intent> = signInLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Kukbuk", "========================================")
        Log.d("Kukbuk", "MainActivity onCreate() called")
        Log.d("Kukbuk", "========================================")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            Log.d("Kukbuk", "setContent called, loading AndroidApp")
            AndroidApp()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
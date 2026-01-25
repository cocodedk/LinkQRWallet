package com.cocode.linkqrwallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import com.cocode.linkqrwallet.ui.theme.LinkQRWalletTheme
import com.cocode.linkqrwallet.ui.LinkQrWalletRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinkQRWalletTheme {
                LinkQrWalletRoot(sharedUrl = extractSharedUrl(intent))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setContent {
            LinkQRWalletTheme {
                LinkQrWalletRoot(sharedUrl = extractSharedUrl(intent))
            }
        }
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent == null) return null
        val action = intent.action ?: return null
        val type = intent.type ?: return null
        return if (Intent.ACTION_SEND == action && type.startsWith("text/")) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }
    }
}

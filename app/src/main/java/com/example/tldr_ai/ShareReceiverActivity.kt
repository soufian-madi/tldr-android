package com.example.tldr_ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.example.tldr_ai.floatingcard.FloatingCardService

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = getSharedText(intent)
        if (sharedUrl != null) {
            if (Settings.canDrawOverlays(this)) {
                startCardService(sharedUrl)
                finish()
            } else {
                // Request overlay permission
                val permissionIntent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(permissionIntent, OVERLAY_PERMISSION_REQUEST)
            }
        } else {
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Settings.canDrawOverlays(this)) {
                val sharedUrl = getSharedText(intent)
                if (sharedUrl != null) {
                    startCardService(sharedUrl)
                }
            }
            finish()
        }
    }

    private fun startCardService(url: String) {
        val serviceIntent = Intent(this, FloatingCardService::class.java).apply {
            putExtra(FloatingCardService.EXTRA_URL, url)
        }
        startForegroundService(serviceIntent)
    }

    private fun getSharedText(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
            val lines = sharedText.trim().lines()
            // Google News shares "Title\nURL" - extract just the URL
            return if (lines.size == 2) lines[1].trim() else sharedText
        }
        return null
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST = 1001
    }
}

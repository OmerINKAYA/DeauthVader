package com.example.deauthvader

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WifiAttackActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout tanımlı değilse doğrudan WebView kullanabilirsin
        webView = WebView(this).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true

            // EKLENMESİ GEREKENLER 👇
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.setSupportZoom(true)

            loadUrl("http://192.168.4.1")
        }

        setContentView(webView)
    }
    override fun onBackPressed() {
        super.onBackPressed()
    }
}

package com.example.deauthvader

import android.app.Activity
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast

class FollowMeActivity : Activity() {

    // Bluetooth soket (bağlantı zaten ana ekranda yapılmış olmalı)
    companion object {
        lateinit var bluetoothSocket: BluetoothSocket
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow_me)

        val buttonFollow = findViewById<Button>(R.id.buttonFollow)
        val buttonManual = findViewById<Button>(R.id.buttonManual)


        buttonFollow.setOnClickListener {
            sendCommand("A")
        }

        buttonManual.setOnClickListener {
            sendCommand("M")
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }



    }

    private fun sendCommand(command: String) {
        try {
            val outStream = BluetoothConnectionManager.outStream
            if (outStream != null) {
                outStream.write(command.toByteArray())
                outStream.flush()
                Toast.makeText(this, "Komut gönderildi: $command", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "⚠️ Bluetooth bağlantısı yok!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "HATA: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        // Geri tuşuna basıldığında 'M' sinyali gönder
        sendCommand("M")
        super.onBackPressed()
    }
}

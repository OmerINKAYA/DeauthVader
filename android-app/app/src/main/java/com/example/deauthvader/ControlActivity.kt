package com.example.deauthvader

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class ControlActivity : AppCompatActivity() {

    private lateinit var btnF: Button
    private lateinit var btnB: Button
    private lateinit var btnL: Button
    private lateinit var btnR: Button
    private lateinit var btnStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        btnF    = findViewById(R.id.btnForward)
        btnB    = findViewById(R.id.btnBackward)
        btnL    = findViewById(R.id.btnLeft)
        btnR    = findViewById(R.id.btnRight)
        btnStop = findViewById(R.id.btnStop)


        // İleri: bas → H, çek → S
        btnF.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> sendCommand("H")
                MotionEvent.ACTION_UP   -> sendCommand("S")
            }
            true
        }

        // Geri: bas → J, çek → S
        btnB.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> sendCommand("J")
                MotionEvent.ACTION_UP   -> sendCommand("S")
            }
            true
        }

        // Sola: bas → K, çek → S
        btnL.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> sendCommand("K")
                MotionEvent.ACTION_UP   -> sendCommand("S")
            }
            true
        }

        // Sağa: bas → N, çek → S
        btnR.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> sendCommand("N")
                MotionEvent.ACTION_UP   -> sendCommand("S")
            }
            true
        }

        // Durdur tuşunu normal click ile de bırakabilirsiniz
        btnStop.setOnClickListener {
            sendCommand("S")
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun sendCommand(cmd: String) {
        val out = BluetoothConnectionManager.outStream
        if (out == null) {
            Toast.makeText(this, "Önce Bluetooth’a bağlan.", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            try {
                out.write(cmd.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Komut gönderilemedi: $cmd", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}

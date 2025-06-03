package com.example.deauthvader

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        try {
            BluetoothConnectionManager.disconnect()
            Toast.makeText(this, "Bluetooth bağlantısı kesildi", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Bağlantı kesme hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
        // Ardından MainActivity’ye dön
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // 🔙 Geri butonuna tıklandığında LoginActivity'e git
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            try {
                BluetoothConnectionManager.disconnect()
                Toast.makeText(this, "Bluetooth bağlantısı kesildi", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Bağlantı kesme hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnGpsTracking).setOnClickListener {
            startActivity(Intent(this, GpsTrackingActivity::class.java))
        }

        findViewById<Button>(R.id.btnFollowMe).setOnClickListener {
            startActivity(Intent(this, FollowMeActivity::class.java))
        }

        findViewById<Button>(R.id.btnGridDrawn).setOnClickListener {
            startActivity(Intent(this, GridDrawControlActivity::class.java))
        }

        findViewById<Button>(R.id.btnWifiAttack).setOnClickListener {
            startActivity(Intent(this, WifiAttackActivity::class.java))
        }

        findViewById<Button>(R.id.btnFreeMove).setOnClickListener {
            startActivity(Intent(this, ControlActivity::class.java))
        }

        findViewById<Button>(R.id.btnCalibration).setOnClickListener {
            startActivity(Intent(this, CalibrationActivity::class.java))
        }
    }
}

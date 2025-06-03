package com.example.deauthvader // Proje paket adınız neyse onu kullanın

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import kotlin.concurrent.thread

class CalibrationActivity : AppCompatActivity() {

    private lateinit var etForwardDelay: EditText
    private lateinit var etRightTurnDelay: EditText
    private lateinit var etLeftTurnDelay: EditText
    private lateinit var btnSendCalibration: Button

    private val PREFS_NAME = "RobotCalibrationPrefs"
    private val KEY_FORWARD_DELAY = "forward_delay"
    private val KEY_RIGHT_TURN_DELAY = "right_turn_delay"
    private val KEY_LEFT_TURN_DELAY = "left_turn_delay"

    // Default değerler (Arduino kodundaki başlangıç değerleriyle aynı)
    private val DEFAULT_FORWARD_DELAY = 300
    private val DEFAULT_RIGHT_TURN_DELAY = 400
    private val DEFAULT_LEFT_TURN_DELAY = 400

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        etForwardDelay = findViewById(R.id.etForwardDelay)
        etRightTurnDelay = findViewById(R.id.etRightTurnDelay)
        etLeftTurnDelay = findViewById(R.id.etLeftTurnDelay)
        btnSendCalibration = findViewById(R.id.btnSendCalibration)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Kaydedilmiş değerleri yükle veya default değerleri göster
        loadCalibrationValues()

        btnSendCalibration.setOnClickListener {
            sendCalibrationValues()
        }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadCalibrationValues() {
        val forwardDelay = sharedPreferences.getInt(KEY_FORWARD_DELAY, DEFAULT_FORWARD_DELAY)
        val rightTurnDelay = sharedPreferences.getInt(KEY_RIGHT_TURN_DELAY, DEFAULT_RIGHT_TURN_DELAY)
        val leftTurnDelay = sharedPreferences.getInt(KEY_LEFT_TURN_DELAY, DEFAULT_LEFT_TURN_DELAY)

        etForwardDelay.setText(forwardDelay.toString())
        etRightTurnDelay.setText(rightTurnDelay.toString())
        etLeftTurnDelay.setText(leftTurnDelay.toString())
    }

    private fun sendCalibrationValues() {
        // BluetoothConnectionManager'dan OutputStream'i alın.
        val out = BluetoothConnectionManager.outStream
        if (out == null) { // Soketin bağlı olup olmadığını da kontrol et
            Toast.makeText(this, "Önce Bluetooth’a bağlanın.", Toast.LENGTH_SHORT).show()
            return
        }

        val forwardDelayStr = etForwardDelay.text.toString()
        val rightTurnDelayStr = etRightTurnDelay.text.toString()
        val leftTurnDelayStr = etLeftTurnDelay.text.toString()

        // *** DEĞİŞİKLİK: Tüm alanları kontrol et ***
        if (forwardDelayStr.isBlank() || rightTurnDelayStr.isBlank() || leftTurnDelayStr.isBlank()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
            return
        }

        val forwardDelay = forwardDelayStr.toIntOrNull()
        val rightTurnDelay = rightTurnDelayStr.toIntOrNull()
        val leftTurnDelay = leftTurnDelayStr.toIntOrNull()

        // *** DEĞİŞİKLİK: Tüm değerleri kontrol et ***
        if (forwardDelay == null || rightTurnDelay == null || leftTurnDelay == null ||
            forwardDelay <= 0 || rightTurnDelay <= 0 || leftTurnDelay <= 0) { // 0'dan büyük olmalı
            Toast.makeText(this, "Lütfen geçerli pozitif (>0) sayı değerleri girin.", Toast.LENGTH_SHORT).show()
            return
        }

        with(sharedPreferences.edit()) {
            putInt(KEY_FORWARD_DELAY, forwardDelay)
            putInt(KEY_RIGHT_TURN_DELAY, rightTurnDelay)
            putInt(KEY_LEFT_TURN_DELAY, leftTurnDelay)
            apply() // Arka planda asenkron kaydeder
        }

        // *** DEĞİŞİKLİK: Arduino'nun beklediği formatta 3 ayrı komut oluştur ***
        val commandForward = "#I$forwardDelay\n"
        val commandRight = "#R$rightTurnDelay\n"
        val commandLeft = "#L$leftTurnDelay\n"

        // Komutları ayrı bir thread'de gönder
        thread {
            try {
                // *** DEĞİŞİKLİK: Üç komutu da sırayla gönder ***
                out.write(commandForward.toByteArray())
                Thread.sleep(50)
                out.write(commandRight.toByteArray())
                Thread.sleep(50)
                out.write(commandLeft.toByteArray())

                runOnUiThread {
                    Toast.makeText(this@CalibrationActivity, "Kalibrasyon ayarları gönderildi.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@CalibrationActivity, "Hata: Ayarlar gönderilemedi. Bağlantıyı kontrol edin.", Toast.LENGTH_LONG).show()
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt() // Thread kesintiye uğrarsa
                runOnUiThread {
                    Toast.makeText(this@CalibrationActivity, "Hata: Gönderim kesintiye uğradı.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
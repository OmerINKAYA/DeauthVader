package com.example.deauthvader // Proje paket adınız neyse onu kullanın

import android.content.Context
import android.content.Intent
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
    // *** DEĞİŞİKLİK: Sağ ve Sol için ayrı EditText'lar ***
    private lateinit var etRightTurnDelay: EditText
    private lateinit var etLeftTurnDelay: EditText
    private lateinit var btnSendCalibration: Button

    // SharedPreferences adı
    private val PREFS_NAME = "RobotCalibrationPrefs"
    // *** DEĞİŞİKLİK: Ayrı anahtarlar ***
    private val KEY_FORWARD_DELAY = "forward_delay"
    private val KEY_RIGHT_TURN_DELAY = "right_turn_delay"
    private val KEY_LEFT_TURN_DELAY = "left_turn_delay"

    // Default değerler (Arduino kodundaki başlangıç değerleriyle aynı)
    private val DEFAULT_FORWARD_DELAY = 300
    private val DEFAULT_RIGHT_TURN_DELAY = 400 // Arduino'daki sagaDonDelay
    private val DEFAULT_LEFT_TURN_DELAY = 400  // Arduino'daki solaDonDelay

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ÖNEMLİ: activity_calibration.xml layout'unuzda
        // etRightTurnDelay ve etLeftTurnDelay ID'lerine sahip EditText'ların
        // olduğundan emin olun.
        setContentView(R.layout.activity_calibration)

        etForwardDelay = findViewById(R.id.etForwardDelay)
        // *** DEĞİŞİKLİK: Yeni EditText ID'lerini bağlayın ***
        etRightTurnDelay = findViewById(R.id.etRightTurnDelay) // XML'de bu ID olmalı
        etLeftTurnDelay = findViewById(R.id.etLeftTurnDelay)   // XML'de bu ID olmalı
        btnSendCalibration = findViewById(R.id.btnSendCalibration)

        // SharedPreferences'ı al
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
        // *** DEĞİŞİKLİK: Ayrı değerleri yükle ***
        val rightTurnDelay = sharedPreferences.getInt(KEY_RIGHT_TURN_DELAY, DEFAULT_RIGHT_TURN_DELAY)
        val leftTurnDelay = sharedPreferences.getInt(KEY_LEFT_TURN_DELAY, DEFAULT_LEFT_TURN_DELAY)

        etForwardDelay.setText(forwardDelay.toString())
        // *** DEĞİŞİKLİK: Ayrı EditText'lara ata ***
        etRightTurnDelay.setText(rightTurnDelay.toString())
        etLeftTurnDelay.setText(leftTurnDelay.toString())
    }

    private fun sendCalibrationValues() {
        // BluetoothConnectionManager'dan OutputStream'i alın.
        // Bu sınıfın projenizde doğru şekilde ayarlandığından emin olun.
        val out = BluetoothConnectionManager.outStream

//        if (out == null || BluetoothConnectionManager.socket?.isConnected != true) { // Soketin bağlı olup olmadığını da kontrol et
//            Toast.makeText(this, "Önce Bluetooth’a bağlanın.", Toast.LENGTH_SHORT).show()
//            return
//        }

        if (out == null) { // Soketin bağlı olup olmadığını da kontrol et
            Toast.makeText(this, "Önce Bluetooth’a bağlanın.", Toast.LENGTH_SHORT).show()
            return
        }

        val forwardDelayStr = etForwardDelay.text.toString()
        // *** DEĞİŞİKLİK: Ayrı değerleri al ***
        val rightTurnDelayStr = etRightTurnDelay.text.toString()
        val leftTurnDelayStr = etLeftTurnDelay.text.toString()

        // *** DEĞİŞİKLİK: Tüm alanları kontrol et ***
        if (forwardDelayStr.isBlank() || rightTurnDelayStr.isBlank() || leftTurnDelayStr.isBlank()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
            return
        }

        val forwardDelay = forwardDelayStr.toIntOrNull()
        // *** DEĞİŞİKLİK: Ayrı değerleri çevir ***
        val rightTurnDelay = rightTurnDelayStr.toIntOrNull()
        val leftTurnDelay = leftTurnDelayStr.toIntOrNull()

        // *** DEĞİŞİKLİK: Tüm değerleri kontrol et ***
        if (forwardDelay == null || rightTurnDelay == null || leftTurnDelay == null ||
            forwardDelay <= 0 || rightTurnDelay <= 0 || leftTurnDelay <= 0) { // 0'dan büyük olmalı
            Toast.makeText(this, "Lütfen geçerli pozitif (>0) sayı değerleri girin.", Toast.LENGTH_SHORT).show()
            return
        }

        // Değerleri SharedPreferences'a kaydet
        with(sharedPreferences.edit()) {
            putInt(KEY_FORWARD_DELAY, forwardDelay)
            // *** DEĞİŞİKLİK: Ayrı değerleri kaydet ***
            putInt(KEY_RIGHT_TURN_DELAY, rightTurnDelay)
            putInt(KEY_LEFT_TURN_DELAY, leftTurnDelay)
            apply() // Arka planda asenkron kaydeder
        }

        // *** DEĞİŞİKLİK: Arduino'nun beklediği formatta 3 ayrı komut oluştur ***
        // Her komutun sonuna newline (\n) ekliyoruz, Arduino'da Serial.parseInt() bunu bekler.
        val commandForward = "#I$forwardDelay\n"
        val commandRight = "#R$rightTurnDelay\n"
        val commandLeft = "#L$leftTurnDelay\n"

        // Komutları ayrı bir thread'de gönder
        thread {
            try {
                // *** DEĞİŞİKLİK: Üç komutu da sırayla gönder ***
                out.write(commandForward.toByteArray())
                Thread.sleep(50) // Arduino'ya işlemek için kısa bir süre verilebilir (isteğe bağlı)
                out.write(commandRight.toByteArray())
                Thread.sleep(50) // Arduino'ya işlemek için kısa bir süre verilebilir (isteğe bağlı)
                out.write(commandLeft.toByteArray())

                // Kullanıcıya başarı mesajını UI thread'de göster
                runOnUiThread {
                    Toast.makeText(this@CalibrationActivity, "Kalibrasyon ayarları gönderildi.", Toast.LENGTH_SHORT).show()
                    // Log.d("CalibrationActivity", "Gönderildi: $commandForward, $commandRight, $commandLeft") // Debug için
                }
            } catch (e: IOException) {
                e.printStackTrace()
                // Kullanıcıya hata mesajını UI thread'de göster
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
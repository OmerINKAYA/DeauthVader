package com.example.deauthvader

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_BLUETOOTH_CONNECT = 100
    }

    private val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var btSocket: BluetoothSocket? = null
    private var outStream: OutputStream? = null
    private val BT_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Android 12+’da izin iste (opsiyonel)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                REQUEST_BLUETOOTH_CONNECT
            )
        }

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            showPairedDevices()
        }
        findViewById<Button>(R.id.btnGoToMenu).setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPairedDevices()
            } else {
                Toast.makeText(this, "Bluetooth izni reddedildi", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showPairedDevices() {
        // Android 12+ inline izin kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
             ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                REQUEST_BLUETOOTH_CONNECT
            )
            return
        }

        val adapter = btAdapter ?: run {
            Toast.makeText(this, "Bluetooth desteklenmiyor", Toast.LENGTH_SHORT).show()
            return
        }
        if (!adapter.isEnabled) {
            Toast.makeText(this, "Lütfen önce Bluetooth’u aç", Toast.LENGTH_SHORT).show()
            return
        }

        val paired: Set<BluetoothDevice> = adapter.bondedDevices
        val list = paired.map { "${it.name}\n${it.address}" }.toTypedArray()
        if (list.isEmpty()) {
            Toast.makeText(this, "Eşleşmiş cihaz yok", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Cihaz seç")
            .setItems(list) { _, which ->
                val address = list[which].split("\n")[1]
                connectToDevice(address)
            }
            .show()
    }

    private fun connectToDevice(address: String) {
        // Yine inline izin kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Bluetooth izni yok", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                val adapter = btAdapter!!
                val device = adapter.getRemoteDevice(address)
                adapter.cancelDiscovery()
                btSocket = device.createRfcommSocketToServiceRecord(BT_UUID)
                btSocket!!.connect()
                outStream = btSocket!!.outputStream
                BluetoothConnectionManager.socket = btSocket

                // Singleton üzerinden paylaş
                BluetoothConnectionManager.outStream = outStream
                BluetoothConnectionManager.inStream = btSocket!!.inputStream

                runOnUiThread {
                    Toast.makeText(this, "Bağlantı başarılı", Toast.LENGTH_SHORT).show()
                    // Menü ekranına geçiş
                    startActivity(Intent(this, MenuActivity::class.java))
                    finish()

                }
            } catch (se: SecurityException) {
                runOnUiThread {
                    Toast.makeText(this, "Bluetooth izni reddedildi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    // Hata varsa aynı sayfada kalıp mesaj gösteriyoruz
                    Toast.makeText(this, "Bağlantı hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                try { btSocket?.close() } catch (_: Exception) {}
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

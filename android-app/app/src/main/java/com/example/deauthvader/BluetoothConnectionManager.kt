package com.example.deauthvader

import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream

object BluetoothConnectionManager {
    /**
     * MainActivity bağlantı kurulduğunda bu değişkene set edilecek.
     * ControlActivity burada tutulan outStream üzerinden komut yollayacak.
     */
    var outStream: OutputStream? = null
    var inStream: InputStream? = null
    var socket: BluetoothSocket? = null
    fun disconnect() {
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket = null
            outStream = null
            inStream = null
        }
    }
}

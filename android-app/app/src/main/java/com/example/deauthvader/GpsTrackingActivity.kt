package com.example.deauthvader

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

class GpsTrackingActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private const val TAG = "GpsTrackingActivity"
    }

    private lateinit var tvLat: TextView
    private lateinit var tvLng: TextView
    private lateinit var tvSat: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView

    private var gpsThread: Thread? = null

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private var lastMarker: Marker? = null
    private var currentLat: Double? = null
    private var currentLng: Double? = null

    private val pathPoints = mutableListOf<LatLng>()
    private var pathLine: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gps_tracking)

        tvLat  = findViewById(R.id.tvLatitude)
        tvLng  = findViewById(R.id.tvLongitude)
        tvSat  = findViewById(R.id.tvSatellites)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isZoomControlsEnabled = true
        }

        startReadingGps()
    }

    private fun startReadingGps() {
        val input = BluetoothConnectionManager.inStream
        Log.d(TAG, "inStream = $input")
        if (input == null) {
            Toast.makeText(this, "Önce Bluetooth’a bağlanmalısın.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val reader = BufferedReader(InputStreamReader(input))
        gpsThread = Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    // Satır oku (block olur ama thread interrupt edince IOException fırlatır)
                    val line = reader.readLine() ?: continue
                    Log.d(TAG, "raw GPS line → $line")

                    // Gelen satırı UI thread’inde parse edip ekrana yaz
                    runOnUiThread {
                        when {
                            line.startsWith("Latitude:") -> {
                                val v = line.substringAfter("Latitude:").trim()
                                tvLat.text = "Latitude: $v"
                                currentLat = v.toDoubleOrNull()
                            }
                            line.startsWith("Longitude:") -> {
                                val v = line.substringAfter("Longitude:").trim()
                                tvLng.text = "Longitude: $v"
                                currentLng = v.toDoubleOrNull()
                                if (currentLat != null && currentLng != null) {
                                    updateMap(currentLat!!, currentLng!!)
                                }
                            }
                            line.startsWith("Satellites:") -> {
                                val v = line.substringAfter("Satellites:").trim()
                                tvSat.text = "Satellites: $v"
                            }
                            line.startsWith("Date:") -> {
                                val v = line.substringAfter("Date:").trim()
                                tvDate.text = "Date: $v"
                            }
                            line.startsWith("Time:") -> {
                                val v = line.substringAfter("Time:").trim()
                                tvTime.text = "Time: $v"
                            }
                            else -> {
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "GPS okuma esnasında hata", e)
                runOnUiThread {
                    Toast.makeText(this, "GPS okuma hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }.apply { start() }
    }

    private fun updateMap(lat: Double, lng: Double) {
        val location = LatLng(lat, lng)

        // Konumu listeye ekle
        pathPoints.add(location)

        // Marker'ı güncelle
        lastMarker?.remove()
        lastMarker = googleMap?.addMarker(
            MarkerOptions().position(location).title("Şu anki konum")
        )

        // Kamerayı konuma taşı
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 18f))

        // Polyline (rota çizgisi) çiz
        pathLine?.remove()
        pathLine = googleMap?.addPolyline(
            PolylineOptions()
                .addAll(pathPoints)
                .color(0xFF2196F3.toInt()) // Mavi
                .width(10f)
        )
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        gpsThread?.interrupt()
        gpsThread = null
        mapView.onDestroy()
        Log.d(TAG, "GPS okuma thread'i sonlandırıldı.")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        googleMap?.uiSettings?.isZoomControlsEnabled = true
    }
}

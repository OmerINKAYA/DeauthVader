package com.example.deauthvader

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

/**
 * GridDrawControlActivity: Izgara üzerinde çizim yaparak robotu yönlendirme aktivitesi.
 * Kullanıcı ızgara çizgileri üzerinde bir yol çizerek robotun takip edeceği rotayı belirler.
 */
class GridDrawControlActivity : AppCompatActivity() {

    private lateinit var gridDrawView: GridDrawView
    private lateinit var btnSend: Button
    private lateinit var btnClear: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_draw_control)

        gridDrawView = findViewById(R.id.gridDrawView)
        btnSend = findViewById(R.id.btnSend)
        btnClear = findViewById(R.id.btnClear)

        // Gönder butonu: çizilen rotayı Arduino'ya gönderir
        btnSend.setOnClickListener {
            if (gridDrawView.pathPoints.size < 2) {
                Toast.makeText(this, "Lütfen önce bir yol çizin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val commands = gridDrawView.generatePathCommands()
            if (commands.isEmpty()) {
                Toast.makeText(this, "Geçerli bir yol çizilemedi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            executeCommands(commands)
        }

        // Temizle butonu: çizimi sıfırlar
        btnClear.setOnClickListener {
            gridDrawView.clearPath()
        }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    // Komutları Arduino'ya gönder
    private fun executeCommands(commands: List<Char>) {
        val out = BluetoothConnectionManager.outStream
        if (out == null) {
            Toast.makeText(this, "Önce Bluetooth'a bağlanın", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Komutlar gönderiliyor...", Toast.LENGTH_SHORT).show()

        // Arka planda komutları gönder
        thread {
            try {
                // Önce bir dur komutu gönder, eski hareketleri durdur
                out.write("S".toByteArray())
                Thread.sleep(200)

                // Komutları tek tek gönder
                for (cmd in commands) {
                    out.write(cmd.toString().toByteArray())
                    // Her komut için Arduino'nun işlemi tamamlamasını bekle
                    Thread.sleep(750) // Aracın hareket etmesi ve durması için yeterli süre
                }

                // En son dur komutu gönder
                out.write("S".toByteArray())

                runOnUiThread {
                    Toast.makeText(this, "Komutlar tamamlandı", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

/**
 * Izgara çizim görünümü - Izgara çizgilerini çizer ve kullanıcının etkileşimini yönetir
 */
class GridDrawView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    // Sabit sütun sayısı ve dinamik satır sayısı
    private val gridCols = 6 // Yatayda 6 sütun
    // Hücre kenar uzunluğu, en geniş ekranda altı kareye bölünür
    private val cellSize: Float get() = width.toFloat() / gridCols.toFloat()
    // Dikeyde kaç satır sığarsa
    private val gridRows: Int get() = (height / cellSize).toInt()

    // Çizim araçları
    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val pathPaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val nodePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    // Yol noktaları ve geçerli ızgara noktası
    val pathPoints = mutableListOf<Point>()
    private var currentGridPoint: Point? = null
    private var lastDrawnPoint: Point? = null

    // İzleme için
    private var isDrawing = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Izgara çizgilerini çiz
        drawGrid(canvas)

        // Yolu çiz
        if (pathPoints.size > 1) {
            val path = Path()

            // İlk noktadan başla
            val startX = pathPoints[0].x * cellSize + cellSize / 2
            val startY = pathPoints[0].y * cellSize + cellSize / 2
            path.moveTo(startX, startY)

            // Diğer noktaları ekle
            for (i in 1 until pathPoints.size) {
                val x = pathPoints[i].x * cellSize + cellSize / 2
                val y = pathPoints[i].y * cellSize + cellSize / 2
                path.lineTo(x, y)
            }

            // Eğer çizim yapılıyorsa ve geçerli bir ızgara noktası varsa
            if (isDrawing && currentGridPoint != null) {
                val x = currentGridPoint!!.x * cellSize + cellSize / 2
                val y = currentGridPoint!!.y * cellSize + cellSize / 2
                path.lineTo(x, y)
            }

            canvas.drawPath(path, pathPaint)

            // Düğüm noktalarını çiz
            for (point in pathPoints) {
                val x = point.x * cellSize + cellSize / 2
                val y = point.y * cellSize + cellSize / 2
                canvas.drawCircle(x, y, 15f, nodePaint)
            }
        }
    }

    // Izgara çizgilerini çiz
    private fun drawGrid(canvas: Canvas) {
        val widthF = width.toFloat()
        val heightF = height.toFloat()

        // Yatay satırlar gridRows kadar
        for (i in 0..gridRows) {
            val y = i * cellSize
            canvas.drawLine(0f, y, widthF, y, gridPaint)
        }
        // Dikey sütunlar gridCols kadar
        for (i in 0..gridCols) {
            val x = i * cellSize
            canvas.drawLine(x, 0f, x, heightF, gridPaint)
        }
    }

    // Dokunma olayını işle
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Çizim başlangıcı
                isDrawing = true
                val gridPoint = pointToGridPoint(event.x, event.y)
                currentGridPoint = gridPoint

                // Yeni bir yol başlatılırsa, eski yolu temizle
                if (pathPoints.isEmpty()) {
                    pathPoints.add(gridPoint)
                    lastDrawnPoint = gridPoint
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Çizim devam ediyor
                if (isDrawing) {
                    val gridPoint = pointToGridPoint(event.x, event.y)
                    currentGridPoint = gridPoint

                    // Sadece ızgara çizgilerinde hareket ediyorsa ve yeni bir nokta ise ekle
                    if (isValidGridLine(lastDrawnPoint, gridPoint) && gridPoint != lastDrawnPoint) {
                        pathPoints.add(gridPoint)
                        lastDrawnPoint = gridPoint
                    }
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Çizim bitti
                isDrawing = false
                currentGridPoint = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // Ekran koordinatını ızgara noktasına dönüştür
    private fun pointToGridPoint(x: Float, y: Float): Point {
        val gridX = (x / cellSize).toInt().coerceIn(0, gridCols - 1)
        val gridY = (y / cellSize).toInt().coerceIn(0, gridRows - 1)
        return Point(gridX, gridY)
    }

    // İki nokta arasındaki hareketin geçerli bir ızgara çizgisi üzerinde olup olmadığını kontrol et
    private fun isValidGridLine(from: Point?, to: Point?): Boolean {
        if (from == null || to == null) return false

        // Aynı nokta ise geçersiz
        if (from == to) return false

        // Sadece yatay veya dikey hareketlere izin ver (diyagonal değil)
        return (from.x == to.x || from.y == to.y)
    }

    // Yolu temizle
    fun clearPath() {
        pathPoints.clear()
        lastDrawnPoint = null
        invalidate()
    }

    // Robotun takip edeceği komutları oluştur
    fun generatePathCommands(): List<Char> {
        if (pathPoints.size < 2) return emptyList()

        val commands = mutableListOf<Char>()
        var currentDirection = -1 // -1: başlangıç, 0: yukarı, 1: sağ, 2: aşağı, 3: sol

        for (i in 1 until pathPoints.size) {
            val prev = pathPoints[i-1]
            val curr = pathPoints[i]

            // Hareket yönünü belirle
            val newDirection = when {
                curr.y < prev.y -> 0 // yukarı
                curr.x > prev.x -> 1 // sağa
                curr.y > prev.y -> 2 // aşağı
                curr.x < prev.x -> 3 // sola
                else -> currentDirection // aynı nokta, değişiklik yok
            }

            // Yön değiştiyse dönüş komutu ekle
            if (currentDirection != -1 && currentDirection != newDirection) {
                // Saat yönünde mi, saat yönünün tersine mi dönüş yapmalı?
                val diff = (newDirection - currentDirection + 4) % 4
                when (diff) {
                    1 -> commands.add('R') // Sağa dön
                    2 -> { commands.add('R'); commands.add('R') } // 180 derece dön (2 kez sağa)
                    3 -> commands.add('L') // Sola dön
                }
            }

            // İleri git komutu ekle
            commands.add('F')

            currentDirection = newDirection
        }

        return commands
    }
}

package com.cloudy.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.Locale

class FloatingCloudService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var aiService: AIService
    private lateinit var tts: TextToSpeech
    private var isExpanded = false
    private var isTTSReady = false

    private lateinit var cloudService: CloudTranslationService

    private val captureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            val resultData = intent.getParcelableExtra<Intent>("resultData")
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                try { overlayView.visibility = View.GONE } catch (e: Exception) {}
                startScreenCapture(resultCode, resultData)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        aiService = AIService(this)
        // initialize cloudService with values from BuildConfig (set via gradle.properties)
        val url = BuildConfig.TRANSLATION_API_URL
        val key = BuildConfig.TRANSLATION_API_KEY
        cloudService = CloudTranslationService(url, key)

        startForeground(1, buildNotification())
        setupTTS()
        createOverlay()
        val filter = IntentFilter(CapturePermissionActivity.ACTION_RESULT)
        registerReceiver(captureReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(captureReceiver) } catch (e: Exception) {}
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        aiService.destroy()
    }

    private fun setupTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTTSReady = true
                tts.language = Locale("ar", "SA")
            }
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "alkhafaji_overlay_channel"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Al-Khafaji AI Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("الخفاجي AI")
            .setContentText("الروبوت العائم مفعّل")
            .setSmallIcon(R.drawable.ic_robot_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createOverlay() {
        windowManager = getSystemService(WindowManager::class.java)
        overlayView = LayoutInflater.from(this).inflate(R.layout.floating_cloud, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 220
        }

        val cloudButton = overlayView.findViewById<ImageButton>(R.id.cloudButton)
        val contentPanel = overlayView.findViewById<LinearLayout>(R.id.contentPanel)
        val inputField = overlayView.findViewById<EditText>(R.id.inputField)
        val responseText = overlayView.findViewById<TextView>(R.id.responseText)
        val sendButton = overlayView.findViewById<Button>(R.id.sendButton)
        val voiceButton = overlayView.findViewById<Button>(R.id.voiceButton)
        val closeButton = overlayView.findViewById<Button>(R.id.closeButton)
        val translateButton = overlayView.findViewById<Button>(R.id.translateButton)

        var isMoving = false
        var lastX = 0
        var lastY = 0
        var initialX = 0
        var initialY = 0

        cloudButton.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    isMoving = true
                    initialX = event.rawX.toInt()
                    initialY = event.rawY.toInt()
                    lastX = initialX
                    lastY = initialY
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    if (isMoving) {
                        val dx = event.rawX.toInt() - lastX
                        val dy = event.rawY.toInt() - lastY
                        params.x += dx
                        params.y += dy
                        windowManager.updateViewLayout(overlayView, params)
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                    }
                }
                android.view.MotionEvent.ACTION_UP -> {
                    isMoving = false
                    if (java.lang.Math.abs(event.rawX.toInt() - initialX) < 10 &&
                        java.lang.Math.abs(event.rawY.toInt() - initialY) < 10
                    ) {
                        isExpanded = !isExpanded
                        contentPanel.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    }
                }
            }
            true
        }

        sendButton.setOnClickListener {
            val userInput = inputField.text.toString().trim()
            if (userInput.isNotEmpty()) {
                responseText.text = "جاري المعالجة..."
                aiService.generateResponse(userInput) { response ->
                    responseText.text = response
                    inputField.setText("")
                    if (isTTSReady) {
                        tts.speak(response, TextToSpeech.QUEUE_FLUSH, null, "ai_response")
                    }
                }
            }
        }

        voiceButton.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث...")
            }
            startActivity(intent)
        }

        translateButton.setOnClickListener {
            // Launch permission activity to ask for MediaProjection
            val i = Intent(this, CapturePermissionActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }

        closeButton.setOnClickListener {
            stopSelf()
        }

        windowManager.addView(overlayView, params)
    }

    private fun startScreenCapture(resultCode: Int, data: Intent) {
        val mm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection: MediaProjection? = mm.getMediaProjection(resultCode, data)
        if (projection == null) return

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val virtualDisplay = projection.createVirtualDisplay(
            "screen-capture",
            width, height, density,
            Display.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        val handler = Handler(Looper.getMainLooper())
        imageReader.setOnImageAvailableListener({ reader ->
            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    val plane = image.planes[0]
                    val buffer: ByteBuffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(buffer)
                    val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)

                    // perform cloud translation (send image to cloud)
                    val responseText = overlayView.findViewById<TextView>(R.id.responseText)
                    responseText.post { responseText.text = "جاري رفع الصورة للترجمة..." }

                    CoroutineScope(Dispatchers.IO + Job()).launch {
                        try {
                            val translated = cloudService.translateBitmap(cropped, "ar")
                            responseText.post { responseText.text = translated }
                            if (isTTSReady) {
                                tts.speak(translated, TextToSpeech.QUEUE_FLUSH, null, "translate_tts")
                            }
                        } catch (e: Exception) {
                            responseText.post { responseText.text = "خطأ ف�� الترجمة السحابية: ${e.message}" }
                        }
                    }

                    // restore overlay visibility
                    try { overlayView.visibility = View.VISIBLE } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { image?.close() } catch (e: Exception) {}
                try { reader.close() } catch (e: Exception) {}
                try { virtualDisplay.release() } catch (e: Exception) {}
                try { projection.stop() } catch (e: Exception) {}
            }
        }, handler)
    }
}

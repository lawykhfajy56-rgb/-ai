package com.cloudy.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.util.Locale

class FloatingCloudService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var aiService: AIService
    private lateinit var tts: TextToSpeech
    private var isExpanded = false
    private var isTTSReady = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        aiService = AIService(this)
        startForeground(1, buildNotification())
        setupTTS()
        createOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
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
        val channelId = "cloudy_overlay_channel"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cloudy Service",
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
            .setContentTitle("كلودي")
            .setContentText("السحابة العائمة مفعلة")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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

        val cloudButton = overlayView.findViewById<Button>(R.id.cloudButton)
        val contentPanel = overlayView.findViewById<LinearLayout>(R.id.contentPanel)
        val inputField = overlayView.findViewById<EditText>(R.id.inputField)
        val responseText = overlayView.findViewById<TextView>(R.id.responseText)
        val sendButton = overlayView.findViewById<Button>(R.id.sendButton)
        val voiceButton = overlayView.findViewById<Button>(R.id.voiceButton)
        val closeButton = overlayView.findViewById<Button>(R.id.closeButton)

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
                        tts.speak(response, TextToSpeech.QUEUE_FLUSH, null)
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

        closeButton.setOnClickListener {
            stopSelf()
        }

        windowManager.addView(overlayView, params)
    }
}

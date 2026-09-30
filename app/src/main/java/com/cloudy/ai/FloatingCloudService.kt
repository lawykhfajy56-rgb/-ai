package com.cloudy.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat

class FloatingCloudService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var isExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
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

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("كلودي")
            .setContentText("السحابة العائمة مفعلة")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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
        val closeButton = overlayView.findViewById<Button>(R.id.closeButton)

        cloudButton.setOnClickListener {
            isExpanded = !isExpanded
            contentPanel.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        closeButton.setOnClickListener {
            stopSelf()
        }

        windowManager.addView(overlayView, params)
    }
}

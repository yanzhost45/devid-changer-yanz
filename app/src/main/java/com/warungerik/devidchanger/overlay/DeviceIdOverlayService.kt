package com.warungerik.devidchanger.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class DeviceIdOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "device_id_overlay"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    private var position = 1
    private var total = 0

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        showOverlay()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

        overlayView = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun showOverlay() {

        if (overlayView != null) {
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(Color.argb(235, 30, 30, 30))
        }

        val title = TextView(this).apply {
            text = "DEVICE ID"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val counter = TextView(this).apply {
            text = "$position / $total"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 8)
        }

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val prevButton = Button(this).apply {
            text = "PREV"
            textSize = 11f
        }

        val nextButton = Button(this).apply {
            text = "NEXT"
            textSize = 11f
        }

        prevButton.setOnClickListener {
            if (total > 0) {
                position =
                    if (position <= 1) total else position - 1

                counter.text = "$position / $total"
            }
        }

        nextButton.setOnClickListener {
            if (total > 0) {
                position =
                    if (position >= total) 1 else position + 1

                counter.text = "$position / $total"
            }
        }

        buttons.addView(
            prevButton,
            LinearLayout.LayoutParams(
                110,
                48
            )
        )

        buttons.addView(
            nextButton,
            LinearLayout.LayoutParams(
                110,
                48
            )
        )

        root.addView(title)

        root.addView(
            counter,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(buttons)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 30
        params.y = 150

        root.setOnTouchListener(
            createDragListener(params)
        )

        overlayView = root

        windowManager.addView(
            root,
            params
        )
    }

    private fun createDragListener(
        params: WindowManager.LayoutParams
    ): View.OnTouchListener {

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        return View.OnTouchListener { view, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y

                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    params.x =
                        initialX +
                            (event.rawX - initialTouchX).toInt()

                    params.y =
                        initialY +
                            (event.rawY - initialTouchY).toInt()

                    windowManager.updateViewLayout(
                        view,
                        params
                    )

                    true
                }

                else -> false
            }
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device ID Overlay",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle("Device ID Overlay")
                .setContentText("Floating controller aktif")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .build()

        } else {

            Notification.Builder(this)
                .setContentTitle("Device ID Overlay")
                .setContentText("Floating controller aktif")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .build()
        }
    }
}

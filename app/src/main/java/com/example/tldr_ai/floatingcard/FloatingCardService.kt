package com.example.tldr_ai.floatingcard

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import com.example.tldr_ai.R
import com.example.tldr_ai.data.api.ClaudeApiService
import com.example.tldr_ai.data.model.SummaryResult
import com.example.tldr_ai.data.repository.SummaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingCardService : Service() {

    private lateinit var windowManager: WindowManager
    private var cardView: View? = null
    private var expandedView: View? = null
    private val apiService = ClaudeApiService()
    private val repository = SummaryRepository(apiService)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentUrl: String? = null

    companion object {
        const val EXTRA_URL = "extra_url"
        private const val CHANNEL_ID = "floating_card_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        val url = intent?.getStringExtra(EXTRA_URL)
        if (url != null) {
            currentUrl = url
            showCard()
            // Auto-expand and start loading
            showExpandedView()
            loadSummary(url)
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TL;DR Floating Card",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows floating card for article summaries"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TL;DR")
            .setContentText("Summarizing article...")
            .setSmallIcon(R.drawable.ic_floating_card)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun showCard() {
        if (cardView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        cardView = LayoutInflater.from(this).inflate(R.layout.floating_card_bubble, null)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        cardView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(cardView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = Math.abs(event.rawX - initialTouchX) > 10 ||
                            Math.abs(event.rawY - initialTouchY) > 10
                    if (!moved) {
                        toggleExpandedView()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(cardView, params)
    }

    @SuppressLint("InflateParams")
    private fun showExpandedView() {
        if (expandedView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        expandedView = LayoutInflater.from(this).inflate(R.layout.floating_card_expanded, null)

        expandedView?.findViewById<ImageView>(R.id.closeButton)?.setOnClickListener {
            closeCard()
        }

        expandedView?.findViewById<CardView>(R.id.summaryCard)?.setOnClickListener {
            // Consume click to prevent closing
        }

        expandedView?.setOnClickListener {
            hideExpandedView()
        }

        windowManager.addView(expandedView, params)
    }

    private fun hideExpandedView() {
        expandedView?.let {
            windowManager.removeView(it)
            expandedView = null
        }
    }

    private fun toggleExpandedView() {
        if (expandedView != null) {
            hideExpandedView()
        } else {
            showExpandedView()
            currentUrl?.let { loadSummary(it) }
        }
    }

    private fun loadSummary(url: String) {
        expandedView?.let { view ->
            val loadingLayout = view.findViewById<View>(R.id.loadingLayout)
            val loadingStatusText = view.findViewById<TextView>(R.id.loadingStatusText)
            val contentLayout = view.findViewById<View>(R.id.contentLayout)
            val titleText = view.findViewById<TextView>(R.id.titleText)
            val scoreText = view.findViewById<TextView>(R.id.scoreText)
            val scoreBar = view.findViewById<ProgressBar>(R.id.scoreBar)
            val summaryText = view.findViewById<TextView>(R.id.summaryText)
            val errorText = view.findViewById<TextView>(R.id.errorText)

            loadingLayout.visibility = View.VISIBLE
            loadingStatusText.text = "Fetching article..."
            contentLayout.visibility = View.GONE
            errorText.visibility = View.GONE

            serviceScope.launch {
                val fetchResult = withContext(Dispatchers.IO) { apiService.fetchUrl(url) }

                if (fetchResult.isFailure) {
                    loadingLayout.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = fetchResult.exceptionOrNull()?.message ?: "Failed to fetch article"
                    return@launch
                }

                loadingStatusText.text = "Waiting for AI response..."

                val result = withContext(Dispatchers.IO) { apiService.summarize(fetchResult.getOrThrow()) }

                loadingLayout.visibility = View.GONE
                result.fold(
                    onSuccess = { summary ->
                        contentLayout.visibility = View.VISIBLE
                        updateSummaryUI(summary, titleText, scoreText, scoreBar, summaryText)
                    },
                    onFailure = { error ->
                        errorText.visibility = View.VISIBLE
                        errorText.text = error.message ?: "Failed to load summary"
                    }
                )
            }
        }
    }

    private fun updateSummaryUI(
        summary: SummaryResult,
        titleText: TextView,
        scoreText: TextView,
        scoreBar: ProgressBar,
        summaryText: TextView
    ) {
        titleText.text = summary.originalTitle ?: "Article"
        scoreText.text = "${summary.clickbaitScore}%"
        scoreBar.progress = summary.clickbaitScore
        summaryText.text = summary.summary

        // Color based on score
        val color = when {
            summary.clickbaitScore <= 30 -> 0xFF4CAF50.toInt() // Green
            summary.clickbaitScore <= 60 -> 0xFFFF9800.toInt() // Orange
            else -> 0xFFF44336.toInt() // Red
        }
        scoreText.setTextColor(color)
    }

    private fun closeCard() {
        hideExpandedView()
        cardView?.let {
            windowManager.removeView(it)
            cardView = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        hideExpandedView()
        cardView?.let {
            windowManager.removeView(it)
            cardView = null
        }
    }
}

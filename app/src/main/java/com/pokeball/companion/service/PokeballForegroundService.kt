package com.pokeball.companion.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pokeball.companion.R
import com.pokeball.companion.data.ble.PokeballBleManager
import com.pokeball.companion.data.model.MappingConfig
import com.pokeball.companion.data.model.PokeballState
import com.pokeball.companion.data.repository.DeviceRepository
import com.pokeball.companion.data.repository.MappingRepository
import com.pokeball.companion.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PokeballForegroundService : Service() {

    private val tag = "PokeballService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var bleManager: PokeballBleManager
        private set
    lateinit var deviceRepository: DeviceRepository
        private set
    lateinit var mappingRepository: MappingRepository
        private set

    private val inputDispatcher = InputDispatcher()
    private var currentConfig = MappingConfig()
    private var isMappingPaused = false

    private var wakeLock: PowerManager.WakeLock? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): PokeballForegroundService = this@PokeballForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this

        deviceRepository = DeviceRepository(applicationContext)
        mappingRepository = MappingRepository(applicationContext)
        bleManager = PokeballBleManager.getInstance(applicationContext)

        createNotificationChannel()
        startAsForeground(buildNotification("Ready to connect", -1))

        acquireWakeLock()

        // Observe calibration
        scope.launch {
            mappingRepository.joystickCalibration.collectLatest { calib ->
                bleManager.updateCalibration(calib)
            }
        }

        // Observe mapping config
        scope.launch {
            mappingRepository.mappingConfig.collectLatest { config ->
                currentConfig = config
            }
        }

        // Observe device settings (MAC & auto reconnect)
        scope.launch {
            deviceRepository.autoReconnect.collectLatest { autoRec ->
                scope.launch {
                    deviceRepository.lastDeviceMac.collectLatest { mac ->
                        bleManager.setAutoReconnect(autoRec, mac)
                    }
                }
            }
        }

        // Observe BLE state for notifications & input dispatching
        scope.launch {
            bleManager.state.collectLatest { state ->
                updateNotificationForState(state)

                if (!isMappingPaused && state.isConnected) {
                    inputDispatcher.processState(state, currentConfig)
                } else {
                    inputDispatcher.reset()
                }

                // If freshly connected, persist MAC address
                if (state.isConnected && state.deviceAddress != null) {
                    deviceRepository.saveDevice(state.deviceAddress, state.deviceName)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                Log.d(tag, "Action DISCONNECT requested from notification")
                inputDispatcher.reset()
                bleManager.disconnect()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_TOGGLE_PAUSE -> {
                isMappingPaused = !isMappingPaused
                if (isMappingPaused) {
                    inputDispatcher.reset()
                }
                Log.d(tag, "Action TOGGLE_PAUSE: paused=$isMappingPaused")
                updateNotificationForState(bleManager.state.value)
            }
            ACTION_CONNECT_MAC -> {
                val mac = intent.getStringExtra(EXTRA_MAC)
                if (mac != null) {
                    bleManager.connectByMac(mac)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        bleManager.disconnect()
        scope.cancel()
        if (instance == this) instance = null
        Log.d(tag, "Service destroyed")
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PokeballCompanion:WakeLock")?.apply {
            setReferenceCounted(false)
            acquire(2 * 60 * 60 * 1000L) // 2 hours max
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "Error releasing wake lock: ${e.message}")
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Poké Ball Plus Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live connection status and controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startAsForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotificationForState(state: PokeballState) {
        val text = when {
            state.isConnected -> {
                val pauseText = if (isMappingPaused) " [Mapping Paused]" else ""
                val battText = if (state.batteryPercent >= 0) " (${state.batteryPercent}%)" else ""
                "Connected to ${state.deviceName ?: "Poké Ball"}$battText$pauseText"
            }
            state.isConnecting -> "Connecting to ${state.deviceName ?: "Poké Ball"}..."
            else -> "Disconnected"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(text, state.batteryPercent))
    }

    @SuppressLint("LaunchActivityFromNotification")
    private fun buildNotification(statusText: String, batteryPercent: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Disconnect action
        val disconnectIntent = Intent(this, PokeballForegroundService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Toggle pause action
        val pauseIntent = Intent(this, PokeballForegroundService::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            2,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseLabel = if (isMappingPaused) "Resume Mapping" else "Pause Mapping"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Poké Ball Companion")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, pauseLabel, pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "pokeball_companion_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_DISCONNECT = "com.pokeball.companion.ACTION_DISCONNECT"
        const val ACTION_TOGGLE_PAUSE = "com.pokeball.companion.ACTION_TOGGLE_PAUSE"
        const val ACTION_CONNECT_MAC = "com.pokeball.companion.ACTION_CONNECT_MAC"
        const val EXTRA_MAC = "extra_mac"

        var instance: PokeballForegroundService? = null
            private set

        fun start(context: Context) {
            val intent = Intent(context, PokeballForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

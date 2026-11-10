package com.komugi.workout.ui.log

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.komugi.workout.MainActivity
import com.komugi.workout.R
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 全域組間休息計時器。
 *
 * 以「結束時間」記帳（elapsedRealtime），剩餘秒數由現在時間推算，
 * 因此切到其他頁面、切到背景、甚至螢幕休眠後回來，倒數都會是正確的。
 * 震動由計時器自己負責，計時結束時即使不在訓練紀錄頁也會震動；
 * 倒數期間同步顯示通知（含進度），切到別的頁面也看得到。
 */
object RestTimer {

    data class State(
        val visible: Boolean = false,
        val totalSec: Int = 60,
        val remainingSec: Int = 60
    )

    private const val DEFAULT_SECONDS = 60
    private const val ADD_SECONDS = 15
    private const val CHANNEL_ID = "rest_timer"
    private const val NOTIFICATION_ID = 4711

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null
    private var endAtElapsed = 0L
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "組間休息",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "組間休息倒數進度"
                    setShowBadge(false)
                }
            )
        }
    }

    /** 開始倒數（預設 60 秒）。 */
    fun start(seconds: Int = DEFAULT_SECONDS) {
        val safe = seconds.coerceAtLeast(1)
        endAtElapsed = SystemClock.elapsedRealtime() + safe * 1000L
        _state.value = State(visible = true, totalSec = safe, remainingSec = safe)
        publishNotification(_state.value)
        startTicker()
    }

    /** 加時；已結束時重新給這段秒數（與原本「休息結束再 +15 秒」行為一致）。 */
    fun addTime(seconds: Int = ADD_SECONDS) {
        val current = _state.value
        if (!current.visible) return
        val remaining = remainingSeconds()
        endAtElapsed = if (remaining <= 0) {
            SystemClock.elapsedRealtime() + seconds * 1000L
        } else {
            endAtElapsed + seconds * 1000L
        }
        _state.value = current.copy(
            totalSec = current.totalSec + seconds,
            remainingSec = remaining + seconds
        )
        publishNotification(_state.value)
        if (ticker?.isActive != true) startTicker()
    }

    /** 跳過／關閉：回到初始 1:00 並停止。 */
    fun skip() {
        stopTicker()
        endAtElapsed = 0L
        _state.value = State()
        cancelNotification()
    }

    /** 回到前景或進入頁面時校正剩餘秒數（休眠後補上實際流逝時間）。 */
    fun sync() {
        val current = _state.value
        if (!current.visible) return
        val remaining = remainingSeconds()
        if (remaining != current.remainingSec) {
            _state.value = current.copy(remainingSec = remaining)
        }
        publishNotification(_state.value)
        if (remaining <= 0) stopTicker()
    }

    private fun remainingSeconds(): Int = remainingSecondsAt(
        endAt = endAtElapsed,
        now = SystemClock.elapsedRealtime()
    )

    private fun startTicker() {
        stopTicker()
        ticker = scope.launch {
            while (true) {
                val remaining = remainingSeconds()
                val current = _state.value
                if (remaining != current.remainingSec) {
                    _state.value = current.copy(remainingSec = remaining)
                    publishNotification(_state.value)
                }
                if (remaining <= 0) break
                delay(200)
            }
            vibrate()
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    /** 把目前倒數進度寫進通知；沒有通知權限時靜默略過。 */
    private fun publishNotification(state: State) {
        val context = appContext ?: return
        if (!state.visible) {
            cancelNotification()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val remaining = state.remainingSec
        val contentText = if (remaining > 0) {
            "剩餘 ${formatClockText(remaining)}，回到 App 可繼續操作"
        } else {
            "時間到，可以開始下一組了"
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("組間休息")
            .setContentText(contentText)
            .setProgress(state.totalSec, remaining, false)
            .setContentIntent(openApp)
            .setOngoing(remaining > 0)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun cancelNotification() {
        val context = appContext ?: return
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }

    private fun vibrate() {
        val context = appContext ?: return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(400)
        }
    }
}

/**
 * 依結束時間與現在時間推算剩餘秒數（無條件進位，顯示與倒數遞減一致）。
 * 抽成純函式方便測試。
 */
internal fun remainingSecondsAt(endAt: Long, now: Long): Int {
    if (endAt <= 0L) return 0
    val left = endAt - now
    if (left <= 0L) return 0
    return ceil(left / 1000.0).toInt()
}

/** 秒數 →「m:ss」文字（通知與畫面共用格式）。 */
internal fun formatClockText(seconds: Int): String {
    val value = seconds.coerceAtLeast(0)
    val hours = value / 3600
    val minutes = (value % 3600) / 60
    val secs = value % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, secs)
    }
}

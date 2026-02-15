package asia.nana7mi.arirang.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import asia.nana7mi.arirang.R
import asia.nana7mi.arirang.hook.IHookNotify
import asia.nana7mi.arirang.ui.ConfirmDialogActivity

class HookNotifyService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())

    // Binder 用于 IPC 回调
    private val binder = object : IHookNotify.Stub() {
        override fun onPermissionUsed(pkgName: String, opName: String) {
            mainHandler.post {
                val intent = Intent(this@HookNotifyService, ConfirmDialogActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("pkg_name", pkgName)
                    putExtra("op_name", opName)
                }

                // 在 Android 14/15 上，建议显式设置
                val options = android.app.ActivityOptions.makeBasic()
                // 允许从后台启动
                options.setPendingIntentBackgroundActivityStartMode(
                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )

                try {
                    startActivity(intent, options.toBundle())
                } catch (e: Exception) {
                    // 如果还是被拦截，回退到通知方案
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification()) // 必须立刻调用
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder

    private fun buildNotification(): Notification {
        val channelId = "hook_notify"

        val channel = NotificationChannel(
            channelId,
            "HookNotify",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("HookNotify")
            .setContentText("Running...")
            .build()
    }
}

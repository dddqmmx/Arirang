package asia.nana7mi.arirang.ui

import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

// 1. 必须继承 AppCompatActivity (或 ComponentActivity) 才能使用 Dispatcher
class ConfirmDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pkgName = intent.getStringExtra("pkg_name") ?: "Unknown"

        // 兼容 Android 13 (API 33) 的 getParcelableExtra 写法
        val receiver = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("receiver", ResultReceiver::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("receiver")
        }

        // 2. 注册返回键回调 (替代原本的 onBackPressed)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 当用户触发返回手势时，视为"拒绝"
                sendResult(receiver, 0)
            }
        })

        AlertDialog.Builder(this)
            .setTitle("剪切板访问警告")
            .setMessage("应用 [$pkgName] 正在尝试读取剪切板。\n是否允许？")
            // 3. 关键：如果你希望返回键能关闭弹窗并触发上面的回调，
            // 建议设置 Cancelable 为 true，并监听 OnCancel
            .setCancelable(true)
            .setPositiveButton("允许") { _, _ ->
                sendResult(receiver, 1)
            }
            .setNegativeButton("拒绝") { _, _ ->
                sendResult(receiver, 0)
            }
            // 监听对话框被"取消"（例如点击外部或按返回键关闭 Dialog 时）
            .setOnCancelListener {
                sendResult(receiver, 0)
            }
            .setOnDismissListener {
                // 确保 Activity 随 Dialog 销毁
                if (!isFinishing) finish()
            }
            .show()
    }

    private fun sendResult(receiver: ResultReceiver?, resultCode: Int) {
        receiver?.send(resultCode, null)
        finish() // 发送结果后关闭 Activity
    }
}
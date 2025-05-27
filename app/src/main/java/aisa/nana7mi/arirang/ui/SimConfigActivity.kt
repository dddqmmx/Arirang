package aisa.nana7mi.arirang.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import aisa.nana7mi.arirang.R
import android.Manifest
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.collections.forEachIndexed

class SimConfigActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101
    private lateinit var infoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        infoTextView = TextView(this).apply {
            textSize = 16f
            setPadding(16, 16, 16, 16)
        }
        val scrollView = ScrollView(this).apply {
            addView(infoTextView)
        }
        setContentView(scrollView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                PERMISSION_REQUEST_CODE
            )
        } else {
            showAllSimInfo()
            infoTextView.setOnClickListener {
                showAllSimInfo()
            }
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            showAllSimInfo()
        } else {
            infoTextView.text = "Permission denied"
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun showAllSimInfo() {
        val subscriptionManager = getSystemService(SubscriptionManager::class.java)
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        val subscriptionList = subscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionList == null || subscriptionList.isEmpty()) {
            infoTextView.text = "No active SIM cards found."
            return
        }

        val sb = StringBuilder()
        subscriptionList.forEachIndexed { index, subscriptionInfo ->
            val subId = subscriptionInfo.subscriptionId
            val tmForSub = telephonyManager.createForSubscriptionId(subId)

            val simOperatorName = tmForSub.simOperatorName ?: "N/A"
            val networkOperatorName = tmForSub.networkOperatorName ?: "N/A"
            val phoneType = when (tmForSub.phoneType) {
                TelephonyManager.PHONE_TYPE_GSM -> "GSM"
                TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
                TelephonyManager.PHONE_TYPE_SIP -> "SIP"
                else -> "Unknown"
            }
            val simState = when (tmForSub.simState) {
                TelephonyManager.SIM_STATE_ABSENT -> "Absent"
                TelephonyManager.SIM_STATE_READY -> "Ready"
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
                TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
                TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Network Locked"
                else -> "Unknown"
            }

            // 构建输出内容
            sb.append("SIM Card #${index + 1}:\n")
            sb.append("  Display Name: ${subscriptionInfo.displayName}\n")
            sb.append("  Carrier Name: ${subscriptionInfo.carrierName}\n")
            sb.append("  SIM Operator Name: $simOperatorName\n")
            sb.append("  Network Operator Name: $networkOperatorName\n")
            sb.append("  Phone Type: $phoneType\n")
            sb.append("  SIM State: $simState\n")
            sb.append("  Phone Number: ${subscriptionInfo.number ?: "N/A"}\n")
            sb.append("  Country ISO: ${subscriptionInfo.countryIso ?: "N/A"}\n")
            sb.append("  ICC ID: ${subscriptionInfo.iccId ?: "N/A"}\n")
            sb.append("  MCC: ${subscriptionInfo.mccString ?: "N/A"}\n")
            sb.append("  MNC: ${subscriptionInfo.mncString ?: "N/A"}\n")
            sb.append("  Is eSIM: ${subscriptionInfo.isEmbedded}\n")
            sb.append("  Card ID: ${subscriptionInfo.cardId}\n")

            // 以下信息可能受权限或系统版本限制
            try {
                sb.append("  SIM Serial Number: ${tmForSub.simSerialNumber ?: "N/A"}\n")
                sb.append("  Subscriber ID (IMSI): ${tmForSub.subscriberId ?: "N/A"}\n")
                sb.append("  Line 1 Number: ${tmForSub.line1Number ?: "N/A"}\n")
                sb.append("  Group ID Level 1: ${tmForSub.groupIdLevel1 ?: "N/A"}\n")
            } catch (e: SecurityException) {
                sb.append("  (Some sensitive info blocked by system permissions)\n")
            }

            sb.append("\n")
        }

        infoTextView.text = sb.toString()
    }
}
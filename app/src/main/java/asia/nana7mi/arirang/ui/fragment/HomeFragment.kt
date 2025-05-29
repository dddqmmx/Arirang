package asia.nana7mi.arirang.ui.fragment

import asia.nana7mi.arirang.R
import asia.nana7mi.arirang.ui.ClipboardConfigActivity
import asia.nana7mi.arirang.ui.DeviceInfoConfigActivity
import asia.nana7mi.arirang.ui.LocationConfigActivity
import asia.nana7mi.arirang.ui.PackageListConfigActivity
import asia.nana7mi.arirang.ui.SimConfigActivity
import asia.nana7mi.arirang.view.FeatureItemView
import android.content.Intent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    private lateinit var statusCard: CardView
    private lateinit var statusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusCard = view.findViewById<CardView>(R.id.status_card)
        statusText = view.findViewById<TextView>(R.id.status_text)
        if (isXposedActivation()){
            statusCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.enabled_color));
            statusText.setText(R.string.status_activated)
        }
        view.findViewById<FeatureItemView>(R.id.clipboard_setting_bottom).setOnClickListener{
            val intent = Intent(requireContext(), ClipboardConfigActivity::class.java)
            startActivity(intent)
        }
        view.findViewById<FeatureItemView>(R.id.sim_info_setting_bottom).setOnClickListener {
            val intent = Intent(requireContext(), SimConfigActivity::class.java)
            startActivity(intent)
        }
        view.findViewById<FeatureItemView>(R.id.location_setting_bottom).setOnClickListener {
            val intent = Intent(requireContext(), LocationConfigActivity::class.java)
            startActivity(intent)
        }
        view.findViewById<FeatureItemView>(R.id.device_info_setting_bottom).setOnClickListener{
            val intent = Intent(requireContext(), DeviceInfoConfigActivity::class.java)
            startActivity(intent)
        }
        view.findViewById<FeatureItemView>(R.id.package_list_bottom).setOnClickListener{
            val intent = Intent(requireContext(), PackageListConfigActivity::class.java)
            startActivity(intent)
        }
    }

    fun isXposedActivation(): Boolean {
        return false;
    }

}
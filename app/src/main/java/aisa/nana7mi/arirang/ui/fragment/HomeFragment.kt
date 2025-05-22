package aisa.nana7mi.arirang.ui.fragment

import aisa.nana7mi.arirang.R
import aisa.nana7mi.arirang.ui.ClipboardConfigActivity
import aisa.nana7mi.arirang.ui.DeviceInfoConfigActivity
import aisa.nana7mi.arirang.ui.PackageListConfigActivity
import aisa.nana7mi.arirang.view.FeatureItemView
import android.content.Intent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<FeatureItemView>(R.id.clipboard_setting_bottom).setOnClickListener{
            val intent = Intent(requireContext(), ClipboardConfigActivity::class.java)
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

}
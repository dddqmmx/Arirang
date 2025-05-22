package aisa.nana7mi.arirang.ui

import aisa.nana7mi.arirang.R
import aisa.nana7mi.arirang.ui.fragment.HomeFragment
import aisa.nana7mi.arirang.ui.fragment.SettingsFragment
import aisa.nana7mi.arirang.ui.fragment.UserFragment
import android.os.Bundle
import android.util.Log
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        val lang = Locale.getDefault().toString()
        Log.d("LANG", "当前语言: $lang")

        setContentView(R.layout.main)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_settings -> SettingsFragment()
                R.id.nav_user -> UserFragment()
                else -> null
            }
            selectedFragment?.let {
                supportFragmentManager.beginTransaction().replace(R.id.container, it).commit()
                true
            } == true
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(
                R.id.container,
                HomeFragment()
            ).commit()
        }
    }


}

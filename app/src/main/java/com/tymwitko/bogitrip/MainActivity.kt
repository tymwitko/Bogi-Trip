package com.tymwitko.bogitrip

//import com.niels_ole.customtileserver.R

//import jdk.incubator.jpackage.internal.Arguments.CLIOptions.context
import android.Manifest
import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.test.core.app.ApplicationProvider
import com.tymwitko.bogitrip.databinding.ActivityMainBinding
//import org.osmdroid.config.Configuration
import org.osmdroid.config.Configuration.getInstance
import android.content.res.Configuration
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {
        var dLocale: Locale? = Locale("en")
    }

    init {
        updateConfig()
    }

    private fun updateConfig() {
        if(dLocale==Locale("") ) // Do nothing if dLocale is null
            return

        Locale.setDefault(dLocale!!)
        val configuration = Configuration()
        configuration.setLocale(dLocale)
        applyOverrideConfiguration(configuration)
    }

//    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    val TAG = "TAG"
//    private lateinit var map : MapView

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this@MainActivity,
                permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(permission.ACCESS_FINE_LOCATION), 1)
        }
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        title = "Bogi Trip"
        val binding = ActivityMainBinding.inflate(layoutInflater)


        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // Instantiate the navController using the NavHostFragment
        navController = navHostFragment.navController
        // Make sure actions in the ActionBar get propagated to the NavController
        setupActionBarWithNavController(navController)

        val change = "en"

        dLocale = Locale(change)


        setContentView(binding.root)
//        setContentView(R.layout.activity_main)

//        findViewById<TextView>(R.id.textView).movementMethod = LinkMovementMethod.getInstance()

    }

//    /**
//     * Enables back button support. Simply navigates one element up on the stack.
//     */
//    override fun onSupportNavigateUp(): Boolean {
//        return navController.navigateUp() || super.onSupportNavigateUp()
//    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        val permissionsToRequest = ArrayList<String>()
//        var i = 0
//        while (i < grantResults.size) {
//            permissionsToRequest.add(permissions[i])
//            i++
//        }
//        if (permissionsToRequest.size > 0) {
//            ActivityCompat.requestPermissions(
//                this,
//                permissionsToRequest.toTypedArray(),
//                REQUEST_PERMISSIONS_REQUEST_CODE)
//        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStartCalled")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume Called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause Called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop Called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy Called")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart Called")
    }


//    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
//        var permissionsToRequest: Array<String> = emptyArray()
//        for (permission in permissions) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission)
//                != PackageManager.PERMISSION_GRANTED) {
//                // Permission is not granted
//                permissionsToRequest.append(Manifest.permission);
//            }
//        }
//        if (permissionsToRequest.size() > 0) {
//            ActivityCompat.requestPermissions(
//                this,
//                permissionsToRequest.toArray("0"),
//                REQUEST_PERMISSIONS_REQUEST_CODE)
//        }
//    }
}
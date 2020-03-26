package com.iseokchan.dorandoran

import android.app.Application
import android.os.Handler
import android.widget.Toast
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class DoranDoranApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        checkMinimumVersionCode()
    }

    private fun checkMinimumVersionCode() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(60*60*6)
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            val minimumVersionCode = remoteConfig.getValue("minimum_version_code").asLong()
            val currentVersionCode = BuildConfig.VERSION_CODE

            if (minimumVersionCode > currentVersionCode) { // minimum version code check fails
                Toast.makeText(
                    this,
                    getString(R.string.installNewVersion),
                    Toast.LENGTH_LONG
                ).show()

                Handler().postDelayed({
                    android.os.Process.killProcess(android.os.Process.myPid())
                }, 5000L)

                return@addOnCompleteListener
            }
        }
    }
}
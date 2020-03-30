package com.iseokchan.dorandoran

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings


class DoranDoranApplication : Application() {

    companion object {

        var isSplash = false;

        lateinit var firebaseAuth: FirebaseAuth
        lateinit var rootRef: FirebaseFirestore

        lateinit var googleSignInOptions: GoogleSignInOptions

        fun addFcmToken(token: String) {
            firebaseAuth.currentUser?.uid?.let { uid ->
                val userTokenRef = rootRef.collection("fcmTokens").document(uid)

                userTokenRef.set(
                    mapOf("tokens" to mapOf(token to true)), SetOptions.merge()
                )
            }
        }

        fun checkIfLoggedIn(callBack: () -> Unit ) {
            if(firebaseAuth.currentUser == null) {
                callBack()
            }
        }

        fun signOut(activity: Activity) {
            GoogleSignIn.getClient(activity, googleSignInOptions)?.let {
                firebaseAuth.signOut()
                it.signOut()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        firebaseAuth = FirebaseAuth.getInstance()
        rootRef = FirebaseFirestore.getInstance()

        googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { task ->
            addFcmToken(task.token)
        }

        firebaseAuth.addAuthStateListener {
            if(it.currentUser == null) {
                resetToLoginActivity()
            }
        }

        checkMinimumVersionCode()
    }

    private fun checkMinimumVersionCode() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(60*60*6)
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate()
    }

    fun resetToLoginActivity() {
        (applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).let {
            if(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    it.appTasks.size > 0
                } else {
                    it.getRunningTasks(Int.MAX_VALUE).size > 0
                } && isSplash
            ) {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }
}
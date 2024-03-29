package com.iseokchan.dorandoran

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.iseokchan.dorandoran.DoranDoranApplication.Companion.firebaseAuth
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.lang.Exception

class SplashActivity : AppCompatActivity() {

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser

        if (currentUser != null) {

            val intent = Intent(this@SplashActivity, ChatListActivity::class.java)
            startActivity(intent)
            finish()

        }
    }
}
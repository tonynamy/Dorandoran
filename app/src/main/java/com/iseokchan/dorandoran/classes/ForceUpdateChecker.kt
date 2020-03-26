package com.iseokchan.dorandoran.classes

import android.app.Activity
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.iseokchan.dorandoran.BuildConfig


class ForceUpdateChecker(
    val activity: Activity,
    onUpdateNeededListener: OnUpdateNeededListener?
) {
    private val onUpdateNeededListener: OnUpdateNeededListener?

    interface OnUpdateNeededListener {
        fun onUpdateNeeded(updateUrl: String?)
    }

    fun check() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val updateCheck = remoteConfig.getBoolean(KEY_UPDATE_REQUIRED)

        if (updateCheck) {
            val minimumVersionCode =
                remoteConfig.getLong(KEY_MINIMUM_VERSION)

            val currentVersionCode = getAppVersionCode()

            val updateUrl =
                remoteConfig.getString(KEY_UPDATE_URL)

            if (minimumVersionCode > currentVersionCode) {
                onUpdateNeededListener?.onUpdateNeeded(updateUrl)
            }
        }
    }

    private fun getAppVersionCode(): Int = BuildConfig.VERSION_CODE

    class Builder(private val activity: Activity) {
        private var onUpdateNeededListener: OnUpdateNeededListener? = null

        fun onUpdateNeeded(onUpdateNeededListener: OnUpdateNeededListener?): Builder {
            this.onUpdateNeededListener = onUpdateNeededListener
            return this
        }

        fun build(): ForceUpdateChecker {
            return ForceUpdateChecker(activity, onUpdateNeededListener)
        }

        fun check(): ForceUpdateChecker {
            val forceUpdateChecker = build()
            forceUpdateChecker.check()
            return forceUpdateChecker
        }

    }

    companion object {
        private val TAG = ForceUpdateChecker::class.java.simpleName

        const val KEY_UPDATE_REQUIRED = "force_update_enabled"
        const val KEY_MINIMUM_VERSION = "minimum_version_code"
        const val KEY_UPDATE_URL = "google_playstore_url"

        fun with(activity: Activity): Builder {
            return Builder(activity)
        }
    }

    init {
        this.onUpdateNeededListener = onUpdateNeededListener
    }
}
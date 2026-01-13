package com.thinh.snaplet

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber


const val LOG_TAG = "Timber"

@HiltAndroidApp
class HiltApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeTimber()
    }

    private fun initializeTimber() {
        Timber.tag(LOG_TAG)
        if (BuildConfig.DEBUG) {
            Logger.plant(Timber.DebugTree())
        } else {
            Logger.plant(ReleaseTree())
        }
    }

    private class ReleaseTree : Timber.Tree() {
        @SuppressLint("LogNotTimber")
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.ERROR || priority == Log.ASSERT) {
                Log.e(LOG_TAG, message, t)
                // Log to crash reporting service (Firebase Crashlytics, Sentry, etc.)
                // Example: FirebaseCrashlytics.getInstance().log("$tag: $message")
                // if (t != null) FirebaseCrashlytics.getInstance().recordException(t)
            }
        }
    }
}


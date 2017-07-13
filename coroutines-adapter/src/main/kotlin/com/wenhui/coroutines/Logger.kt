package com.wenhui.coroutines

import android.util.Log


internal val DEBUG_TAG = "CoroutinesAdapter"

internal fun log(message: String) {
    if (BuildConfig.DEBUG) {
        Log.d(DEBUG_TAG, "[${Thread.currentThread().name}] $message")
    }
}
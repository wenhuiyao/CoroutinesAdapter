package com.wenhui.coroutines

import android.os.Process
import android.util.Log

/**
 * For debugging
 */

internal val DEBUG_TAG = "CoroutinesAdapter"

internal fun log(message: String) = Log.d(DEBUG_TAG, message)

internal fun logThreadPriority(context: CoroutineContexts) {
    val myId = Process.getThreadPriority(Process.myTid())
    val priority = when(myId) {
        Process.THREAD_PRIORITY_BACKGROUND -> "BACKGROUND"
        Process.THREAD_PRIORITY_DEFAULT -> "DEFAULT"
        Process.THREAD_PRIORITY_DISPLAY -> "DISPLAY"
        Process.THREAD_PRIORITY_FOREGROUND -> "FOREGROUND"
        Process.THREAD_PRIORITY_LOWEST -> "LOWEST"
        else -> myId.toString()
    }

    Log.d(DEBUG_TAG, "[${context.name}] Thread priority is $priority")
}
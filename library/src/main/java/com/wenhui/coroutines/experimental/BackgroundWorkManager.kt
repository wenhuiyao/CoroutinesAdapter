package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.Job

/**
 * Manager for background work. Use this to cancel or check background works are active or not
 */
class BackgroundWorkManager {

    internal val jobRef = Job()
    internal val activeWorks = ArrayList<Work>(3)

    fun cancelAllWorks() {
        jobRef.cancel()
    }

    fun hasActiveWorks() = !activeWorks.isEmpty()
}
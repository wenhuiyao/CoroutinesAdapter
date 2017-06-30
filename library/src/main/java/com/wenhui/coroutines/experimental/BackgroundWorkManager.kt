package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.Job

/**
 * Manager for background work. Use this to cancel or check background works are active or not
 */
class BackgroundWorkManager {

    internal val jobRef = Job()
    private val activeJobs = ArrayList<Job>(3)

    internal fun manageJob(job: Job){
        job.invokeOnCompletion {
            activeJobs.remove(job)
        }
        activeJobs.add(job)
    }

    fun cancelAllWorks() {
        jobRef.cancel()
    }

    fun hasActiveWorks() = !activeJobs.isEmpty()
}
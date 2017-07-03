package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.Job

/**
 * Manager for background work. Use this to cancel or check background works are active or not
 */
class BackgroundWorkManager {

    private val monitoredJob = Job()
    private val activeJobs = ArrayList<Job>(3)

    internal fun manageJob(job: Job) {
        job.invokeOnCompletion {
            activeJobs.remove(job)
        }
        activeJobs.add(job)
    }

    fun cancelAllWorks() {
        monitoredJob.cancel()
        for (job in activeJobs) {
            job.cancel()
        }
    }

    fun hasActiveWorks() = !activeJobs.isEmpty()
}
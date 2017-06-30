package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.Job
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Manager for background work. Use this to cancel or check background works are active or not
 */
class BackgroundWorkManager {

    private val monitoredJob = Job()
    private val activeJobs = ArrayList<Job>(3)

    internal fun monitorJobWithNewContext(context: CoroutineContext) = context + monitoredJob

    internal fun manageJob(job: Job){
        job.invokeOnCompletion {
            activeJobs.remove(job)
        }
        activeJobs.add(job)
    }

    fun cancelAllWorks() {
        monitoredJob.cancel()
    }

    fun hasActiveWorks() = !activeJobs.isEmpty()
}
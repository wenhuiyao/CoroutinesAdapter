package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.Job

/**
 * Manager for background work. Use this to cancel or check background works are active or not
 */
class BackgroundWorkManager {

    private val activeJobs = ArrayList<Job>(3)

    internal fun manageJob(job: Job) {
        job.invokeOnCompletion {
            activeJobs.remove(job)
        }
        activeJobs.add(job)
    }

    /**
     * Cancel all the works managed by this manager
     */
    fun cancelAllWorks() {
        for (job in activeJobs) {
            job.cancel()
        }
    }

    /**
     * Return `true` if there is at least one active work
     */
    fun hasActiveWorks() = !activeJobs.isEmpty()
}

interface Manageable<W> {

    /**
     * Allow auto manage by [BackgroundWorkManager], so when [BackgroundWorkManager.cancelAllWorks] is called, the
     * current work will be cancelled
     */
    fun manageBy(manager: BackgroundWorkManager): W
}
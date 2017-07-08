package com.wenhui.coroutines

import kotlinx.coroutines.experimental.Job
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manager of coroutine works. Use this to cancel all the active works.
 */
class WorkManager {

    private val activeJobs = CopyOnWriteArrayList<Job>()

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
    fun hasActiveWorks() = activeJobs.any { it.isActive }

}

interface Manageable<W> {

    /**
     * Allow auto manage by [WorkManager], so when [WorkManager.cancelAllWorks] is called, the
     * current work will be cancelled
     */
    fun manageBy(manager: WorkManager): W
}
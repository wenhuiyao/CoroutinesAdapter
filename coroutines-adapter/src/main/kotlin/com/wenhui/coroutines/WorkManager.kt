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
            // This may be called from different thread, so it is important to keep it synchronous
            activeJobs.remove(job)
        }
        activeJobs.add(job)
    }

    /**
     * Cancel all the works managed by this manager
     */
    fun cancelAllWorks() {
        activeJobs.forEach { it.cancel() }
    }

    /**
     * Return `true` if there is at least one active work
     */
    fun hasActiveWork() = activeJobs.any { it.isActive }

}

interface Manageable<S> {

    /**
     * Allow auto manage by [WorkManager], so when [WorkManager.cancelAllWorks] is called, the
     * current work will be cancelled
     */
    fun manageBy(manager: WorkManager): S
}
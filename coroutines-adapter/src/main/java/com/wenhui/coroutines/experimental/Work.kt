package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.Job


/**
 * Represent a background work, which can be [cancel] when needed.
 */
interface Work {

    /**
     * Returns `true` when this work is active.
     */
    val isActive: Boolean

    /**
     * Returns `true` when this work has completed for any reason, even if it is cancelled.
     */
    val isCompleted: Boolean


    /**
     * Cancel this work. The result is `true` if this work was
     * cancelled as a result of this invocation and `false` otherwise
     */
    fun cancel(): Boolean

    /**
     * Allow auto manage by [BackgroundWorkManager], so when [BackgroundWorkManager.cancelAllWorks] is called, the
     * current work will be cancelled
     */
    fun manageBy(manager: BackgroundWorkManager): Work

}

internal fun newWork(job: Job): Work = WorkImpl(job)

private class WorkImpl(private val job: Job) : Work {

    override val isActive = job.isActive
    override val isCompleted = job.isCompleted

    override fun cancel() = job.cancel()

    override fun manageBy(manager: BackgroundWorkManager): Work {
        manager.manageJob(job)
        return this
    }
}
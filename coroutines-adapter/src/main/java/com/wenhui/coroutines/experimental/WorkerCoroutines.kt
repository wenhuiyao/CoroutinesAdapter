
package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.Job

/**
 * Utility method to create a new background work
 */
internal fun <T> Executor<T>.newWorker(): Operator<T, Work> = WorkerImpl(this)

/**
 * Represent a background work, which can be [cancel] when needed.
 */
interface Work: Manageable<Work> {

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

}

private class WorkImpl(private val job: Job) : Work {

    override val isActive = job.isActive
    override val isCompleted = job.isCompleted

    override fun cancel() = job.cancel()

    override fun manageBy(manager: BackgroundWorkManager): Work {
        manager.manageJob(job)
        return this
    }
}


private class WorkerImpl<T>(work: Executor<T>) : BaseWorker<T, Work>(work) {

    override fun <R> newWorker(executor: Executor<R>): Operator<R, Work> = WorkerImpl(executor)

    override fun start(): Work = WorkImpl(executeWork(CONTEXT_BG))
}


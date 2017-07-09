package com.wenhui.coroutines

import kotlinx.coroutines.experimental.Job

/**
 * Utility method to create a new background work
 */
internal fun <T> newWorker(executor: Executor<T>): Operator<T, Work> = WorkerImpl(executor)

/**
 * Represent a background work, which can be [cancel] when needed.
 */
interface Work : Manageable<Work> {

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

class WorkImpl(val job: Job) : Work {

    override val isActive: Boolean get() = job.isActive

    override val isCompleted: Boolean get() = job.isCompleted

    override fun cancel() = job.cancel()

    override fun manageBy(manager: WorkManager): Work {
        manager.manageJob(job)
        return this
    }
}


private class WorkerImpl<T>(executor: Executor<T>) : BaseWorker<T, Work>(executor) {

    override fun <R> newWorker(executor: Executor<R>): Operator<R, Work> = WorkerImpl(executor)

    override fun start(): Work = WorkImpl(executeWork(CONTEXT_BG))
}


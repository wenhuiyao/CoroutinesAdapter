package com.wenhui.coroutines

import kotlinx.coroutines.experimental.Job

/**
 * Utility method to create a new background work
 */
internal fun <T> newWorker(action: Action<T>): Worker<T, Work> = WorkerImpl(action)

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

internal class WorkImpl(val job: Job) : Work {

    override val isActive: Boolean get() = job.isActive

    override val isCompleted: Boolean get() = job.isCompleted

    override fun cancel() = job.cancel()

    override fun manageBy(manager: WorkManager): Work {
        manager.manageJob(job)
        return this
    }
}


private class WorkerImpl<T>(action: Action<T>) : BaseWorker<T, Work>(action) {

    override fun <R> newWorker(action: Action<R>): Worker<R, Work> = WorkerImpl(action)

    override fun start(): Work = WorkImpl(executeWork(CONTEXT_BG))
}


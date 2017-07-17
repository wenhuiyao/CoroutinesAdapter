package com.wenhui.coroutines

import kotlinx.coroutines.experimental.Job

/**
 * Utility method to create a new background work
 */
internal fun <T> newFutureWork(action: Action<T>): FutureWork<T, Worker> = FutureWorkImpl(action)

/**
 * Represent a background work, which can be [cancel] when needed.
 */
interface Worker : Manageable<Worker> {

    /**
     * Returns `true` when this work has completed for any reason, included cancellation.
     */
    val isCompleted: Boolean


    /**
     * Cancel this work. The result is `true` if this work was
     * cancelled as a result of this invocation and `false` otherwise
     */
    fun cancel(): Boolean

}

internal class WorkerImpl(val job: Job) : Worker {

    override val isCompleted: Boolean get() = job.isCompleted

    override fun cancel() = job.cancel()

    override fun manageBy(manager: WorkManager): Worker {
        manager.manageJob(job)
        return this
    }
}


private class FutureWorkImpl<T>(action: Action<T>) : BaseFutureWork<T, Worker>(action) {

    override fun <R> newWorker(action: Action<R>): FutureWork<R, Worker> = FutureWorkImpl(action)

    override fun start(): Worker = WorkerImpl(executeWork(CONTEXT_BG))
}


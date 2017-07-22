package com.wenhui.coroutines

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.ExecutionException

/**
 * Utility method to create a new background work
 */
internal fun <T> newFutureWork(action: Action<T>): FutureWork<T> = FutureWorkImpl(action)

interface FutureWork<T> : Work<T, Worker> {
    /**
     * Synchronous call to get the computation result, throw exception if there are errors
     *
     * NOTE: this will block the current thread to wait of the result
     *
     * @throws [ExecutionException]
     * @throws [CancellationException]
     */
    fun get(): T
}

class ExecutionException(cause: Throwable) : Exception(cause)
class CancellationException(message: String = "Execution cancelled") : Exception(message)

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


private class FutureWorkImpl<T>(private val action: Action<T>) : FutureWork<T>, BaseWork<T, Worker>(action) {

    override fun <R> newWorker(action: Action<R>): Work<R, Worker> = FutureWorkImpl(action)

    override fun start(): Worker = WorkerImpl(executeWork(CONTEXT_BG))

    override fun get(): T {
        var result: T? = null
        var exception: Throwable = CancellationException()
        runBlocking {
            val aResult = async(context) { action.perform(this) }
            try {
                result = aResult.await()
            } catch(e: Throwable) {
                if (shouldReportException(e) && e !is kotlinx.coroutines.experimental.CancellationException) {
                    exception = ExecutionException(e)
                } else {
                    exception = CancellationException()
                }
            }
        }

        return result ?: throw exception
    }
}


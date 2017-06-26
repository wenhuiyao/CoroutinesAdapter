@file:JvmName("Coroutines")

package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import kotlin.coroutines.experimental.CoroutineContext


@JvmField val CONTEXT_BG = newFixedThreadPoolContext(2 * Runtime.getRuntime().availableProcessors(), "context_bg")
@JvmField val CONTEXT_UI = UI

/**
 * Force work not to be cancelled when [Working.cancel] is called
 */
@JvmField val CONTEXT_NON_CANCELLABLE = NonCancellable

fun <T> newBackgroundWork(action: Action<T>) = createBackgroundWork(ActionWork(action))
fun <T, R> newBackgroundWork(arg: T, action: TransformAction<T, R>) = createBackgroundWork(Action1Work(arg, action))

/**
 * Typical action that start a block of code and return a result
 */
typealias Action<T> = () -> T

/**
 * Transform a value to another value from type T to type R
 */
typealias TransformAction<T, R> = (T) -> R

/**
 * Complete an action, no result will be returned
 */
typealias CompleteAction<T> = (T) -> Unit

interface CompleteListener<T> {
    fun onSuccess(action: CompleteAction<T>): Worker<T>
    fun onError(action: CompleteAction<Throwable>): Worker<T>
}

interface Delay<T> {
    fun setStartDelay(delay: Long): Worker<T>
}

interface WorkStarter {
    fun start(): Working
}

interface Worker<T> : CompleteListener<T>, WorkStarter, Delay<T>

interface Transformer<T> : Worker<T> {
    /**
     * Transform a source from type T to type R in background thread
     */
    fun <R> transform(action: TransformAction<T, R>): Transformer<R> = transform(CONTEXT_BG, action)

    /**
     * Transform a source from type T to type R
     *
     * @param context: one of [CONTEXT_BG], [CONTEXT_UI], [CONTEXT_NON_CANCELLABLE]
     */
    fun <R> transform(context: CoroutineContext, action: TransformAction<T, R>): Transformer<R>
}

abstract class Work<out T> {

    // by default it is cancellable
    open var cancellable = true

    abstract suspend fun startWork(scope: CoroutineScope): T

    protected fun ensureWorkActive(scope: CoroutineScope) {
        if (cancellable && !scope.isActive) {
            throw CancellationException()
        }
    }
}

/**
 * Background work in progress. It can be [cancel] when needed.
 * To listen to work complete, use [completionHandler]
 */
interface Working {

    /**
     * Returns `true` when this job is active.
     */
    val isActive: Boolean

    /**
     * Returns `true` when this job has completed for any reason, even if it is cancelled.
     */
    val isCompleted: Boolean

    /**
     * Signaling when the job is finished for any reason, regardless it is success or failure.
     */
    var completionHandler: CompletionHandler?

    /**
     * Cancel this job. The result is `true` if this job was
     * cancelled as a result of this invocation and `false` otherwise
     */
    fun cancel(): Boolean
}

private class ActionWork<out T>(private val action: Action<T>) : Work<T>() {

    suspend override fun startWork(scope: CoroutineScope): T {
        ensureWorkActive(scope)
        return action()
    }
}

private class Action1Work<T, out R>(
        private val arg: T,
        private val action: TransformAction<T, R>) : Work<R>() {

    suspend override fun startWork(scope: CoroutineScope): R {
        ensureWorkActive(scope)
        return action(arg)
    }

}
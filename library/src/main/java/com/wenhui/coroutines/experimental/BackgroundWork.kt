@file:JvmName("Coroutines")
package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.async

/**
 * Create a new background work with _action_
 */
fun <T> backgroundWork(action: Action<T>) = createBackgroundWork(ActionWork(action))

/**
 * Create a new background work with an action, and an argument that will pass into the action
 */
fun <T, R> backgroundWork(arg: T, action: TransformAction<T, R>) = createBackgroundWork(Action1Work(arg, action))

/**
 * Create a new background work with two actions, and later two results can be merged
 */
fun <T1, T2, R> mergeBackgroundWork(action1: Action<T1>, action2: Action<T2>): Merger<T1, T2, R> = MergeActionWork (action1, action2)

/**
 * Create a new background work with three actions, and later three results can be merged
 */
fun <T1, T2, T3, R> mergeBackgroundWork(action1: Action<T1>, action2: Action<T2>, action3: Action<T3>): TriMerger<T1, T2, T3, R> = TriMergeActionWork(action1, action2, action3)


/**
 * Typical action that start a block of code and return a result
 */
typealias Action<T> = () -> T

/**
 * Transform a value to another value from type T to type R
 */
typealias TransformAction<T, R> = (T) -> R
typealias ParametrizedAction<T> = (T) -> Unit
typealias MergeAction<T1, T2, R> = (T1, T2) -> R
typealias TriMergeAction<T1, T2, T3, R> = (T1, T2, T3) -> R

interface WorkCompletion<T> {

    /**
     * Callback when work succeeded. This is going to be called on UI thread
     */
    fun onSuccess(action: ParametrizedAction<T>): Worker<T>

    /**
     * Callback when work failed. This is going to be called on UI thread
     */
    fun onError(action: ParametrizedAction<Throwable>): Worker<T>
}

interface WorkManager<T> {
    fun manageBy(manager: BackgroundWorkManager): Worker<T>
}

interface WorkStarter<T> {
    fun setStartDelay(delay: Long): Worker<T>
    fun start(): Work
}

interface Worker<T> : WorkCompletion<T>, WorkStarter<T>, WorkManager<T>

interface Operator<T> : Worker<T> {
    /**
     * Transform a source from type T to type R in background thread
     */
    fun <R> transform(action: TransformAction<T, R>): Operator<R> = transform(CoroutineContexts.BACKGROUND, action)

    /**
     * Transform a source from type T to type R
     *
     * @param context: The context where is transformation will be executed
     */
    fun <R> transform(context: CoroutineContexts, action: TransformAction<T, R>): Operator<R>

    /**
     * Operate on the item, by default it is running on the background
     */
    fun operate(action: ParametrizedAction<T>): Operator<T> = operate(CoroutineContexts.BACKGROUND, action)

    /**
     *  @param context: The context where is transformation will be executed
     */
    fun operate(context: CoroutineContexts, action: ParametrizedAction<T>): Operator<T>
}

interface Merger<T1, T2, R> {
    fun merge(action: MergeAction<T1, T2, R>): Operator<R>
}

interface TriMerger<T1, T2, T3, R> {
    fun merge(action: TriMergeAction<T1, T2, T3, R>): Operator<R>
}

abstract class BaseWork<out T> {

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
 */
interface Work {

    /**
     * Returns `true` when this jobRef is active.
     */
    val isActive: Boolean

    /**
     * Returns `true` when this jobRef has completed for any reason, even if it is cancelled.
     */
    val isCompleted: Boolean


    /**
     * Cancel this jobRef. The result is `true` if this jobRef was
     * cancelled as a result of this invocation and `false` otherwise
     */
    fun cancel(): Boolean
}

private class ActionWork<out T>(private val action: Action<T>) : BaseWork<T>() {

    suspend override fun startWork(scope: CoroutineScope): T {
        ensureWorkActive(scope)
        return action()
    }
}

private class Action1Work<T, out R>(
        private val arg: T,
        private val action: TransformAction<T, R>) : BaseWork<R>() {

    suspend override fun startWork(scope: CoroutineScope): R {
        ensureWorkActive(scope)
        return action(arg)
    }

}

private class MergeActionWork<T1, T2, R>(
        private val action1: Action<T1>,
        private val action2: Action<T2>): Merger<T1, T2, R>, BaseWork<R>() {

    private lateinit var mergeAction: MergeAction<T1, T2, R>

    suspend override fun startWork(scope: CoroutineScope): R {
        ensureWorkActive(scope)

        val result1 = async(scope.context) { action1() }
        val result2 = async(scope.context) { action2() }

        return mergeAction(result1.await(), result2.await())
    }

    override fun merge(mergeAction: MergeAction<T1, T2, R>): Operator<R> {
        this.mergeAction = mergeAction
        return createBackgroundWork(this)
    }
}

private class TriMergeActionWork<T1, T2, T3, R>(
        private val action1: Action<T1>,
        private val action2: Action<T2>,
        private val action3: Action<T3>): TriMerger<T1, T2, T3, R>, BaseWork<R>() {

    private lateinit var mergeAction: TriMergeAction<T1, T2, T3, R>

    suspend override fun startWork(scope: CoroutineScope): R {
        ensureWorkActive(scope)

        val result1 = async(scope.context) { action1() }
        val result2 = async(scope.context) { action2() }
        val result3 = async(scope.context) { action3() }

        return mergeAction(result1.await(), result2.await(), result3.await())
    }

    override fun merge(mergeAction: TriMergeAction<T1, T2, T3, R>): Operator<R> {
        this.mergeAction = mergeAction
        return createBackgroundWork(this)
    }
}


package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Transform a value from type T to type R
 */
internal typealias TransformAction<T, R> = (T) -> R
internal typealias ParametrizedAction<T> = (T) -> Unit
internal typealias FilterAction<T> = (T) -> Boolean

interface WorkCompletion<T, W> {

    /**
     * Callback when work succeeded. This is going to be called on UI thread
     */
    fun onSuccess(action: ParametrizedAction<T>): Worker<T, W>

    /**
     * Callback when work failed. This is going to be called on UI thread
     */
    fun onError(action: ParametrizedAction<Throwable>): Worker<T, W>
}

interface WorkStarter<T, W> {
    fun setStartDelay(delay: Long): Worker<T, W>

    /**
     * This must be called to start the work
     */
    fun start(): W
}

interface Worker<T, W> : WorkCompletion<T, W>, WorkStarter<T, W>

interface Operator<T, W> : Worker<T, W> {
    /**
     * Transform a source from type T to type R in background thread
     */
    fun <R> transform(action: TransformAction<T, R>): Operator<R, W> = transform(CoroutineContexts.BACKGROUND, action)

    /**
     * Transform a source from type T to type R
     *
     * @param context: The context where is transformation will be executed
     */
    fun <R> transform(context: CoroutineContexts, action: TransformAction<T, R>): Operator<R, W>

    /**
     * Operate on the item, by default it is running on the background
     */
    fun operate(action: ParametrizedAction<T>): Operator<T, W> = operate(CoroutineContexts.BACKGROUND, action)

    /**
     *  @param context: The context where is transformation will be executed
     */
    fun operate(context: CoroutineContexts, action: ParametrizedAction<T>): Operator<T, W>

    /**
     * Filter an item, return `true` is the item is valid, `false` to ignore the item
     * NOTE: the filter action will be running on the same thread as last action whichever that is
     */
    fun filter(action: FilterAction<T>): Operator<T, W> = filter(CoroutineContexts.BACKGROUND, action)

    /**
     * Filter an item, return `true` is the item is valid, `false` to ignore the item
     * NOTE: the filter action will be running on the same thread as last action whichever that is
     */
    fun filter(context: CoroutineContexts, action: FilterAction<T>): Operator<T, W>
}

/**
 * Base worker that doing all the essential works
 */
internal abstract class BaseWorker<T, W>(private val work: Executor<T>) : Operator<T, W> {

    protected var successAction: ParametrizedAction<T>? = null
    protected var errorAction: ParametrizedAction<Throwable>? = null
    protected var startDelay = 0L

    override fun <R> transform(context: CoroutineContexts, action: TransformAction<T, R>): Operator<R, W> {
        val executor = Transformation(work, action, context)
        return newWorker(executor)
    }

    override fun operate(context: CoroutineContexts, action: ParametrizedAction<T>): Operator<T, W> {
        val executor = Operation(work, action, context)
        return newWorker(executor)
    }

    override fun filter(context: CoroutineContexts, action: (T) -> Boolean): Operator<T, W> {
        val executor = Filtration(work, action, context)
        return newWorker(executor)
    }

    protected abstract fun <R> newWorker(executor: Executor<R>): Operator<R, W>

    override fun onSuccess(action: ParametrizedAction<T>): Worker<T, W> {
        if (successAction != null) {
            throw IllegalArgumentException("onSuccess() is called twice")
        }
        successAction = action
        return this
    }

    override fun onError(action: ParametrizedAction<Throwable>): Worker<T, W> {
        if (errorAction != null) {
            throw IllegalArgumentException("onError() is called twice")
        }
        errorAction = action
        return this
    }

    override fun setStartDelay(delay: Long): Worker<T, W> {
        startDelay = delay
        return this
    }

    protected fun executeWork(context: CoroutineContext): Job = launch(context) {
        if (startDelay > 0) {
            delay(startDelay)
        }

        try {
            val response = work.execute(this)
            if (isActive) { // make sure jobRef is not yet cancelled
                kotlinx.coroutines.experimental.run(CONTEXT_UI) {
                    successAction?.invoke(response)
                }
            }
        } catch(exception: Throwable) {
            exception.printStackTrace()
            if (isActive && exception !is IgnoreException) { // make sure jobRef is not yet cancelled
                kotlinx.coroutines.experimental.run(CONTEXT_UI) {
                    errorAction?.invoke(exception)
                }
            }
        }
    }
}



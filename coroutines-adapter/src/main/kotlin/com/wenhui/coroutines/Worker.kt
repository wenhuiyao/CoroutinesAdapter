package com.wenhui.coroutines

import com.wenhui.coroutines.functions.ConsumeAction
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Transform a value from type T to type R
 */
internal typealias TransformAction<T, R> = (T) -> R
// Kotlin doesn't support java SAM type conversion, this is to workaround that issue: https://discuss.kotlinlang.org/t/kotlin-and-sam-interface-with-two-parameters/293/18
internal typealias KConsumeAction<T> = (T) -> Unit
internal typealias FilterAction<T> = (T) -> Boolean

internal class KConsumeActionWrapper<T>(private val action: KConsumeAction<T>): ConsumeAction<T> {
    override fun invoke(item: T) = action(item)
}

interface Worker<T, W> : Operator<T, W>, WorkStarter<T, W>


interface WorkStarter<T, W> : CompleteNotifier<T, W>, Starter<T, W>


interface Operator<T, W> {
    /**
     * Transform a source from type T to type R in background thread
     */
    fun <M> transform(action: TransformAction<T, M>): Worker<M, W> = transform(CoroutineContexts.BACKGROUND, action)

    /**
     * Transform a source from type T to type R
     * @param context: The context where is transformation will be executed
     */
    fun <M> transform(context: CoroutineContexts, action: TransformAction<T, M>): Worker<M, W>

    /**
     * Consume the item, by default it is running in the background
     */
    fun consume(action: ConsumeAction<T>): Worker<T, W> = consume(CoroutineContexts.BACKGROUND, action)

    /**
     *  @param context: The context where the consume action will be executed
     */
    fun consume(context: CoroutineContexts, action: ConsumeAction<T>): Worker<T, W>

    /**
     * Kotlin specific version of consume
     */
    fun consume(context: CoroutineContexts = CoroutineContexts.BACKGROUND, action: KConsumeAction<T>): Worker<T, W>
            = consume(context, KConsumeActionWrapper(action))

    /**
     * Filter an item in background thread, return `true` is the item is valid, `false` to ignore the item
     */
    fun filter(action: FilterAction<T>): Worker<T, W> = filter(CoroutineContexts.BACKGROUND, action)

    /**
     * Filter an item, return `true` is the item is valid, `false` to ignore the item
     * @param context: The context where the filter action will be executed
     */
    fun filter(context: CoroutineContexts, action: FilterAction<T>): Worker<T, W>
}


interface CompleteNotifier<T, W> {

    /**
     * Callback when execution succeeded
     */
    fun onSuccess(action: ConsumeAction<T>): WorkStarter<T, W>

    /**
     * [Kotlin version] Callback when execution succeeded
     */
    fun onSuccess(action: KConsumeAction<T>): WorkStarter<T, W> = onSuccess(KConsumeActionWrapper(action))

    /**
     * Callback when there is exception
     */
    fun onError(action: ConsumeAction<Throwable>): WorkStarter<T, W>

    /**
     * [Kotlin version] Callback when there is exception
     */
    fun onError(action: KConsumeAction<Throwable>): WorkStarter<T, W> = onError(KConsumeActionWrapper(action))
}

/**
 * `W` The return type after [start], should normally allow the work to be cancel
 */
interface Starter<T, W> {

    fun setStartDelay(delay: Long): WorkStarter<T, W>

    /**
     * This must be called to start the work
     */
    fun start(): W
}

/**
 * Base worker that doing all the essential works
 */
internal abstract class BaseWorker<T, W>(private val executor: Executor<T>) : Worker<T, W> {

    private var successAction: ConsumeAction<T>? = null
    private var errorAction: ConsumeAction<Throwable>? = null
    private var startDelay = 0L

    override fun <M> transform(context: CoroutineContexts, action: TransformAction<T, M>): Worker<M, W> {
        return newWorker(Transformer(executor, context, action))
    }

    override fun consume(context: CoroutineContexts, action: ConsumeAction<T>): Worker<T, W> {
        return newWorker(User(executor, context, action))
    }

    override fun filter(context: CoroutineContexts, action: FilterAction<T>): Worker<T, W> {
        return newWorker(Filter(executor, context, action))
    }

    /**
     * Create a new instance of a Worker, most likely, it is a new instance of itself
     */
    protected abstract fun <R> newWorker(executor: Executor<R>): Worker<R, W>

    override fun onSuccess(action: ConsumeAction<T>): WorkStarter<T, W> {
        require(successAction == null) { "onSuccess() is called twice" }
        successAction = action
        return this
    }

    override fun onError(action: ConsumeAction<Throwable>): WorkStarter<T, W> {
        require(errorAction == null) { "onError() is called twice" }
        errorAction = action
        return this
    }

    override fun setStartDelay(delay: Long): WorkStarter<T, W> {
        startDelay = delay
        return this
    }

    protected fun executeWork(context: CoroutineContext): Job = launch(context) {
        if (startDelay > 0) delay(startDelay)

        try {
            val response = executor.execute(this)
            if (isActive) { // make sure job is not yet cancelled
                successAction?.let { launch(CONTEXT_UI) { it(response) } }
            }
        } catch(exception: Throwable) {
            if (isActive && shouldReportException(exception)) { // make sure job is not yet cancelled
                errorAction?.let { launch(CONTEXT_UI) { it(exception) } }
            }
        }
    }
}



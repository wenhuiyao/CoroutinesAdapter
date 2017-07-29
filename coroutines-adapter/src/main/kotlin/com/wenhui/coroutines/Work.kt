package com.wenhui.coroutines

import com.wenhui.coroutines.functions.ConsumeAction
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext


// Kotlin doesn't support java SAM type conversion, this is to workaround that issue: https://discuss.kotlinlang.org/t/kotlin-and-sam-interface-with-two-parameters/293/18
internal typealias KConsumeAction<T> = (T) -> Unit
internal class KConsumeActionWrapper<T>(private val action: KConsumeAction<T>) : ConsumeAction<T> {
    override fun invoke(item: T) = action(item)
}

internal typealias FilterAction<T> = (T) -> Boolean


/**
 * An object that doing all the work and deliver the result back to UI thread. To start the work, [start] must be called
 */
interface Work<T, S> : Operator<T, S>, WorkStarter<T, S>


interface WorkStarter<T, S> : CompleteNotifier<T, S>, Starter<T, S>


interface Operator<T, S> {
    /**
     * Transform a source from type T to type R in background thread
     */
    fun <U> transform(action: Function1<T, U>): Work<U, S> = transform(CoroutineContexts.BACKGROUND, action)

    /**
     * Transform a source from type T to type R
     * @param context: The context where is transformation will be executed
     */
    fun <U> transform(context: CoroutineContexts, action: Function1<T, U>): Work<U, S>

    /**
     * Consume the item, by default it is running in the background
     */
    fun consume(action: ConsumeAction<T>): Work<T, S> = consume(CoroutineContexts.BACKGROUND, action)

    /**
     *  @param context: The context where the consume action will be executed
     */
    fun consume(context: CoroutineContexts, action: ConsumeAction<T>): Work<T, S>

    /**
     * Kotlin specific version of consume
     */
    fun consume(context: CoroutineContexts = CoroutineContexts.BACKGROUND, action: KConsumeAction<T>): Work<T, S>
            = consume(context, KConsumeActionWrapper(action))

    /**
     * Filter an item in background thread, return `true` is the item is valid, `false` to ignore the item
     */
    fun filter(action: FilterAction<T>): Work<T, S> = filter(CoroutineContexts.BACKGROUND, action)

    /**
     * Filter an item, return `true` is the item is valid, `false` to ignore the item
     * @param context: The context where the filter action will be executed
     */
    fun filter(context: CoroutineContexts, action: FilterAction<T>): Work<T, S>
}


interface CompleteNotifier<T, S> {

    /**
     * Callback when execution succeeded
     */
    fun onSuccess(action: ConsumeAction<T>): WorkStarter<T, S>

    /**
     * [Kotlin version] Callback when execution succeeded
     */
    fun onSuccess(action: KConsumeAction<T>): WorkStarter<T, S> = onSuccess(KConsumeActionWrapper(action))

    /**
     * Callback when there is exception
     */
    fun onError(action: ConsumeAction<Throwable>): WorkStarter<T, S>

    /**
     * [Kotlin version] Callback when there is exception
     */
    fun onError(action: KConsumeAction<Throwable>): WorkStarter<T, S> = onError(KConsumeActionWrapper(action))
}

/**
 * `S` The return type after [start], should normally allow the work to be cancel
 */
interface Starter<T, S> {

    fun setStartDelay(delay: Long): WorkStarter<T, S>

    /**
     * This must be called to start the work
     */
    fun start(): S
}

/**
 * Base worker that doing all the essential works
 */
internal abstract class BaseWork<T, S>(private val action: Action<T>) : Work<T, S> {

    private var successAction: ConsumeAction<T>? = null
    private var errorAction: ConsumeAction<Throwable>? = null
    private var startDelay = 0L

    override fun <U> transform(context: CoroutineContexts, action: Function1<T, U>): Work<U, S> {
        return newWork(Transformer(this.action, context, action))
    }

    override fun consume(context: CoroutineContexts, action: ConsumeAction<T>): Work<T, S> {
        return newWork(User(this.action, context, action))
    }

    override fun filter(context: CoroutineContexts, action: FilterAction<T>): Work<T, S> {
        return newWork(Filter(this.action, context, action))
    }

    /**
     * Create a new instance of a Work, most likely, it is a new instance of itself
     */
    protected abstract fun <R> newWork(action: Action<R>): Work<R, S>

    override fun onSuccess(action: ConsumeAction<T>): WorkStarter<T, S> {
        require(successAction == null) { "onSuccess() is called twice" }
        successAction = action
        return this
    }

    override fun onError(action: ConsumeAction<Throwable>): WorkStarter<T, S> {
        require(errorAction == null) { "onError() is called twice" }
        errorAction = action
        return this
    }

    override fun setStartDelay(delay: Long): WorkStarter<T, S> {
        startDelay = delay
        return this
    }

    protected fun executeWork(context: CoroutineContext): Job = launch(context) {
        if (startDelay > 0) delay(startDelay)

        try {
            val response = action.runAsync(this)
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



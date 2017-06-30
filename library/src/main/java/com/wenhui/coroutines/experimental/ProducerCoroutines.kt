@file:JvmName("Producers")

package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Utility method to create a [Producer], and the producer can be reused to consume items by calling [Producer.produce],
 * producer will be active until [Producer.close] is called, or when using [BackgroundWorkManager], producer will be
 * closed when [BackgroundWorkManager.cancelAllWorks] is called
 */
fun <T, R> consumeBy(action: ConsumerAction<T, R>): ConsumerOperator<T, R> {
    val consumer = ConsumerImpl(action)
    return ProducerConsumer(consumer, consumer)
}

typealias ConsumerAction<T, R> = (T) -> R
typealias FilterAction<T> = (T) -> Boolean


interface Producer<in T> {

    /**
     * Indicates an active state of the producer. The produced item can be consumed only when the state is active
     */
    fun isActive(): Boolean

    /**
     * Produce an item that will be consumed by the consumer
     */
    fun produce(element: T)

    /**
     * Close this producer job, no more item will be accepted, and the state will be inactive at this point
     */
    fun close(reason: Throwable?)
}

interface Completion<T, R> {

    /**
     * Callback when consumer work is done. This is called on UI thread
     */
    fun onSuccess(action: ParametrizedAction<R>): ProducerBuilder<T, R>

    /**
     * Callback when there is an error. This is called on UI thread
     */
    fun onError(action: ParametrizedAction<Throwable>): ProducerBuilder<T, R>
}

interface JobManager<T, R> {

    /**
     * Allow automatically cancel the producer/consumer job when [BackgroundWorkManager.cancelAllWorks] is called
     */
    fun manageBy(manager: BackgroundWorkManager): ProducerBuilder<T, R>
}

interface ProducerBuilder<T, R> : Completion<T, R>, JobManager<T, R> {
    fun build(): Producer<T>
}

interface ConsumerOperator<T, R> : ProducerBuilder<T, R> {
    /**
     * Transform a source from type T to type R in background thread
     */
    fun <M> transform(action: ConsumerAction<R, M>): ConsumerOperator<T, M> = transform(CONTEXT_BG, action)

    /**
     * Transform a source from type T to type R
     *
     * @param context: one of [CONTEXT_BG], [CONTEXT_UI], but not[CONTEXT_NON_CANCELLABLE]
     *
     */
    fun <M> transform(context: CoroutineContext, action: ConsumerAction<R, M>): ConsumerOperator<T, M>

    /**
     * Operate on the item, by default it is running on the background thread
     */
    fun operate(action: ParametrizedAction<R>): ConsumerOperator<T, R> = operate(CONTEXT_BG, action)

    fun operate(context: CoroutineContext, action: ParametrizedAction<R>): ConsumerOperator<T, R>

    /**
     * Filter an item, return `true` is the item is valid, `false` to ignore the item
     * NOTE: the filter action will be running on the same thread as last action whichever that is
     */
    fun filter(action: FilterAction<R>): ConsumerOperator<T, R>
}

private interface Updater<T> {
    fun updateElement(element: T)
}

private interface Consumer<T> {
    suspend fun consume(scope: CoroutineScope): T
}

private class ConsumerImpl<T, R>(private val action: ConsumerAction<T, R>) : Updater<T>, Consumer<R> {

    @Volatile private var element: T? = null

    override fun updateElement(element: T) {
        this.element = element
    }

    suspend override fun consume(scope: CoroutineScope): R {
        scope.ensureActive()
        return action(element!!)
    }
}

private class TransformerImpl<T, R>(private val dependedConsumer: Consumer<T>,
                                    private val transformAction: ConsumerAction<T, R>,
                                    private val context: CoroutineContext) : Consumer<R> {

    suspend override fun consume(scope: CoroutineScope): R {
        val t = dependedConsumer.consume(scope)

        scope.ensureActive()
        return run(context) { transformAction(t) }
    }

}

private class OperatorImpl<R>(private val dependedConsumer: Consumer<R>,
                              private val action: ParametrizedAction<R>,
                              private val context: CoroutineContext) : Consumer<R> {

    suspend override fun consume(scope: CoroutineScope): R {
        val r = dependedConsumer.consume(scope)

        scope.ensureActive()
        run(context) { action(r) }
        return r
    }
}

private class FilterImpl<R>(private val dependedConsumer: Consumer<R>,
                            private val filter: FilterAction<R>) : Consumer<R> {

    suspend override fun consume(scope: CoroutineScope): R {
        val r = dependedConsumer.consume(scope)

        scope.ensureActive()
        if (!filter(r)) throw IgnoreException()
        return r
    }
}

private class ProducerConsumer<T, R>(private val updater: Updater<T>,
                                     private val consumer: Consumer<R>) : ConsumerOperator<T, R> {

    private var successAction: ParametrizedAction<R>? = null
    private var errorAction: ParametrizedAction<Throwable>? = null
    private var manager: BackgroundWorkManager? = null

    override fun <M> transform(context: CoroutineContext, action: ConsumerAction<R, M>): ConsumerOperator<T, M> {
        ensureContextValidExcludeNonCancellable(context)
        val newTransformer = TransformerImpl(consumer, action, context)
        return ProducerConsumer(updater, newTransformer)
    }

    override fun operate(context: CoroutineContext, action: ParametrizedAction<R>): ConsumerOperator<T, R> {
        ensureContextValidExcludeNonCancellable(context)
        val newTransformer = OperatorImpl(consumer, action, context)
        return ProducerConsumer(updater, newTransformer)
    }

    override fun filter(action: FilterAction<R>): ConsumerOperator<T, R> {
        val newTransformer = FilterImpl(consumer, action)
        return ProducerConsumer(updater, newTransformer)
    }

    override fun onSuccess(action: ParametrizedAction<R>): ProducerBuilder<T, R> {
        if (successAction != null) {
            throw IllegalArgumentException("onSuccess() is called more than once")
        }
        successAction = action
        return this
    }

    override fun onError(action: ParametrizedAction<Throwable>): ProducerBuilder<T, R> {
        if (errorAction != null) {
            throw IllegalArgumentException("onError() is called more than once")
        }
        errorAction = action
        return this
    }

    override fun manageBy(manager: BackgroundWorkManager): ProducerBuilder<T, R> {
        this.manager = manager
        return this
    }

    override fun build(): Producer<T> {
        val producer = ProducerImpl<T>()

        val context = manager?.let { CONTEXT_BG + it.jobRef } ?: CONTEXT_BG
        val job = launch(context) {
            var internalJob: Job? = null
            producer.channel.consumeEach {
                if (!isActive) return@consumeEach

                internalJob?.cancel()
                internalJob = consumeElement(it)
            }
        }
        producer.job = job

        return producer
    }

    private suspend fun consumeElement(element: T) = launch(CONTEXT_BG) InternalJob@ {
        try {
            updater.updateElement(element)
            val result = consumer.consume(this)
            if (isActive) {
                run(CONTEXT_UI) {
                    successAction?.invoke(result)
                }
            }
        } catch(e: Throwable) {
            if (isActive) {
                if (e is IgnoreException) return@InternalJob

                run(CONTEXT_UI) {
                    errorAction?.invoke(e)
                }
            }
        }
    }
}

private class ProducerImpl<T> : Producer<T> {
    val channel: Channel<T> = Channel(Channel.UNLIMITED)
    var job: Job? = null

    override fun isActive(): Boolean = job?.isActive ?: false

    override fun produce(element: T) {
        launch(Unconfined) {
            channel.send(element)
        }
    }

    override fun close(reason: Throwable?) {
        channel.close(reason)
        job?.cancel(reason)
    }

}

private fun CoroutineScope.ensureActive() {
    if (!isActive) {
        throw CancellationException()
    }
}

internal class IgnoreException : Exception()
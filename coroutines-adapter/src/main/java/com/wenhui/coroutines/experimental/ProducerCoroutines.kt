@file:JvmName("Producers")

package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Utility method to create a [Producer], and the producer can be reused to execute items by calling [Producer.produce],
 * producer will be active until [Producer.close] is called, or when using [BackgroundWorkManager], producer will be
 * closed when [BackgroundWorkManager.cancelAllWorks] is called.
 *
 * NOTE: the producer will only execute one item at a time, and if an item is received before the previous work
 * completed, previous work will be cancelled and the current item will be consumed immediately
 */
fun <T, R> consumeBy(action: TransformAction<T, R>): Operator<R, Producer<T>> {
    val consumer = ConsumerImpl(action)
    return ProducerConsumer(consumer, consumer)
}

interface Producer<in T> {

    /**
     * Return `true` is producer is active. The produced item can be consumed only when the state is active
     */
    fun isActive(): Boolean

    /**
     * Produce an item that will be consumed by the consumer
     *
     * NOTE: the producer will only execute one item at a time, and if an item is received before the previous work
     * completed, previous work will be cancelled and the current item will be consumed immediately
     */
    fun produce(element: T)

    /**
     * Close this producer job, no more item will be accepted, and the state will be inactive at this point
     */
    fun close(reason: Throwable? = null)

    fun manageBy(manager: BackgroundWorkManager): Producer<T>
}

private interface Consumer<T> {
    fun startConsuming(context: CoroutineContext, execute: suspend CoroutineScope.() -> Job): Producer<T>
}

private class ConsumerImpl<T, R>(private val action: TransformAction<T, R>) : Consumer<T>, BaseExecutor<R>() {

    @Volatile private var element: T? = null

    suspend override fun execute(scope: CoroutineScope): R {
        ensureActive(scope)
        element?.let { return action(it) } ?: throw IgnoreException()
    }

    override fun startConsuming(context: CoroutineContext, execute: suspend CoroutineScope.() -> Job): Producer<T> {
        val producer = ProducerImpl<T>()
        producer.job = launch(context) {
            var internalJob: Job? = null
            producer.channel.consumeEach {
                if (!isActive) return@consumeEach

                // consume the element
                internalJob?.cancel()
                element = it
                internalJob = execute()
            }
        }
        return producer
    }

}

private class ProducerImpl<T> : Producer<T> {
    val channel: Channel<T> = Channel(Channel.UNLIMITED)
    lateinit var job: Job

    override fun isActive(): Boolean = job.isActive

    override fun produce(element: T) {
        launch(Unconfined) {
            channel.send(element)
        }
    }

    override fun close(reason: Throwable?) {
        channel.close(reason)
        job.cancel(reason)
    }

    override fun manageBy(manager: BackgroundWorkManager): Producer<T> {
        manager.manageJob(job)
        return this
    }

}

private class ProducerConsumer<T, R>(private val consumer: Consumer<T>,
                                     executor: Executor<R>) : BaseWorker<R, Producer<T>>(executor) {

    override fun <M> newWorker(executor: Executor<M>): Operator<M, Producer<T>> = ProducerConsumer(consumer, executor)

    override fun start(): Producer<T> {
        return consumer.startConsuming(CONTEXT_BG) {
            executeWork(CONTEXT_BG)
        }
    }
}
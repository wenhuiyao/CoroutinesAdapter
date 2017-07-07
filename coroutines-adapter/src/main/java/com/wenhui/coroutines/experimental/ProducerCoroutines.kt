@file:JvmName("Producers")

package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch


private typealias ConsumeOp<T, R> = Operator<R, Producer<T>>
private const val CONSUMER_POOL_SIZE = 4

/**
 * Utility method to create a [Producer], and the producer can be reused to execute items by calling [Producer.produce],
 * producer will be active until [Producer.close] is called, or when using [BackgroundWorkManager], producer will be
 * closed when [BackgroundWorkManager.cancelAllWorks] is called.
 *
 * NOTE: the producer will only execute one item at a time, and if an item is received before the previous work
 * completed, previous work will be cancelled and the current item will be consumed immediately
 */
fun <T, R> consumeBy(action: TransformAction<T, R>): ConsumeOp<T, R> {
    return createConsumeOp { channel, job ->
        val producer = ProducerJob(channel, job)
        val consumer = ConsumerJob(channel, job, action)
        ProducerConsumer(producer, consumer, consumer)
    }
}

/**
 * Utility method to create a [Producer], whose item will be consumed by a pool of consumers, and the producer can be
 * reused to execute items by calling [Producer.produce], producer will be active until [Producer.close] is called,
 * or when using [BackgroundWorkManager], producer will be closed when [BackgroundWorkManager.cancelAllWorks] is called.
 *
 * NOTE: Since all the operators will be shared among consumers in different thread, make sure the operators are
 * stateless
 */
fun <T, R> consumeByPool(action: TransformAction<T, R>): ConsumeOp<T, R> {
    return createConsumeOp { channel, job ->
        val producer = ProducerJob(channel, job)
        val producerConsumers = ArrayList<ProducerConsumer<T, R>>(CONSUMER_POOL_SIZE)
        repeat(CONSUMER_POOL_SIZE) {
            val consumer = ConsumerJob(channel, job, action)
            producerConsumers.add(ProducerConsumer(producer, consumer, consumer))
        }
        ProducerConsumers(producer, producerConsumers)
    }
}

private inline fun <T, R> createConsumeOp(block: (Channel<T>, Job) -> ConsumeOp<T, R>): ConsumeOp<T, R> {
    val channel = Channel<T>(Channel.UNLIMITED)
    val job = Job() // a job that used to monitor/cancel producer/consumer works
    return block(channel, job)
}

interface Producer<T> : Manageable<Producer<T>> {

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
}

private interface Consumer {

    /**
     * Consume elements, and execute it with _block_ of coroutine code
     */
    fun consume(block: suspend CoroutineScope.() -> Unit)
}

private class ProducerJob<T>(private val channel: SendChannel<T>,
                             private val job: Job) : Producer<T> {

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

private class ConsumerJob<T, R>(private val channel: ReceiveChannel<T>,
                                private val job: Job,
                                private val action: TransformAction<T, R>) : Consumer, BaseExecutor<R>() {

    @Volatile private var element: T? = null

    override fun onExecute(): R {
        element?.let { return action(it) } ?: throw IgnoreException()
    }

    override fun consume(block: suspend CoroutineScope.() -> Unit) {
        launch(CONTEXT_BG + job) {
            channel.consumeEach {
                element = it
                block()
            }
        }
    }
}

/**
 * This is suitable for single consumer
 */
private const val CONSUME_POLICY_LAST = 0

/**
 * This is suitable for multiple consumers
 */
private const val CONSUME_POLICY_EACH = 1


private class ProducerConsumer<T, R>(private val producer: Producer<T>,
                                     private val consumer: Consumer,
                                     executor: Executor<R>) : BaseWorker<R, Producer<T>>(executor) {

    var consumePolicy = CONSUME_POLICY_LAST

    override fun <M> newWorker(executor: Executor<M>): Operator<M, Producer<T>> {
        return ProducerConsumer(producer, consumer, executor)
    }

    override fun start(): Producer<T> {
        when (consumePolicy) {
            CONSUME_POLICY_EACH -> consumeEach()
            CONSUME_POLICY_LAST -> consumeOnlyLast()
            else -> throw IllegalArgumentException("Please use either CONSUME_POLICY_EACH or CONSUME_POLICY_LAST")
        }
        return producer
    }

    private fun consumeOnlyLast() {
        var internalJob: Job? = null
        consumer.consume {
            // consume the element, but we first need to make sure the current job is cancelled to avoid race
            // condition since we only have one worker to run
            internalJob?.cancel()
            internalJob = executeWork(context)
        }
    }

    private fun consumeEach() {
        consumer.consume {
            executeWork(context).join()
        }

    }
}

private class ProducerConsumers<T, R>(private val producer: Producer<T>,
                                      private val consumers: List<ProducerConsumer<T, R>>) : Operator<R, Producer<T>> {

    override fun <M> transform(context: CoroutineContexts, action: TransformAction<R, M>): Operator<M, Producer<T>> {
        return newInstance { transform(context, action) as ProducerConsumer<T, M> }
    }

    override fun consume(context: CoroutineContexts, action: ConsumeAction<R>): Operator<R, Producer<T>> {
        return newInstance { consume(context, action) as ProducerConsumer<T, R> }
    }

    override fun filter(context: CoroutineContexts, action: FilterAction<R>): Operator<R, Producer<T>> {
        return newInstance { filter(context, action) as ProducerConsumer<T, R> }
    }

    private inline fun <M> newInstance(block: ProducerConsumer<T, R>.() -> ProducerConsumer<T, M>): ProducerConsumers<T, M> {
        val list = consumers.map { block(it) }
        return ProducerConsumers(producer, list)
    }

    override fun onSuccess(action: ConsumeAction<R>): Worker<R, Producer<T>> {
        consumers.forEach { it.onSuccess(action) }
        return this
    }

    override fun onError(action: ConsumeAction<Throwable>): Worker<R, Producer<T>> {
        consumers.forEach { it.onError(action) }
        return this
    }

    override fun setStartDelay(delay: Long): Worker<R, Producer<T>> {
        consumers.forEach { it.setStartDelay(delay) }
        return this
    }

    override fun start(): Producer<T> {
        consumers.forEach {
            it.consumePolicy = CONSUME_POLICY_EACH
            it.start()
        }
        return producer
    }
}
@file:JvmName("Producers")

package com.wenhui.coroutines

import com.wenhui.coroutines.functions.ConsumeAction
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch


// Use all the available threads, this doesn't mean other work will be blocked, they will be executed in parallel
private val CONSUMER_POOL_SIZE = THREAD_SIZE

/**
 * Utility method to create a [Producer], and the producer can be reused to execute items by calling [Producer.produce],
 * producer will be active until [Producer.close] is called, or when using [WorkManager], producer will be
 * closed when [WorkManager.cancelAllWorks] is called.
 *
 * NOTE: the producer will only execute one item at a time, and if an item is received before the previous work
 * completed, previous work will be cancelled and the current item will be consumed immediately
 */
fun <T, R> consumeBy(action: Function1<T, R>): FutureWork<R, Producer<T>> {
    val channel = newChannel<T>()
    val parentJob = parentJob()
    val producer = ProducerImpl(channel, parentJob)
    val consumer = ConsumerImpl(channel, parentJob, action)
    return ProducerConsumer(producer, consumer, consumer)
}

/**
 * Utility method to create a [Producer], whose item will be consumed by a pool of consumers, and the producer can be
 * reused to execute items by calling [Producer.produce], producer will be active until [Producer.close] is called,
 * or when using [WorkManager], producer will be closed when [WorkManager.cancelAllWorks] is called.
 *
 * NOTE: Since all the operators will be shared among consumers in different threads, make sure the operators are
 * stateless to avoid race condition
 */
fun <T, R> consumeByPool(action: Function1<T, R>): FutureWork<R, Producer<T>> {
    val channel = newChannel<T>()
    val parentJob = parentJob()
    val producer = ProducerImpl(channel, parentJob)
    val producerConsumers = ArrayList<ProducerConsumer<T, R>>(CONSUMER_POOL_SIZE)
    repeat(CONSUMER_POOL_SIZE) {
        val consumer = ConsumerImpl(channel, parentJob, action)
        producerConsumers.add(ProducerConsumer(producer, consumer, consumer))
    }
    return ProducerConsumers(producerConsumers)
}

private fun <T> newChannel() = Channel<T>(Channel.UNLIMITED)

/**
 * The parent job that used to monitor producer/consumer job
 */
private fun parentJob(): Job = Job()

interface Producer<T> : Manageable<Producer<T>> {

    /**
     * Return `true` is producer is active. The produced item can be consumed only when the state is active
     */
    val isActive: Boolean

    /**
     * Produce an item that will be consumed by the consumer(s), return `true` if element is added successfully,
     * `false` if the producer is closed or has error, and no longer accept elements
     */
    fun produce(element: T): Boolean

    /**
     * Close this producer job, no more item will be accepted, and the state will be inactive at this point
     */
    fun close()
}

private interface Consumer<T> {

    /**
     * Consume each element, and when the buffer is empty, the queue will be blocked until next item is received, and
     * execute it with [block] of coroutine code
     */
    fun consumeEach(block: suspend CoroutineScope.(ReceiveChannel<T>, T) -> Unit): Job
}

private class ProducerImpl<T>(private val channel: SendChannel<T>,
                              private val parentJob: Job) : Producer<T> {

    override val isActive: Boolean get() = parentJob.isActive && !channel.isClosedForSend

    override fun produce(element: T): Boolean {
        if (!parentJob.isActive) {
            if (!channel.isClosedForSend) {
                channel.close()
            }
            return false
        }
        try {
            return channel.offer(element)
        } catch(ignore: Throwable) {
            return false
        }
    }

    override fun close() {
        channel.close()
        parentJob.cancel()
    }

    override fun manageBy(manager: WorkManager): Producer<T> {
        manager.manageJob(parentJob)
        return this
    }

}

private class ConsumerImpl<T, R>(private val channel: ReceiveChannel<T>,
                                 private val parentJob: Job,
                                 private val action: Function1<T, R>) : Consumer<T>, BaseAction<R>() {

    @Volatile private var element: T? = null

    override fun onPerform(): R {
        element?.let { return action(it) } ?: discontinueExecution()
    }

    override fun consumeEach(block: suspend CoroutineScope.(ReceiveChannel<T>, T) -> Unit): Job {
        return launch(CONTEXT_BG + parentJob) {
            for (e in channel) {
                element = e
                block(channel, e)
            }
        }
    }
}

/**
 * This is suitable for single consumer
 */
private const val CONSUME_POLICY_ONLY_LAST = 0

/**
 * This is suitable for multiple consumers
 */
private const val CONSUME_POLICY_EACH = 1


private class ProducerConsumer<T, R>(private val producer: Producer<T>,
                                     private val consumer: Consumer<T>,
                                     action: Action<R>) : BaseFutureWork<R, Producer<T>>(action) {

    var consumePolicy = CONSUME_POLICY_ONLY_LAST

    override fun <U> newWorker(action: Action<U>): FutureWork<U, Producer<T>> {
        return ProducerConsumer(producer, consumer, action)
    }

    override fun start(): Producer<T> {
        when (consumePolicy) {
            CONSUME_POLICY_EACH -> consumeEach()
            CONSUME_POLICY_ONLY_LAST -> consumeOnlyLast()
            else -> throw IllegalArgumentException("Please use either CONSUME_POLICY_EACH or CONSUME_POLICY_ONLY_LAST")
        }
        return producer
    }

    private fun consumeOnlyLast(): Job {
        var internalJob: Job? = null
        return consumer.consumeEach { channel, _ ->
            if (channel.isEmpty) {
                // only consume the last element
                // but we first need to make sure the current job is cancelled to avoid race condition
                internalJob?.cancel()
                // must use the context from the scope, so when the parent job is cancelled, this will be cancelled
                internalJob = executeWork(context)
            }
        }
    }

    private fun consumeEach(): Job {
        return consumer.consumeEach { _, _ ->
            // must use the context from the scope, so when the parent job is cancelled, this will be cancelled
            executeWork(context).join()
        }
    }
}

private class ProducerConsumers<T, R>(private val consumers: List<ProducerConsumer<T, R>>) : FutureWork<R, Producer<T>> {

    override fun <U> transform(context: CoroutineContexts, action: Function1<R, U>): FutureWork<U, Producer<T>> {
        return newInstance { transform(context, action) as ProducerConsumer<T, U> }
    }

    override fun consume(context: CoroutineContexts, action: ConsumeAction<R>): FutureWork<R, Producer<T>> {
        return newInstance { consume(context, action) as ProducerConsumer<T, R> }
    }

    override fun filter(context: CoroutineContexts, action: FilterAction<R>): FutureWork<R, Producer<T>> {
        return newInstance { filter(context, action) as ProducerConsumer<T, R> }
    }

    private inline fun <U> newInstance(block: ProducerConsumer<T, R>.() -> ProducerConsumer<T, U>): ProducerConsumers<T, U> {
        val list = consumers.map { block(it) }
        return ProducerConsumers(list)
    }

    override fun onSuccess(action: ConsumeAction<R>): WorkStarter<R, Producer<T>> {
        consumers.forEach { it.onSuccess(action) }
        return this
    }

    override fun onError(action: ConsumeAction<Throwable>): WorkStarter<R, Producer<T>> {
        consumers.forEach { it.onError(action) }
        return this
    }

    override fun setStartDelay(delay: Long): WorkStarter<R, Producer<T>> {
        consumers.forEach { it.setStartDelay(delay) }
        return this
    }

    override fun start(): Producer<T> {
        val producers = ArrayList<Producer<T>>(CONSUMER_POOL_SIZE)
        consumers.forEach {
            it.consumePolicy = CONSUME_POLICY_EACH
            producers.add(it.start())
        }
        return ensureOneProducerAndReturn(producers)
    }

    /**
     * Make sure all consumers have the same producer, so they can consume the same pool of items
     */
    private fun ensureOneProducerAndReturn(producers: List<Producer<T>>): Producer<T> {
        val p: Producer<T> = producers.first()
        if (producers.any { it !== p }) {
            p.close()
            throw IllegalStateException("Can't start properly, internal producer conflicts")
        }
        return p
    }
}
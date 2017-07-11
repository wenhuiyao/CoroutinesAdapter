package com.wenhui.coroutines

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class ProducerCoroutinesTest {

    @Test
    fun testProducer_producer() {
        val doneSignal = CountDownLatch(1)
        val receivedItem = AtomicReference<String>()
        val itemToTest = 1000

        val producer = consumeBy<Int, String> {
            receivedItem.set("item=" + it)
            doneSignal.countDown()
            it.toString()
        }.start()

        assertThat(receivedItem.get()).isNull()
        val success = producer.produce(itemToTest)
        assertThat(success).isEqualTo(true)

        doneSignal.await(1, TimeUnit.SECONDS)
        Robolectric.flushForegroundThreadScheduler()

        assertThat(receivedItem.get()).isEqualTo("item=$itemToTest")
    }

    @Test
    fun testProducer_activeState() {
        val producer = consumeBy<Int, String> {
            it.toString()
        }.start()

        assertThat(producer.isActive).isEqualTo(true)
        producer.close()
        assertThat(producer.isActive).isEqualTo(false)
    }

    @Test
    fun testProducer_manageByWorkManager() {
        val workManager = WorkManager()
        val producer = consumeBy<Int, String> {
            it.toString()
        }.start()

        producer.manageBy(workManager)

        assertThat(producer.isActive).isEqualTo(true)
        workManager.cancelAllWorks()
        assertThat(producer.isActive).isEqualTo(false)
    }

    @Test
    fun testProducer_producerAfterClose() {
        val doneSignal = CountDownLatch(1)
        val receivedItem = AtomicReference<String>()
        val itemToTest = 1000

        val producer = consumeBy<Int, String> {
            receivedItem.set("item=" + it)
            doneSignal.countDown()
            it.toString()
        }.start()

        assertThat(receivedItem.get()).isNull()
        producer.close()
        val success = producer.produce(itemToTest)
        assertThat(success).isEqualTo(false)

        doneSignal.await(1, TimeUnit.SECONDS)
        Robolectric.flushForegroundThreadScheduler()

        assertThat(receivedItem.get()).isNull()
    }

    @Test
    fun testProducer_producerAfterCancelByWorkManager() {
        val workManager = WorkManager()
        val doneSignal = CountDownLatch(1)
        val receivedItem = AtomicReference<String>()
        val itemToTest = 1000

        val producer = consumeBy<Int, String> {
            receivedItem.set("item=" + it)
            doneSignal.countDown()
            it.toString()
        }.start().manageBy(workManager)

        assertThat(receivedItem.get()).isNull()
        workManager.cancelAllWorks()
        val success = producer.produce(itemToTest)
        assertThat(success).isEqualTo(false)

        doneSignal.await(1, TimeUnit.SECONDS)
        Robolectric.flushForegroundThreadScheduler()

        assertThat(receivedItem.get()).isNull()
    }
}
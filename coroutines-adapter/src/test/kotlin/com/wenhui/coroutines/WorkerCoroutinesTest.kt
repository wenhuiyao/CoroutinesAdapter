package com.wenhui.coroutines

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class WorkerCoroutinesTest {

    @Test
    fun TestCancelWork() {
        val doneSignal = CountDownLatch(1)
        val got = AtomicInteger(0)
        val work = newFutureWork(TestAction(1000)).onSuccess {
            got.set(it)
            doneSignal.countDown()
        }.setStartDelay(200).start()

        assertThat(got.get()).isEqualTo(0)
        work.cancel()

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()

        // Work is cancelled
        assertThat(got.get()).isEqualTo(0)
    }

    @Test
    fun TestWorkStatus_completeSuccess() {
        val doneSignal = CountDownLatch(1)
        val got = AtomicInteger(0)
        val work = newFutureWork(TestAction(1000)).onSuccess {
            got.set(it)
            doneSignal.countDown()
        }.start()

        assertThat(work.isCompleted).isEqualTo(false)
        doneSignal.await(2000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()

        assertThat(work.isCompleted).isEqualTo(true)
    }

    @Test
    fun TestWorkStatus_cancelled() {
        val doneSignal = CountDownLatch(1)
        val got = AtomicInteger(0)
        val work = newFutureWork(TestAction(1000)).onSuccess {
            got.set(it)
            doneSignal.countDown()
        }.setStartDelay(200).start()

        assertThat(work.isCompleted).isEqualTo(false)
        work.cancel()

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()

        assertThat(got.get()).isEqualTo(0)
        assertThat(work.isCompleted).isEqualTo(true)
    }


    private class TestAction<T>(private val t: T) : BaseAction<T>() {
        override fun run(): T {
            Thread.sleep(100)
            return t
        }
    }

}
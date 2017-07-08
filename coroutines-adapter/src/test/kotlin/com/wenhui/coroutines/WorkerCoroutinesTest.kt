package com.wenhui.coroutines

import kotlinx.coroutines.experimental.CoroutineScope
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual.equalTo
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
        val work = newWorker(TestExecutor(1000)).onSuccess {
            got.set(it)
            doneSignal.countDown()
        }.start()

        assertThat(got.get(), equalTo(0))
        work.cancel()

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()

        // Work is cancelled
        assertThat(got.get(), equalTo(0))
    }

    @Test
    fun TestWorkStatus_completeSuccess() {
        val doneSignal = CountDownLatch(1)
        val got = AtomicInteger(0)
        val work = newWorker(TestExecutor(1000)).onSuccess {
            got.set(it)
            doneSignal.countDown()
        }.start()

        assertThat(work.isActive, `is`(true))
        assertThat(work.isCompleted, `is`(false))
        doneSignal.await(2000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()

        assertThat(work.isActive, `is`(false))
        assertThat(work.isCompleted, `is`(true))
    }

    @Test
    fun TestWorkStatus_cancelled() {
        val doneSignal = CountDownLatch(1)
        val got = AtomicInteger(0)
        val work = newWorker(TestExecutor(1000)).onSuccess {
            got.set(it)
            doneSignal.countDown()
        }.start()

        assertThat(work.isActive, `is`(true))
        assertThat(work.isCompleted, `is`(false))
        work.cancel()

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()

        assertThat(got.get(), equalTo(0))
        assertThat(work.isActive, `is`(false))
        assertThat(work.isCompleted, `is`(true))
    }


    private class TestExecutor<T>(private val t: T) : Executor<T> {
        override var cancellable = true

        suspend override fun execute(scope: CoroutineScope): T {
            Thread.sleep(100)
            return t
        }
    }

}
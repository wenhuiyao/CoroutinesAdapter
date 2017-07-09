package com.wenhui.coroutines

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsNull.nullValue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


/**
 * Test operators, and callbacks
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class WorkerTest {

    @Test
    fun testOnSuccess() {
        val got = AtomicReference<Int>()
        val callSuccess = AtomicBoolean(false)
        val callError = AtomicBoolean(false)
        val doneSignal = CountDownLatch(1)
        createBackgroundWork {
            Thread.sleep(200)
            2000
        }.onSuccess {
            got.set(it)
            callSuccess.set(true) // we should never get here
            doneSignal.countDown()
        }.onError {
            callError.set(true)
            doneSignal.countDown()
        }.start()

        // make sure current thread is not blocking
        assertThat(got.get(), nullValue())
        assertThat(callSuccess.get(), `is`(false))
        assertThat(callError.get(), `is`(false))

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get(), equalTo(2000))
        assertThat(callSuccess.get(), `is`(true))
        assertThat(callError.get(), `is`(false))
    }

    @Test
    fun testOnError() {
        val got = AtomicReference<Int>()
        val callSuccess = AtomicBoolean(false)
        val callError = AtomicBoolean(false)
        val doneSignal = CountDownLatch(1)
        createBackgroundWork {
            Thread.sleep(200)
            throw IllegalStateException("Failed")
            2000
        }.onSuccess {
            got.set(it)
            callSuccess.set(true) // we should never get here
            doneSignal.countDown()
        }.onError {
            callError.set(true)
            doneSignal.countDown()
        }.start()

        // make sure current thread is not blocking
        assertThat(got.get(), nullValue())
        assertThat(callSuccess.get(), `is`(false))
        assertThat(callError.get(), `is`(false))

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get(), nullValue())
        assertThat(callSuccess.get(), `is`(false))
        assertThat(callError.get(), `is`(true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOnSuccess_callTwice() {
        createBackgroundWork {
            2000
        }.onSuccess {
        }.onSuccess {
        }.start()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOnError_callTwice() {
        createBackgroundWork {
            2000
        }.onError {
        }.onError {
        }.start()
    }


    @Test
    fun testFilter_false() {
        val got = AtomicReference<Int>()
        val called = AtomicBoolean(false)
        val doneSignal = CountDownLatch(2)
        createBackgroundWork {
            Thread.sleep(200)
            2000
        }.filter {
            doneSignal.countDown()
            false
        }.onSuccess {
            got.set(it)
            called.set(true) // we should never get here
            doneSignal.countDown()
        }.start()

        // make sure current thread is not blocking
        assertThat(got.get(), nullValue())
        assertThat(called.get(), `is`(false))

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get(), nullValue())
        assertThat(called.get(), `is`(false))
    }

    @Test
    fun testFilter_true() {
        val got = AtomicReference<Int>()
        val called = AtomicBoolean(false)
        val doneSignal = CountDownLatch(2)
        createBackgroundWork {
            Thread.sleep(200)
            2000
        }.filter {
            doneSignal.countDown()
            true
        }.onSuccess {
            got.set(it)
            called.set(true) // we should call this
            doneSignal.countDown()
        }.start()

        // make sure current thread is not blocking
        assertThat(got.get(), nullValue())
        assertThat(called.get(), `is`(false))

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get(), equalTo(2000))
        assertThat(called.get(), `is`(true))
    }

    @Test
    fun testConsume() {
        val got = AtomicReference<Int>()
        val called = AtomicBoolean(false)
        val doneSignal = CountDownLatch(2)
        createBackgroundWork {
            Thread.sleep(200)
            2000
        }.consume {
            called.set(true) // we should call this
            doneSignal.countDown()
        }.onSuccess {
            got.set(it)
            doneSignal.countDown()
        }.start()

        // make sure current thread is not blocking
        assertThat(got.get(), nullValue())
        assertThat(called.get(), `is`(false))
        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get(), equalTo(2000))
        assertThat(called.get(), `is`<Boolean>(true))
    }

    @Test
    fun testTransform() {
        val got = AtomicReference<String>()
        val called = AtomicBoolean(false)
        val doneSignal = CountDownLatch(2)
        createBackgroundWork {
            Thread.sleep(200)
            2000
        }.transform {
            doneSignal.countDown()
            called.set(true)
            "got " + it
        }.onSuccess {
            got.set(it)
            doneSignal.countDown()
        }.start()

        // make sure current thread is not blocking
        assertThat(got.get(), nullValue())
        assertThat(called.get(), `is`(false))
        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get(), equalTo<String>("got 2000"))
        assertThat(called.get(), `is`<Boolean>(true))
    }

}
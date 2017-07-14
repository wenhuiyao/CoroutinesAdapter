package com.wenhui.coroutines

import com.wenhui.coroutines.functions.ConsumeAction
import org.assertj.core.api.Java6Assertions.assertThat

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Test operators, and callbacks
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class WorkerTest {

    @Test
    fun testOnSuccess() {
        val got = AtomicInteger()
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
        assertThat(got.get()).isEqualTo(0)
        assertThat(callSuccess.get()).isEqualTo(false)
        assertThat(callError.get()).isEqualTo(false)

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get()).isEqualTo(2000)
        assertThat(callSuccess.get()).isEqualTo(true)
        assertThat(callError.get()).isEqualTo(false)
    }

    @Test
    fun testOnError() {
        val got = AtomicInteger()
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
        assertThat(got.get()).isEqualTo(0)
        assertThat(callSuccess.get()).isEqualTo(false)
        assertThat(callError.get()).isEqualTo(false)

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get()).isEqualTo(0)
        assertThat(callSuccess.get()).isEqualTo(false)
        assertThat(callError.get()).isEqualTo(true)
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

    @Test(expected = IllegalArgumentException::class)
    fun testOnSuccess_callTwice_differentVersions() {
        createBackgroundWork {
            2000
        }.onSuccess {
        }.onSuccess(ConsumeAction{
        }).start()
    }

    @Test
    fun testFilter_false() {
        val got = AtomicInteger(0)
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
        assertThat(got.get()).isEqualTo(0)
        assertThat(called.get()).isEqualTo(false)

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get()).isEqualTo(0)
        assertThat(called.get()).isEqualTo(false)
    }

    @Test
    fun testFilter_true() {
        val got = AtomicInteger(0)
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
        assertThat(got.get()).isEqualTo(0)
        assertThat(called.get()).isEqualTo(false)

        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get()).isEqualTo(2000)
        assertThat(called.get()).isEqualTo(true)
    }

    @Test
    fun testConsume() {
        val got = AtomicInteger()
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
        assertThat(got.get()).isEqualTo(0)
        assertThat(called.get()).isEqualTo(false)
        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get()).isEqualTo(2000)
        assertThat(called.get()).isEqualTo(true)
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
        assertThat(got.get()).isNull()
        assertThat(called.get()).isEqualTo(false)
        doneSignal.await(1000, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        assertThat(got.get()).isEqualTo("got 2000")
        assertThat(called.get()).isEqualTo(true)
    }

}

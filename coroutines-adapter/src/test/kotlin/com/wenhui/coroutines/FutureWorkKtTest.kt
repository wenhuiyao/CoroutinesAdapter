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
class FutureWorkKtTest {

    @Test fun mergeNullable() {
        val got = AtomicReference<String>()
        val doneSignal = CountDownLatch(1)
        merge({
            Thread.sleep(300)
            "consume"
        }, {
            Thread.sleep(600)
            null
        }, {
            Thread.sleep(500)
            "nullable"
        }).transform {
            it.filter { it != null }.joinToString(" ")
        }.onSuccess {
            got.set(it)
            doneSignal.countDown()
        }.start()

        assertThat(got.get()).isNull()

        doneSignal.await(3, TimeUnit.SECONDS)
        Robolectric.flushForegroundThreadScheduler()

        assertThat(got.get()).isEqualTo("consume nullable")
    }

    @Test fun andNullable() {
        val got = AtomicReference<String>()
        val doneSignal = CountDownLatch(1)
        and({
            Thread.sleep(300)
            "consume"
        }, {
            Thread.sleep(600)
            null
        }, {
            Thread.sleep(500)
            1000
        }).merge { str, n, i ->
            "$str $i"
        }.onSuccess {
            got.set(it)
            doneSignal.countDown()
        }.start()

        assertThat(got.get()).isNull()

        doneSignal.await(3, TimeUnit.SECONDS)
        Robolectric.flushForegroundThreadScheduler()

        assertThat(got.get()).isEqualTo("consume 1000")
    }

}
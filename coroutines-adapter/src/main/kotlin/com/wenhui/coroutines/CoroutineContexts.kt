package com.wenhui.coroutines

import android.os.Process
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.CoroutineContext

/**
 * We cap to max 6 threads, this can be changed accordingly
 */
internal val THREAD_SIZE = Math.min(6, 2 * Runtime.getRuntime().availableProcessors())
internal val CONTEXT_BG = newFixedThreadPoolContext(THREAD_SIZE, "background")
internal val CONTEXT_UI = UI

enum class CoroutineContexts(internal val context: CoroutineContext) {
    BACKGROUND(CONTEXT_BG),
    UI(CONTEXT_UI)
}

private fun newFixedThreadPoolContext(nThreads: Int, name: String): CoroutineContext {
    return Executors.newScheduledThreadPool(nThreads, BackgroundThreadFactory(name)).asCoroutineDispatcher()
}

private class BackgroundThreadFactory(private val name: String) : ThreadFactory {

    private val threadNo = AtomicInteger()

    override fun newThread(target: Runnable?): Thread {
        val threadName = "$name-${threadNo.incrementAndGet()}"
        return PoolThread(target, threadName)
    }
}

private class PoolThread(target: Runnable?, name: String) : Thread(target, name) {
    init {
        isDaemon = true
    }

    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        super.run()
    }
}


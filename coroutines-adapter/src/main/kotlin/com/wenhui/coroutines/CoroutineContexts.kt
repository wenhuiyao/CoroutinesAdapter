package com.wenhui.coroutines

import android.os.Process
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.CoroutineContext

// We want at least 2 threads and at most 6 threads in the core pool,
// preferring to have 1 less than the CPU count to avoid saturating
// the CPU with background work
internal val THREAD_SIZE = Math.max(2, Math.min(Runtime.getRuntime().availableProcessors() - 1, 6))

internal val CONTEXT_BG = newFixedThreadPoolContext(THREAD_SIZE, "CoroutinesAdapter-Background")
internal val CONTEXT_UI = UI

enum class CoroutineContexts(internal val context: CoroutineContext) {
    BACKGROUND(CONTEXT_BG),
    UI(CONTEXT_UI)
}

private fun newFixedThreadPoolContext(nThreads: Int, name: String): CoroutineContext {
    // Using ScheduledThreadPool instead of FixThreadPool since default ExecutorCoroutineDispatcherBase requires support delay,
    // if a non ScheduledThreadPool is provided, Kotlin may just create one when it want to delay an execution
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
    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        super.run()
    }
}


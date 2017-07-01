package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlin.coroutines.experimental.CoroutineContext

/**
 * A ThreadPool executor with max 6 threads
 */
@JvmField internal val CONTEXT_BG = newFixedThreadPoolContext(Math.min(6, 2 * Runtime.getRuntime().availableProcessors()), "background")
@JvmField internal val CONTEXT_UI = UI

enum class CoroutineContexts(internal val context: CoroutineContext, internal val cancellable: Boolean) {
    BACKGROUND(CONTEXT_BG, true),
    UI(CONTEXT_UI, true),
    NON_CANCELLABLE(CONTEXT_BG, false)
}

internal fun ensureContextCancellable(context: CoroutineContexts) {
    if (context.cancellable) return

    throw IllegalArgumentException("Please use either ${CoroutineContexts.BACKGROUND.name} or ${CoroutineContexts.UI.name}")
}
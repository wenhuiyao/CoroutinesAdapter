package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlin.coroutines.experimental.CoroutineContext

/**
 * We cap to max 6 threads, this can be changed accordingly
 */
internal val THREAD_SIZE = Math.min(6, 2 * Runtime.getRuntime().availableProcessors())
internal val CONTEXT_BG = newFixedThreadPoolContext(THREAD_SIZE, "background")
internal val CONTEXT_UI = UI

enum class CoroutineContexts(internal val context: CoroutineContext, internal val cancellable: Boolean) {
    BACKGROUND(CONTEXT_BG, true),
    UI(CONTEXT_UI, true),
    // TODO: non cancellable doesn't work when using UI context, will need to experiment more
//    NON_CANCELLABLE(CONTEXT_BG, false)
}
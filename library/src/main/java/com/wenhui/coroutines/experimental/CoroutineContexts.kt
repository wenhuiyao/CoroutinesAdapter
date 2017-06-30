@file:JvmName("CoroutineContexts")
package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.NonCancellable
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlin.coroutines.experimental.CoroutineContext


@JvmField val CONTEXT_BG = newFixedThreadPoolContext(2 * Runtime.getRuntime().availableProcessors(), "context_bg")
@JvmField val CONTEXT_UI = UI

/**
 * Force work not to be cancelled when [Work.cancel] is called
 */
@JvmField val CONTEXT_NON_CANCELLABLE = NonCancellable

internal fun ensureContextValid(context: CoroutineContext) {
    if (context === CONTEXT_BG || context === CONTEXT_UI || context === CONTEXT_NON_CANCELLABLE) {
        return
    }

    throw IllegalArgumentException("Please use ${CONTEXT_BG.javaClass.name} or ${CONTEXT_UI.javaClass.name} or ${CONTEXT_NON_CANCELLABLE.javaClass.name}")
}

internal fun ensureContextValidExcludeNonCancellable(context: CoroutineContext) {
    if (context === CONTEXT_BG || context === CONTEXT_UI) {
        return
    }

    throw IllegalArgumentException("Please use ${CONTEXT_BG.javaClass.name} or ${CONTEXT_UI.javaClass.name}")
}
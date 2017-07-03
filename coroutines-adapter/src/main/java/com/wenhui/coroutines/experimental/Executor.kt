package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CoroutineScope

internal interface Executor<out T> {

    var cancellable: Boolean

    suspend fun execute(scope: CoroutineScope): T
}

/**
 * The basic building block for a background work.
 */
abstract class BaseExecutor<out T> : Executor<T> {
    override var cancellable: Boolean = true
}

internal fun <T> Executor<T>.ensureActive(scope: CoroutineScope) {
    if (cancellable && !scope.isActive) {
        throw CancellationException()
    }
}
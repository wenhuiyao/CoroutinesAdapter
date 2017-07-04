package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CoroutineScope

internal interface Executor<out T> {

    var cancellable: Boolean

    suspend fun execute(scope: CoroutineScope): T
}

/**
 * The basic building block for a background work. The current design can only be used in Kotlin
 */
abstract class BaseExecutor<out T> : Executor<T> {
    override var cancellable: Boolean = true

    suspend final override fun execute(scope: CoroutineScope): T {
        ensureActive(scope)
        return onExecute()
    }

    /**
     * Doing work, and return the result. Throw exception if there is any error.
     */
    suspend abstract fun onExecute(): T
}

internal fun <T> Executor<T>.ensureActive(scope: CoroutineScope) {
    if (cancellable && !scope.isActive) {
        throw CancellationException()
    }
}
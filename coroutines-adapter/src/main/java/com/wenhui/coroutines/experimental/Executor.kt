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


internal abstract class DependableExecutor<T, R>(private val dependedExecutor: Executor<T>,
                                                 context: CoroutineContexts) : Executor<R> {

    override var cancellable: Boolean = true

    init {
        if (!context.cancellable) {
            cancellable = false
            dependedExecutor.cancellable = false
        }
    }

    suspend final override fun execute(scope: CoroutineScope): R {
        val t = dependedExecutor.execute(scope)
        ensureActive(scope)
        return onExecute(t)
    }

    suspend abstract fun onExecute(input: T): R
}

internal fun <T> Executor<T>.ensureActive(scope: CoroutineScope) {
    if (cancellable && !scope.isActive) {
        throw CancellationException()
    }
}
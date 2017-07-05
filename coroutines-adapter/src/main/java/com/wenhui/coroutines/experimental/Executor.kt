package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CoroutineScope
import kotlin.coroutines.experimental.CoroutineContext

internal interface Executor<out T> {
    var cancellable: Boolean

    /**
     * Doing work, and return the result. Throw exception if there is any error.
     */
    suspend fun execute(scope: CoroutineScope): T
}

/**
 * The basic building block for a background work. Can be extended from Java.
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
    @Throws(Exception::class)
    abstract fun onExecute(): T
}

/**
 * The basic building block for a background work. The current design can only be used in Kotlin
 */
internal abstract class BaseSuspendableExecutor<out T> : Executor<T> {
    override var cancellable: Boolean = true

    suspend final override fun execute(scope: CoroutineScope): T {
        ensureActive(scope)
        return onExecute(scope.context)
    }

    /**
     * Doing work, and return the result. Throw exception if there is any error.
     */
    suspend abstract fun onExecute(context: CoroutineContext): T
}

/**
 * Ensure the current coroutine scope is active. It is not active when the work is cancellable and is cancelled.
 */
internal fun <T> Executor<T>.ensureActive(scope: CoroutineScope) {
    if (cancellable && !scope.isActive) {
        throw CancellationException()
    }
}

/**
 * Discontinue current execution by throwing [IgnoreException]
 */
internal fun discontinueExecution(): Nothing {
    throw IgnoreException("Ignore work")
}

internal fun shouldReportException(exception: Throwable): Boolean = exception !is IgnoreException


/**
 * Exception indicates the operation should be ignored
 */
internal class IgnoreException(message: String = "") : Exception(message)
package com.wenhui.coroutines

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CoroutineScope

internal interface Executor<out T> {
    /**
     * Doing work, and return the result. Throw exception if there is any error.
     */
    suspend fun execute(scope: CoroutineScope): T
}

/**
 * The basic building block for a background work. This can be extended from Java.
 */
abstract class BaseExecutor<out T> : Executor<T> {
    suspend final override fun execute(scope: CoroutineScope): T {
        return onExecute()
    }

    /**
     * Doing work, and return the result. Throw exception if there is any error.
     */
    @Throws(Exception::class)
    abstract fun onExecute(): T
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
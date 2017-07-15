package com.wenhui.coroutines

import kotlinx.coroutines.experimental.CoroutineScope

internal interface Action<out T> {
    /**
     * Doing work, and return the result. Throw exception if there is any error.
     */
    suspend fun perform(scope: CoroutineScope): T
}

/**
 * The basic building block for a background work. This can be extended from Java.
 */
abstract class BaseAction<out T> : Action<T> {
    suspend final override fun perform(scope: CoroutineScope): T {
        return onPerform()
    }

    /**
     * Doing work, and return the result. Throw exception if there is any error.
     */
    @Throws(Exception::class) abstract fun onPerform(): T
}

/**
 * Discontinue current execution by throwing [IgnoreException]
 */
internal fun discontinueExecution(): Nothing = throw IgnoreException()

internal fun shouldReportException(exception: Throwable): Boolean = exception !is IgnoreException


/**
 * Exception indicates the operation should be ignored
 */
internal class IgnoreException(message: String = "Ignore the execution") : Exception(message)
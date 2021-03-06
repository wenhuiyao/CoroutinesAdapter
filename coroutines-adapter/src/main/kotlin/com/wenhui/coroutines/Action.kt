package com.wenhui.coroutines

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.runBlocking

internal interface Action<out T> {
    /**
     * Doing work asynchronously, this will be called from kotlin coroutines
     */
    suspend fun runAsync(scope: CoroutineScope): T

    /**
     * Doing work, block current thread until work is done, then return the result.
     * Throw exception if there is any error.
     */
    @Throws(Exception::class) fun run(): T
}

/**
 * The basic building block for a background work. This can be extended from Java.
 *
 * Subclass this when there is not coroutine call in action
 */
abstract class BaseAction<out T> : Action<T> {
    final override suspend fun runAsync(scope: CoroutineScope): T = run()
}

/**
 * Subclass this when there is coroutine call in the action
 */
internal abstract class BaseSuspendableAction<out T> : Action<T> {

    /**
     * run the action interrupt synchronously, and return the result, throw exception if there is error
     *
     * @throws [ExecutionException]
     * @throws [CancellationException]
     */
    final override fun run(): T {
        var result: T? = null
        var exception: Throwable = CancellationException()
        runBlocking { // blocking current thread util have the result
            try {
                result = runAsync(this)
            } catch(e: Throwable) {
                if (e is kotlinx.coroutines.experimental.CancellationException) {
                    exception = CancellationException()
                } else {
                    exception = e
                }
            }
        }

        return result ?: throw exception
    }
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
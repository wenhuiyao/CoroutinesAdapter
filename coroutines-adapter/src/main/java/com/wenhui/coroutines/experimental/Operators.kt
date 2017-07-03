package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.run

internal abstract class BaseOperator<T, R>(private val dependedExecutor: Executor<T>,
                                           context: CoroutineContexts) : Executor<R> {

    override var cancellable: Boolean = true
        set(value) {
            field = value
            dependedExecutor.cancellable = value
        }

    init {
        if (!context.cancellable) {
            cancellable = false
        }
    }

    suspend final override fun execute(scope: CoroutineScope): R {
        val t = dependedExecutor.execute(scope)
        ensureActive(scope)
        return onExecute(t)
    }

    suspend abstract fun onExecute(input: T): R
}

internal class Transformer<T, R>(dependedExecutor: Executor<T>,
                                 private val transformAction: (T) -> R,
                                 private val context: CoroutineContexts) : BaseOperator<T, R>(dependedExecutor, context) {

    suspend override fun onExecute(input: T): R = run(context.context) { transformAction(input) }
}


internal class User<R>(dependedExecutor: Executor<R>,
                       private val action: (R) -> Unit,
                       private val context: CoroutineContexts) : BaseOperator<R, R>(dependedExecutor, context) {

    suspend override fun onExecute(input: R): R {
        run(context.context) { action(input) }
        return input
    }
}

internal class Filter<R>(dependedExecutor: Executor<R>,
                         private val filter: (R) -> Boolean,
                         private val context: CoroutineContexts) : BaseOperator<R, R>(dependedExecutor, context) {

    suspend override fun onExecute(input: R): R {
        val pass = run(context.context) { filter(input) }
        if (!pass) throw IgnoreException()
        return input
    }
}


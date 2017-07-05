package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.run

internal abstract class BaseOperator<T, R>(private val dependedExecutor: Executor<T>,
                                           private val context: CoroutineContexts) : Executor<R> {

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

    suspend override fun execute(scope: CoroutineScope): R {
        val t = dependedExecutor.execute(scope)
        ensureActive(scope)
        return run(context.context) { onExecute(t) }
    }

    protected abstract fun onExecute(input: T): R
}

internal class Transformer<T, R>(dependedExecutor: Executor<T>,
                                 context: CoroutineContexts,
                                 private val transform: TransformAction<T, R>) : BaseOperator<T, R>(dependedExecutor, context) {
    override fun onExecute(input: T): R = transform(input)
}

/**
 * Consume the item T.
 *
 * NOTE: the name Consumer is taken, so User in this case is the Consumer
 */
internal class User<T>(dependedExecutor: Executor<T>,
                       context: CoroutineContexts,
                       private val consume: ConsumeAction<T>) : BaseOperator<T, T>(dependedExecutor, context) {
    override fun onExecute(input: T): T {
        consume(input)
        return input
    }
}

internal class Filter<T>(dependedExecutor: Executor<T>,
                         context: CoroutineContexts,
                         private val filter: FilterAction<T>) : BaseOperator<T, T>(dependedExecutor, context) {
    override fun onExecute(input: T): T {
        if (!filter(input)) discontinueExecution()
        return input
    }
}


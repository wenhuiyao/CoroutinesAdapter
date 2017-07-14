package com.wenhui.coroutines

import com.wenhui.coroutines.functions.ConsumeAction
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.run

internal abstract class BaseOperator<T, R>(private val dependedExecutor: Executor<T>,
                                           private val context: CoroutineContexts) : Executor<R> {

    suspend override fun execute(scope: CoroutineScope): R {
        val t = dependedExecutor.execute(scope)
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
                         private val consumer: ConsumeAction<T>) : BaseOperator<T, T>(dependedExecutor, context) {
    override fun onExecute(input: T): T {
        consumer.invoke(input)
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


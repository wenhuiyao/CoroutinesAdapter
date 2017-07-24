package com.wenhui.coroutines

import com.wenhui.coroutines.functions.ConsumeAction
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.run

internal abstract class BaseOperator<T, R>(private val dependedAction: Action<T>,
                                           private val context: CoroutineContexts) : Action<R> {

    suspend override fun runAsync(scope: CoroutineScope): R {
        val t = dependedAction.runAsync(scope)
        return run(context.context) { onRun(t) }
    }

    override fun run(): R {
        val t = dependedAction.run()
        return onRun(t)
    }

    protected abstract fun onRun(input: T): R
}

internal class Transformer<T, R>(dependedAction: Action<T>,
                                 context: CoroutineContexts,
                                 private val transform: Function1<T, R>) : BaseOperator<T, R>(dependedAction, context) {
    override fun onRun(input: T): R = transform(input)
}

/**
 * Consume the item T.
 *
 * NOTE: the name Consumer is taken, so User in this case is the Consumer
 */
internal class User<T>(dependedAction: Action<T>,
                       context: CoroutineContexts,
                       private val consume: ConsumeAction<T>) : BaseOperator<T, T>(dependedAction, context) {
    override fun onRun(input: T): T {
        consume(input)
        return input
    }
}

internal class Filter<T>(dependedAction: Action<T>,
                         context: CoroutineContexts,
                         private val filter: FilterAction<T>) : BaseOperator<T, T>(dependedAction, context) {
    override fun onRun(input: T): T {
        if (!filter(input)) discontinueExecution()
        return input
    }
}


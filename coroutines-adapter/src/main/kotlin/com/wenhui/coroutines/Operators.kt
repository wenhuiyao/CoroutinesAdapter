package com.wenhui.coroutines

import com.wenhui.coroutines.functions.ConsumeAction
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.run

internal abstract class BaseOperator<T, R>(private val dependedAction: Action<T>,
                                           private val context: CoroutineContexts) : Action<R>() {

    suspend override fun perform(scope: CoroutineScope): R {
        val t = dependedAction.perform(scope)
        return run(context.context) { onPerform(t) }
    }

    protected abstract fun onPerform(input: T): R
}

internal class Transformer<T, R>(dependedAction: Action<T>,
                                 context: CoroutineContexts,
                                 private val transform: Function1<T, R>) : BaseOperator<T, R>(dependedAction, context) {
    override fun onPerform(input: T): R = transform(input)
}

/**
 * Consume the item T.
 *
 * NOTE: the name Consumer is taken, so User in this case is the Consumer
 */
internal class User<T>(dependedAction: Action<T>,
                       context: CoroutineContexts,
                       private val consume: ConsumeAction<T>) : BaseOperator<T, T>(dependedAction, context) {
    override fun onPerform(input: T): T {
        consume(input)
        return input
    }
}

internal class Filter<T>(dependedAction: Action<T>,
                         context: CoroutineContexts,
                         private val filter: FilterAction<T>) : BaseOperator<T, T>(dependedAction, context) {
    override fun onPerform(input: T): T {
        if (!filter(input)) discontinueExecution()
        return input
    }
}


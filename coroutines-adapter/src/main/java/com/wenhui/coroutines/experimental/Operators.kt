package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.run


internal class Transformation<T, R>(dependedExecutor: Executor<T>,
                                    private val transformAction: (T) -> R,
                                    private val context: CoroutineContexts) : DependableExecutor<T, R>(dependedExecutor, context) {

    suspend override fun onExecute(input: T): R = run(context.context) { transformAction(input) }
}


internal class Operation<R>(dependedExecutor: Executor<R>,
                            private val action: (R) -> Unit,
                            private val context: CoroutineContexts) : DependableExecutor<R, R>(dependedExecutor, context) {

    suspend override fun onExecute(input: R): R {
        run(context.context) { action(input) }
        return input
    }
}

internal class Filtration<R>(dependedExecutor: Executor<R>,
                             private val filter: (R) -> Boolean,
                             private val context: CoroutineContexts) : DependableExecutor<R, R>(dependedExecutor, context) {

    suspend override fun onExecute(input: R): R {
        val pass = run(context.context) { filter(input) }
        if (!pass) throw IgnoreException()
        return input
    }
}


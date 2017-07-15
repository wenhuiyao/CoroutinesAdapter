@file:JvmName("Workers")

package com.wenhui.coroutines

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.async


/**
 * Create a new background work with _action_
 */
fun <R> createBackgroundWork(action: Action<R>) = newWorker(ActionWork(action))

/**
 * Create a new background work with at least two actions
 */
fun <R> createBackgroundWorks(action1: Action<R>, action2: Action<R>, vararg actions: Action<R>) = newWorker(MultiActionsWork(listOf(action1, action2, *actions)))
fun <R> createBackgroundWorks(actions: List<Action<R>>) = newWorker(MultiActionsWork(actions))

/**
 * Create a new background work with an action, and an argument that will pass into the action
 */
fun <T, R> createBackgroundWork(arg: T, action: TransformAction<T, R>) = newWorker(TransformActionWork(arg, action))

/**
 * Create a new background work start with the _work_
 */
fun <T> createBackgroundWork(executor: BaseExecutor<T>): Worker<T, Work> = newWorker(executor)

/**
 * Create a new background work with 2 actions, and later 2 results can be merged. The actions are executed in
 * parallel, so be aware of shared variables
 */
fun <T1, T2> mergeBackgroundWorks(action1: Action<T1>, action2: Action<T2>): Merger<T1, T2> = MergeWork(action1, action2)

/**
 * Create a new background work with 3 actions, and later 3 results can be merged. The actions are executed in
 * parallel, so be aware of shared variables
 */
fun <T1, T2, T3> mergeBackgroundWorks(action1: Action<T1>, action2: Action<T2>, action3: Action<T3>): TriMerger<T1, T2, T3> = TriMergeWork(action1, action2, action3)

/**
 * Create a new background work with 4 actions, and later 4 results can be merged. The actions are executed in
 * parallel, so be aware of shared variables
 */
fun <T1, T2, T3, T4> mergeBackgroundWorks(action1: Action<T1>, action2: Action<T2>, action3: Action<T3>, action4: Action<T4>): QuadMerger<T1, T2, T3, T4> = QuadMergeWork(action1, action2, action3, action4)


private typealias MergeAction<T1, T2, R> = (T1, T2) -> R

interface Merger<T1, T2> {
    fun <R> merge(mergeAction: MergeAction<T1, T2, R>): Worker<R, Work>
}

private typealias TriMergeAction<T1, T2, T3, R> = (T1, T2, T3) -> R

interface TriMerger<T1, T2, T3> {
    fun <R> merge(mergeAction: TriMergeAction<T1, T2, T3, R>): Worker<R, Work>
}

private typealias QuadMergeAction<T1, T2, T3, T4, R> = (T1, T2, T3, T4) -> R

interface QuadMerger<T1, T2, T3, T4> {
    fun <R> merge(mergeAction: QuadMergeAction<T1, T2, T3, T4, R>): Worker<R, Work>
}

/**
 * Typical action that start a block of code and return a result
 */
private typealias Action<R> = () -> R

private class ActionWork<out R>(private val action: Action<R>) : Executor<R> {
    suspend override fun execute(scope: CoroutineScope): R = action()
}

private class MultiActionsWork<out R>(private val actions: List<Action<R>>) : Executor<List<R>> {
    suspend override fun execute(scope: CoroutineScope): List<R> {
        return actions.map { async(scope.context) { it() } }.map { it.await() }
    }
}

private class TransformActionWork<T, R>(private val arg: T,
                                        private val action: TransformAction<T, R>) : Executor<R> {
    suspend override fun execute(scope: CoroutineScope): R = action(arg)
}

private class MergeWork<T1, T2>(private val action1: Action<T1>,
                                private val action2: Action<T2>) : Merger<T1, T2> {

    override fun <R> merge(mergeAction: Function2<T1, T2, R>): Worker<R, Work> {
        return newWorker(object : Executor<R> {
            suspend override fun execute(scope: CoroutineScope): R {
                val context = scope.context
                val result1 = async(context) { action1() }
                val result2 = async(context) { action2() }

                return mergeAction(result1.await(), result2.await())
            }
        })
    }
}

private class TriMergeWork<T1, T2, T3>(private val action1: Action<T1>,
                                       private val action2: Action<T2>,
                                       private val action3: Action<T3>) : TriMerger<T1, T2, T3> {

    override fun <R> merge(mergeAction: Function3<T1, T2, T3, R>): Worker<R, Work> {
        return newWorker(object : Executor<R> {
            suspend override fun execute(scope: CoroutineScope): R {
                val context = scope.context
                val result1 = async(context) { action1() }
                val result2 = async(context) { action2() }
                val result3 = async(context) { action3() }

                return mergeAction(result1.await(), result2.await(), result3.await())
            }
        })
    }
}

private class QuadMergeWork<T1, T2, T3, T4>(private val action1: Action<T1>,
                                            private val action2: Action<T2>,
                                            private val action3: Action<T3>,
                                            private val action4: Action<T4>) : QuadMerger<T1, T2, T3, T4> {

    override fun <R> merge(mergeAction: Function4<T1, T2, T3, T4, R>): Worker<R, Work> {
        return newWorker(object : Executor<R> {
            suspend override fun execute(scope: CoroutineScope): R {
                val context = scope.context
                val result1 = async(context) { action1() }
                val result2 = async(context) { action2() }
                val result3 = async(context) { action3() }
                val result4 = async(context) { action4() }

                return mergeAction(result1.await(), result2.await(), result3.await(), result4.await())
            }
        })
    }
}

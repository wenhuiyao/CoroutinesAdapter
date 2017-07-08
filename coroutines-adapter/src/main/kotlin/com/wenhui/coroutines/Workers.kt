@file:JvmName("Workers")

package com.wenhui.coroutines

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.async


/**
 * Create a new background work with _action_
 */
fun <T> createBackgroundWork(action: Action<T>) = newWorker(ActionWork(action))

/**
 * Create a new background work with at least two actions
 */
fun <T> createBackgroundWorks(action: Action<T>, vararg actions: Action<T>) = newWorker(MultiActionsWork(arrayOf(action, *actions)))

/**
 * Create a new background work with an action, and an argument that will pass into the action
 */
fun <T, R> createBackgroundWork(arg: T, action: TransformAction<T, R>) = newWorker(TransformActionWork(arg, action))

/**
 * Create a new background work start with the _work_
 */
fun <T> createBackgroundWork(executor: BaseExecutor<T>): Operator<T, Work> = newWorker(executor)

/**
 * Create a new background work with two actions, and later two results can be merged. The actions are executed in
 * parallel, so be aware of shared variables
 */
fun <T1, T2, R> mergeBackgroundWorks(action1: Action<T1>, action2: Action<T2>): Merger<T1, T2, R> = MergeWork(action1, action2)

/**
 * Create a new background work with three actions, and later three results can be merged
 */
fun <T1, T2, T3, R> mergeBackgroundWorks(action1: Action<T1>, action2: Action<T2>, action3: Action<T3>): TriMerger<T1, T2, T3, R> = TriMergeWork(action1, action2, action3)


interface Merger<T1, T2, R> {
    fun merge(mergeAction: MergeAction<T1, T2, R>): Operator<R, Work>
}

interface TriMerger<T1, T2, T3, R> {
    fun merge(mergeAction: TriMergeAction<T1, T2, T3, R>): Operator<R, Work>
}

/**
 * Typical action that start a block of code and return a result
 */
private typealias Action<T> = () -> T
private typealias MergeAction<T1, T2, R> = (T1, T2) -> R
private typealias TriMergeAction<T1, T2, T3, R> = (T1, T2, T3) -> R


private class ActionWork<out T>(private val action: Action<T>) : BaseExecutor<T>() {
    override fun onExecute(): T = action()
}

private class MultiActionsWork<out T>(private val actions: Array<Action<T>>) : BaseSuspendableExecutor<List<T>>() {
    suspend override fun execute(scope: CoroutineScope): List<T> {
        return actions.map { async(scope.context) { it() } }.map { it.await() }
    }
}

private class TransformActionWork<T, out R>(
        private val arg: T,
        private val action: TransformAction<T, R>) : BaseExecutor<R>() {

    override fun onExecute(): R = action(arg)
}

private class MergeWork<T1, T2, R>(
        private val action1: Action<T1>,
        private val action2: Action<T2>) : Merger<T1, T2, R>, BaseSuspendableExecutor<R>() {

    private lateinit var mergeAction: MergeAction<T1, T2, R>

    suspend override fun execute(scope: CoroutineScope): R {
        val context = scope.context
        val result1 = async(context) { action1() }
        val result2 = async(context) { action2() }

        return mergeAction(result1.await(), result2.await())
    }

    override fun merge(mergeAction: MergeAction<T1, T2, R>): Operator<R, Work> {
        this.mergeAction = mergeAction
        return newWorker(this)
    }
}

private class TriMergeWork<T1, T2, T3, R>(
        private val action1: Action<T1>,
        private val action2: Action<T2>,
        private val action3: Action<T3>) : TriMerger<T1, T2, T3, R>, BaseSuspendableExecutor<R>() {

    private lateinit var mergeAction: TriMergeAction<T1, T2, T3, R>

    suspend override fun execute(scope: CoroutineScope): R {
        val context = scope.context
        val result1 = async(context) { action1() }
        val result2 = async(context) { action2() }
        val result3 = async(context) { action3() }

        return mergeAction(result1.await(), result2.await(), result3.await())
    }

    override fun merge(mergeAction: TriMergeAction<T1, T2, T3, R>): Operator<R, Work> {
        this.mergeAction = mergeAction
        return newWorker(this)
    }
}

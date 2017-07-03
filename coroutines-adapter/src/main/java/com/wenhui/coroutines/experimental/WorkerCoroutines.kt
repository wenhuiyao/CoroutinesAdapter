@file:JvmName("Workers")

package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.async

/**
 * Create a new background work with _action_
 */
fun <T> backgroundWork(action: Action<T>) = createBackgroundWork(ActionWork(action))

/**
 * Create a new background work with an action, and an argument that will pass into the action
 */
fun <T, R> backgroundWork(arg: T, action: TransformAction<T, R>) = createBackgroundWork(Action1Work(arg, action))

/**
 * Create a new background work with two actions, and later two results can be merged
 */
fun <T1, T2, R> mergeBackgroundWork(action1: Action<T1>, action2: Action<T2>): Merger<T1, T2, R> = MergeActionWork(action1, action2)

/**
 * Create a new background work with three actions, and later three results can be merged
 */
fun <T1, T2, T3, R> mergeBackgroundWork(action1: Action<T1>, action2: Action<T2>, action3: Action<T3>): TriMerger<T1, T2, T3, R> = TriMergeActionWork(action1, action2, action3)

/**
 * Create a new background work start with the _work_
 */
fun <T> createBackgroundWork(executor: BaseExecutor<T>): Operator<T, Work> = WorkerImpl(executor)

interface Merger<T1, T2, R> {
    fun merge(action: MergeAction<T1, T2, R>): Operator<R, Work>
}

interface TriMerger<T1, T2, T3, R> {
    fun merge(action: TriMergeAction<T1, T2, T3, R>): Operator<R, Work>
}

/**
 * Typical action that start a block of code and return a result
 */
private typealias Action<T> = () -> T
private typealias MergeAction<T1, T2, R> = (T1, T2) -> R
private typealias TriMergeAction<T1, T2, T3, R> = (T1, T2, T3) -> R


private class ActionWork<out T>(private val action: Action<T>) : BaseExecutor<T>() {

    suspend override fun execute(scope: CoroutineScope): T {
        ensureActive(scope)
        return action()
    }
}

private class Action1Work<T, out R>(
        private val arg: T,
        private val action: TransformAction<T, R>) : BaseExecutor<R>() {

    suspend override fun execute(scope: CoroutineScope): R {
        ensureActive(scope)
        return action(arg)
    }
}

private class MergeActionWork<T1, T2, R>(
        private val action1: Action<T1>,
        private val action2: Action<T2>) : Merger<T1, T2, R>, BaseExecutor<R>() {

    private lateinit var mergeAction: MergeAction<T1, T2, R>

    suspend override fun execute(scope: CoroutineScope): R {
        ensureActive(scope)

        val result1 = async(scope.context) { action1() }
        val result2 = async(scope.context) { action2() }

        return mergeAction(result1.await(), result2.await())
    }

    override fun merge(mergeAction: MergeAction<T1, T2, R>): Operator<R, Work> {
        this.mergeAction = mergeAction
        return createBackgroundWork(this)
    }
}

private class TriMergeActionWork<T1, T2, T3, R>(
        private val action1: Action<T1>,
        private val action2: Action<T2>,
        private val action3: Action<T3>) : TriMerger<T1, T2, T3, R>, BaseExecutor<R>() {

    private lateinit var mergeAction: TriMergeAction<T1, T2, T3, R>

    suspend override fun execute(scope: CoroutineScope): R {
        ensureActive(scope)

        val result1 = async(scope.context) { action1() }
        val result2 = async(scope.context) { action2() }
        val result3 = async(scope.context) { action3() }

        return mergeAction(result1.await(), result2.await(), result3.await())
    }

    override fun merge(mergeAction: TriMergeAction<T1, T2, T3, R>): Operator<R, Work> {
        this.mergeAction = mergeAction
        return createBackgroundWork(this)
    }
}

private class WorkerImpl<T>(work: Executor<T>) : BaseWorker<T, Work>(work) {

    override fun <R> newWorker(executor: Executor<R>): Operator<R, Work> = WorkerImpl(executor)

    override fun start(): Work = newWork(executeWork(CONTEXT_BG))
}




@file:JvmName("FutureWorks")

package com.wenhui.coroutines

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.async


/**
 * Create a single background work
 */
fun <R> from(action: Function0<R>) = newFutureWork(ActionWork(action))

/**
 * Create a single background work, with the [arg] that will be passed into [action]
 */
fun <T, R> from(arg: T, action: Function1<T, R>) = newFutureWork(TransformActionWork(arg, action))

/**
 * Create a single background work from [BaseAction]
 */
fun <T> from(action: BaseAction<T>): FutureWork<T, Worker> = newFutureWork(action)

/**
 * Merge multiple background works of same types into one
 */
fun <R> merge(action1: Function0<R>, action2: Function0<R>, vararg actions: Function0<R>) = newFutureWork(MultiActionsWork(listOf(action1, action2, *actions)))
fun <R> merge(actions: List<Function0<R>>) = newFutureWork(MultiActionsWork(actions))

/**
 * Combine multiple background works into one
 */
fun <T1, T2> and(action1: Function0<T1>, action2: Function0<T2>): Merger<T1, T2> = MergeWork(action1, action2)
fun <T1, T2, T3> and(action1: Function0<T1>, action2: Function0<T2>, action3: Function0<T3>): TriMerger<T1, T2, T3> = TriMergeWork(action1, action2, action3)
fun <T1, T2, T3, T4> and(action1: Function0<T1>, action2: Function0<T2>, action3: Function0<T3>, action4: Function0<T4>): QuadMerger<T1, T2, T3, T4> = QuadMergeWork(action1, action2, action3, action4)


/**
 * A functional interface that accept 2 inputs and output 1 result
 */
interface Merger<T1, T2> {
    fun <R> merge(mergeAction: Function2<T1, T2, R>): FutureWork<R, Worker>
}

/**
 * A functional interface that accept 3 inputs and output 1 result
 */
interface TriMerger<T1, T2, T3> {
    fun <R> merge(mergeAction: Function3<T1, T2, T3, R>): FutureWork<R, Worker>
}

/**
 * A functional interface that accept 4 inputs and output 1 result
 */
interface QuadMerger<T1, T2, T3, T4> {
    fun <R> merge(mergeAction: Function4<T1, T2, T3, T4, R>): FutureWork<R, Worker>
}


private class ActionWork<out R>(private val action: Function0<R>) : Action<R>() {
    suspend override fun perform(scope: CoroutineScope): R = action()
}

private class MultiActionsWork<out R>(private val actions: List<Function0<R>>) : Action<List<R>>() {
    suspend override fun perform(scope: CoroutineScope): List<R> {
        return actions.map { async(scope.context) { it() } }.map { it.await() }
    }
}

private class TransformActionWork<T, R>(private val arg: T,
                                        private val action: Function1<T, R>) : Action<R>() {
    suspend override fun perform(scope: CoroutineScope): R = action(arg)
}

private class MergeWork<T1, T2>(private val action1: Function0<T1>,
                                private val action2: Function0<T2>) : Merger<T1, T2> {

    override fun <R> merge(mergeAction: Function2<T1, T2, R>): FutureWork<R, Worker> {
        return newFutureWork(object : Action<R>() {
            suspend override fun perform(scope: CoroutineScope): R {
                val context = scope.context
                val result1 = async(context) { action1() }
                val result2 = async(context) { action2() }

                return mergeAction(result1.await(), result2.await())
            }
        })
    }
}

private class TriMergeWork<T1, T2, T3>(private val action1: Function0<T1>,
                                       private val action2: Function0<T2>,
                                       private val action3: Function0<T3>) : TriMerger<T1, T2, T3> {

    override fun <R> merge(mergeAction: Function3<T1, T2, T3, R>): FutureWork<R, Worker> {
        return newFutureWork(object : Action<R>() {
            suspend override fun perform(scope: CoroutineScope): R {
                val context = scope.context
                val result1 = async(context) { action1() }
                val result2 = async(context) { action2() }
                val result3 = async(context) { action3() }

                return mergeAction(result1.await(), result2.await(), result3.await())
            }
        })
    }
}

private class QuadMergeWork<T1, T2, T3, T4>(private val action1: Function0<T1>,
                                            private val action2: Function0<T2>,
                                            private val action3: Function0<T3>,
                                            private val action4: Function0<T4>) : QuadMerger<T1, T2, T3, T4> {

    override fun <R> merge(mergeAction: Function4<T1, T2, T3, T4, R>): FutureWork<R, Worker> {
        return newFutureWork(object : Action<R>() {
            suspend override fun perform(scope: CoroutineScope): R {
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

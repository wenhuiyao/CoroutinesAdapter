package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext


internal fun <T> createBackgroundWork(work: BaseWork<T>): Operator<T> = WorkerImpl(work)

private class TransformationWork<T, out R>(
        private val dependedWork: BaseWork<T>,
        private val action: TransformAction<T, R>,
        private val context: CoroutineContext) : BaseWork<R>() {

    override var cancellable = true
        set(value) {
            field = value
            // propagate signal up to depended work, so if current scope is not cancellable, the depended work
            // shouldn't be cancellable either
            dependedWork.cancellable = value
        }

    suspend override fun startWork(scope: CoroutineScope): R {
        val t = dependedWork.startWork(scope)

        ensureWorkActive(scope)
        return run(context) { action(t) }
    }
}

private class OperationWork<out T>(private val dependedWork: BaseWork<T>,
                                   private val action: ParametrizedAction<T>,
                                   private val context: CoroutineContext) : BaseWork<T>() {

    override var cancellable = true
        set(value) {
            field = value
            // propagate signal up to depended work, so if current scope is not cancellable, the depended work
            // shouldn't be cancellable either
            dependedWork.cancellable = value
        }

    suspend override fun startWork(scope: CoroutineScope): T {
        val t = dependedWork.startWork(scope)

        ensureWorkActive(scope)
        run(context) { action(t) }
        return t
    }

}

internal abstract class BaseWorker<T>: Operator<T> {
    protected var successAction: ParametrizedAction<T>? = null
    protected var errorAction: ParametrizedAction<Throwable>? = null
    protected var startDelay = 0L
    protected var manager: BackgroundWorkManager? = null

    override fun onSuccess(action: ParametrizedAction<T>): Worker<T> {
        if (successAction != null) {
            throw IllegalArgumentException("onSuccess() is called twice")
        }
        successAction = action
        return this
    }

    override fun onError(action: ParametrizedAction<Throwable>): Worker<T> {
        if (errorAction != null) {
            throw IllegalArgumentException("onError() is called twice")
        }
        errorAction = action
        return this
    }

    override fun setStartDelay(delay: Long): Worker<T> {
        startDelay = delay
        return this
    }

    override fun manageBy(manager: BackgroundWorkManager): Worker<T> {
        this.manager = manager
        return this
    }

    override fun start(): Work {
        val context = manager?.let { CONTEXT_BG + it.jobRef } ?: CONTEXT_BG
        val job = launch(context) {
            if (startDelay > 0) {
                delay(startDelay)
            }

            onStart(this)
        }

        val work = WorkImpl(job)
        manager?.run {
            this.activeWorks.add(work)
            job.invokeOnCompletion {
                this.activeWorks.remove(work)
            }
        }
        return work
    }

    abstract suspend fun onStart(scope: CoroutineScope)
}

private class WorkerImpl<T>(private val work: BaseWork<T>) : BaseWorker<T>() {

    override fun <R> transform(context: CoroutineContext, action: TransformAction<T, R>): Operator<R> {
        ensureContextValid(context)
        val work = createWork(context) { TransformationWork(work, action, it) }
        return WorkerImpl(work)
    }

    override fun operate(context: CoroutineContext, action: ParametrizedAction<T>): Operator<T> {
        ensureContextValid(context)
        val work = createWork(context) { OperationWork(work, action, it) }
        return WorkerImpl(work)
    }

    private inline fun <R> createWork(context: CoroutineContext, block: (CoroutineContext) -> BaseWork<R>): BaseWork<R> {
        val isNonCancellable = context === CONTEXT_NON_CANCELLABLE
        val newContext = if (isNonCancellable) CONTEXT_BG else context
        val newWork = block(newContext)
        if (isNonCancellable) {
            // mark all the worker before this non cancellable, so all the jobRef before this transformation
            // will proper start, so this can properly transform
            newWork.cancellable = false
        }
        return newWork
    }

    suspend override fun onStart(scope: CoroutineScope) {
        try {
            val response = work.startWork(scope)
            if (scope.isActive) { // make sure jobRef is not yet cancelled
                run(CONTEXT_UI) {
                    successAction?.invoke(response)
                }
            }

        } catch(exception: Throwable) {
            if (scope.isActive) { // make sure jobRef is not yet cancelled
                run(CONTEXT_UI) {
                    errorAction?.invoke(exception)
                }
            }
        }
    }
}

private class WorkImpl(private val job: Job) : Work {
    override val isActive = job.isActive
    override val isCompleted = job.isCompleted

    override fun cancel() = job.cancel()
}
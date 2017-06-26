package com.wenhui.coroutines.experimental

import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext

/*
 *  ## Adapt Kotlin coroutines for Java usage
 */

fun <T> createBackgroundWork(work: Work<T>): Transformer<T> = TransformableWorker(work)

private class TransformationWork<T, out R>(
        private val dependedWork: Work<T>,
        private val action: TransformAction<T, R>,
        private val context: CoroutineContext) : Work<R>() {

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

private class TransformableWorker<T>(private val work: Work<T>) : Transformer<T> {

    private var successAction: CompleteAction<T>? = null
    private var errorAction: CompleteAction<Throwable>? = null
    private var startDelay = 0L

    override fun <R> transform(context: CoroutineContext, action: TransformAction<T, R>): Transformer<R> {
        // don't use noncancellable context, it will block all coroutines
        val isNonCancellable = context === CONTEXT_NON_CANCELLABLE
        val newContext = if (isNonCancellable) CONTEXT_BG else context
        val newWork = TransformationWork(work, action, newContext)
        if (isNonCancellable) {
            // mark all the worker before this non cancellable, so all the job before this transformation
            // will proper start, so this can properly transform
            newWork.cancellable = false
        }
        return TransformableWorker(newWork)
    }

    override fun onSuccess(action: CompleteAction<T>): Worker<T> {
        if (successAction != null) {
            throw IllegalArgumentException("onSuccess() is called twice")
        }
        successAction = action
        return this
    }

    override fun onError(action: CompleteAction<Throwable>): Worker<T> {
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

    override fun start(): Working {
        val job = launch(CONTEXT_BG) {
            if (startDelay > 0) {
                delay(startDelay)
            }

            try {
                val response = work.startWork(this)
                if (isActive) { // make sure job is not yet cancelled
                    run(CONTEXT_UI) {
                        successAction?.invoke(response)
                    }
                }

            } catch(exception: Throwable) {
                if (isActive) { // make sure job is not yet cancelled
                    run(CONTEXT_UI) {
                        errorAction?.invoke(exception)
                    }
                }
            }
        }

        return WorkingImpl(job)
    }
}

private class WorkingImpl(private val job: Job) : Working {

    override var completionHandler: CompletionHandler? = null

    override val isActive = job.isActive
    override val isCompleted = job.isCompleted

    init {
        job.invokeOnCompletion { throwable ->
            completionHandler?.invoke(throwable)
        }
    }

    override fun cancel() = job.cancel()
}
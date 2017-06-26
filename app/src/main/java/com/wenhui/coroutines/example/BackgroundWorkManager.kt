package com.wenhui.ktexampleapp.coroutines

import com.wenhui.coroutines.experimental.Working
import java.util.*

/**
 * Created by wyao on 6/23/17.
 */
class BackgroundWorkManager {

    private val activeWorks = ArrayList<Working>(3)

    fun addActiveWork(work: Working) {
        if(work.isCompleted) return

        work.completionHandler = {
            activeWorks.remove(work)
        }
        activeWorks.add(work)
    }

    fun cancelJobs() {
        for(job in activeWorks){
            job.cancel()
        }
        activeWorks.clear()
    }

    fun hasActiveJobs() = !activeWorks.isEmpty()

}
package com.wenhui.coroutines

import kotlinx.coroutines.experimental.Job
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Test


class WorkManagerTest {

    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        workManager = WorkManager()
    }

    @Test
    fun testHaveActiveWorks_empty() {
        assertThat(workManager.hasActiveWork(), `is`(false))
    }

    @Test
    fun testHaveActiveWorks_1() {
        addJobToManager()

        assertThat(workManager.hasActiveWork(), `is`(true))
    }

    @Test
    fun testHaveActiveWorks_many() {
        addJobToManager()
        addJobToManager()
        assertThat(workManager.hasActiveWork(), `is`(true))
    }

    @Test
    fun testCancelAllWorks() {
        addJobToManager()
        addJobToManager()
        addJobToManager()

        assertThat(workManager.hasActiveWork(), `is`(true))
        workManager.cancelAllWorks()

        assertThat(workManager.hasActiveWork(), `is`(false))
    }

    private fun addJobToManager() {
        val job = Job()
        workManager.manageJob(job)
    }

    fun test() {
        mergeBackgroundWorks( {
            "Merge "
        }, {
            1000
        }).merge { s, i ->
            s.hashCode() + i
        }.consume {  }
    }
}
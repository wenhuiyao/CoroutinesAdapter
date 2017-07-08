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
        assertThat(workManager.hasActiveWorks(), `is`(false))
    }

    @Test
    fun testHaveActiveWorks_1() {
        addJobToManager()

        assertThat(workManager.hasActiveWorks(), `is`(true))
    }

    @Test
    fun testHaveActiveWorks_many() {
        addJobToManager()
        addJobToManager()
        assertThat(workManager.hasActiveWorks(), `is`(true))
    }

    @Test
    fun testCancelAllWorks() {
        addJobToManager()
        addJobToManager()
        addJobToManager()

        assertThat(workManager.hasActiveWorks(), `is`(true))
        workManager.cancelAllWorks()

        assertThat(workManager.hasActiveWorks(), `is`(false))
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
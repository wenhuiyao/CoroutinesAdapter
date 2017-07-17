package com.wenhui.coroutines

import kotlinx.coroutines.experimental.Job
import org.assertj.core.api.Java6Assertions.assertThat

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
        assertThat(workManager.hasActiveWork()).isEqualTo(false)
    }

    @Test
    fun testHaveActiveWorks_1() {
        addJobToManager()

        assertThat(workManager.hasActiveWork()).isEqualTo(true)
    }

    @Test
    fun testHaveActiveWorks_many() {
        addJobToManager()
        addJobToManager()
        assertThat(workManager.hasActiveWork()).isEqualTo(true)
    }

    @Test
    fun testCancelAllWorks() {
        addJobToManager()
        addJobToManager()
        addJobToManager()

        assertThat(workManager.hasActiveWork()).isEqualTo(true)
        workManager.cancelAllWorks()

        assertThat(workManager.hasActiveWork()).isEqualTo(false)
    }

    private fun addJobToManager() {
        val job = Job()
        workManager.manageJob(job)
    }

    fun test() {
        and({
            "Merge "
        }, {
            1000
        }).merge { s, i ->
            s.hashCode() + i
        }.consume { }
    }
}
package com.wenhui.coroutines

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class CoroutinesAdapterTest {

    @Before
    fun setup() {
        resetConfiguration()
    }

    @Test
    fun testConfigurationBuilder() {
        val builder = Configuration.Builder()
        val executor = Executors.newSingleThreadExecutor()
        val configuration = builder.executor(executor).build()
        assertThat(configuration.executor).isSameAs(executor)
    }

    @Test
    fun testSingletonConfiguration_customConfiguration() {
        val executor = Executors.newSingleThreadExecutor()
        val configuration = Configuration.Builder().executor(executor).build()
        configCoroutinesAdapter(configuration)

        assertThat(getSingletonConfig()).isSameAs(configuration)
    }

    @Test(expected = IllegalArgumentException::class)
    fun ensureConfigurationOnlySetOnce() {
        val executor = Executors.newSingleThreadExecutor()
        val configuration = Configuration.Builder().executor(executor).build()
        configCoroutinesAdapter(configuration)
        configCoroutinesAdapter(configuration)
    }

    @Test
    fun testGetDefaultConfiguration() {
        val singletonConfig = getSingletonConfig()
        assertThat(singletonConfig).isNotNull()
        assertThat(singletonConfig.executor).isNotNull()
    }
}
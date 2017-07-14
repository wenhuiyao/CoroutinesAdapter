@file:JvmName("CoroutinesAdapter")

package com.wenhui.coroutines

import java.util.concurrent.Executor

private var configuration: Configuration? = null

/**
 * Configure CoroutinesAdapter.
 *
 * This must be called before the first background work used
 */
fun config(config: Configuration) {
    require(configuration == null) { "Configuration is already set" }
    configuration = config
}

internal fun getSingletonConfig(): Configuration {
    val config = configuration ?: defaultConfiguration()
    configuration = config
    return config
}

private fun defaultConfiguration() = Configuration.Builder().build()

class Configuration private constructor(internal val executor: Executor) {

    class Builder {
        /**
         * Config the executor use to execute background work
         */
        private var executor: Executor? = null

        fun executor(executor: Executor): Builder {
            this.executor = executor
            return this
        }

        fun build(): Configuration {
            return Configuration(executor = executor ?: newDefaultExecutorService())
        }
    }
}


/**
 * For testing
 */
internal fun resetConfiguration() {
    configuration = null
}




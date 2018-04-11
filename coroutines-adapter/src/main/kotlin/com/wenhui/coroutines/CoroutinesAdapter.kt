@file:JvmName("CoroutinesAdapter")

package com.wenhui.coroutines

import java.util.concurrent.Executor

@Volatile private var configuration: Configuration? = null

/**
 * Configure CoroutinesAdapter.
 *
 * This must be called before the first background work used
 */
fun configCoroutinesAdapter(config: Configuration) {
    require(configuration == null) { "Configuration is already set" }
    configuration = config
}

internal fun getSingletonConfig(): Configuration {
    val config = configuration ?: Configuration.Builder().build()
    configuration = config
    return config
}

class Configuration private constructor(internal val executor: Executor) {

    class Builder {

        private var executor: Executor? = null

        /**
         * Config the executor use to execute background work
         */
        fun executor(executor: Executor) = also { it.executor = executor }

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




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

class Configuration {

    /**
     * Config the executor use to execute background work
     */
    lateinit var executor: Executor

}

internal fun getGlobalConfig(): Configuration {
    if (configuration == null) {
        configuration = Configuration().apply { executor = newDefaultExecutorService() }
    }
    return configuration!!
}





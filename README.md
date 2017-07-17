# CoroutinesAdapter

Kotlin coroutines make asynchronous programming simple and easy, however, it has Kotlin specific keyword that can't be interoped with Java.
This library is to adapt Kotlin [coroutines](https://github.com/Kotlin/kotlinx.coroutines) to Android use.

[Introduction to Kotlin coroutines](https://kotlinlang.org/docs/reference/coroutines.html)

[Examples usage of the core library](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)


##### Simple background work

```Java
Worker worker = FutureWorks.from(() -> {
            // Do intensive background work, and return a result
        }).transform(data -> {
            // Optionally, transform the data to different type in background
        }).onSuccess(value -> {
            // Listen to work complete success, this is running on UI thread
        }).onError(e -> {
            // Listen to exception
        }).start(); // Call start to start the work

// The work can be cancelled at anytime
worker.cancel();

```

##### Merge multiple background works

```Java
FutureWorks.and(() -> {
            // Do one background work, and return its result
        }, () -> {
            // Do another background work, and return its result
        }).merge((result1, result2) -> {
            // Merge both works' result into one, and return it
        }).consume(CoroutineContexts.UI, data -> {
            // Optionally, consume the data on UI thread
        }).onSuccess((value) -> {
            // Listen to work complete success, this is running on UI thread
        }).onError(e -> {
            // Listen to exception
        }).start();

```

##### Producer/Consumer

```Java
Producer producer = Producers.consumeBy((String element) -> {
        // do background work when receive element
    }).filter(element -> {
        // filter out element that doesn't meet the condition
    }).consume(result -> {
        // consume the data, e.g. saving it to database
    }).onSuccess(element -> {
         // Listen to work complete success, this is running on UI thread
    }).onError(throwable -> {
        // Listen to exception
    }).start();

    // Now, produce item to be consumed by the above code
    producer.produce(element);

    // When producer is no longer needed
    producer.close();

    // Create a pool of consumers to consume the produced elements
    Producers.consumeByPool()
```

##### Usage

```Groovy

dependencies {
    compile 'com.wenhui:coroutines-adapter:0.8.0'
}

```


##### NOTE:
1. Kotlin coroutines is still an experimental feature
2. All examples are written in Java 8, but can be use in to Java 6, 7
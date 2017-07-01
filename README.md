# CoroutinesAdapter
Adapt Kotlin [coroutines](https://github.com/Kotlin/kotlinx.coroutines) core library for Java usage.

[Introduction to Kotlin coroutines](https://kotlinlang.org/docs/reference/coroutines.html)

[Examples usage of the core library](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)

Unfortunately, Kotlin coroutines can only be used directly by Kotlin, so this library is to adapt Kotlin to use by Java.

NOTE:
1. Kotlin coroutines is still an experimental feature
2. All example is written using Java 8 lambda expression, but can be use back to Java 6
3. The library doesn't cover all the use case of coroutines

##### Simple background work usage

```Java
Work work = background(() -> {
        return doSomeBackgroundWork();
    }).transform(CONTEXT_BG, (value) -> {
        // Optionally, transform value to a new value
        return newValue;
    }).onSuccess((value) -> {
        // Do work when background work done. This is running on UI thread
        return Unit.INSTANCE;
    }).onError(throwable -> {
        // Do work when background work has exeception. This is running on UI thread
        return Unit.INSTANCE;
    }).setStartDelay(2000).start();

// When activity/fragment is destroyed, the work can be canceled to avoid memory leak
work.cancel();

```

##### Use Producer

```Java
Producer producer = Producers.consumeBy((Integer element) -> {
        // do some work in the background
        int result = element % 5;
        return result;
    }).filter(element -> {
        return element % 2 == 0; // only interesting in any even numbers
    }).operate(result -> {
        // consume the data, e.g. saving it to database
        return Unit.INSTANCE; // must return this
    }).transform(CoroutineContexts.UI, element ->  {
        // optionally, data can be consumed on UI thread
        return "Consume " + element;
    }).onSuccess(element -> {
        // success callback
        return Unit.INSTANCE;
    }).onError(throwable -> {
        // error callback
        return Unit.INSTANCE;
    }).build();

    // Now, produce item to be consumed by the above code
    producer.produce(element);

    // When producer is no longer needed
    producer.close();
```

##### Specific Context when doing operation
```Java
1. CONTEXT_BG -> Doing working on background
2. CONTEXT_UI -> Doing working on UI thread
3. CONTEXT_NON_CANCELLABLE -> Indicate the work can't be cancelled
```

##### Usage
```Groovy

dependencies {
    compile 'com.wenhui:coroutines-adapter:0.2.2'
}

```
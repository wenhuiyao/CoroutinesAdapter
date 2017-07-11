# CoroutinesAdapter

Kotlin coroutines make asynchronous programming simple and easy, however, it has Kotlin specific keyword that can't be interoped with Java.
This library is to adapt Kotlin [coroutines](https://github.com/Kotlin/kotlinx.coroutines) for use in Android.

[Introduction to Kotlin coroutines](https://kotlinlang.org/docs/reference/coroutines.html)

[Examples usage of the core library](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)


##### Simple background work

```Java
Work work = Workers.createBackgroundWork(() -> {
        // Do some in the background
        return doSomeBackgroundWork();
    }).transform(CoroutineContexts.BACKGROUND, (value) -> {
        // Optionally, transform value to a new value in background
        return newValue;
    }).onSuccess((value) -> {
        // Callback when work is completed successfully. This is happending on UI thread
        return Unit.INSTANCE;
    }).onError(throwable -> {
        // Callback when work has error. This is happening on UI thread
        return Unit.INSTANCE;
    }).setStartDelay(2000).start();

// When activity/fragment is destroyed, the work can be canceled to avoid memory leak
work.cancel();

```

##### Merge multiple background works

```Java
Work work = Workers.mergeBackgroundWorks(() -> {
        // Do some work in background
    }, () -> {
        // Do another work in background in parallel with the previous work
    }).merge((int1, int2) -> {
        // This block will be executed when both works are completed
        return int1 + int2;
    }).onSuccess((value) -> {
        // Callback when work is completed successfully. This is happending on UI thread
        mTextView.setText(value);
        return Unit.INSTANCE;
    }).onError(e -> {
        // Callback when work is completed successfully. This is happending on UI thread
        Toast.makeText(this, "error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        return Unit.INSTANCE;
    }).start();

```

##### Producer/Consumer

```Java
Producer producer = Producers.consumeBy((Integer element) -> {
        // do some work in the background
        int result = element % 5;
        return result;
    }).filter(element -> {
        return element % 2 == 0; // only interesting in any even numbers
    }).consume(result -> {
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
    compile 'com.wenhui:coroutines-adapter:0.6.5'
}

```


##### NOTE:
1. Kotlin coroutines is still an experimental feature
2. All examples are written in Java 8, but can be use in to Java 6, 7
3. The library doesn't cover all the use cases of coroutines
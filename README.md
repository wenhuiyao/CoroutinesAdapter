# CoroutinesAdapter
Adapt Kotlin [coroutines](https://github.com/Kotlin/kotlinx.coroutines) core library for Java usage.

[Introduction to Kotlin coroutines](https://kotlinlang.org/docs/reference/coroutines.html)

[Examples usage of the core library](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)

Unfortunately, Kotlin coroutines core library can only be used directly by Kotlin, so this library is to adapt Kotlin to use by Java.

NOTE:
1. Kotlin coroutines is still an experimental feature
2. All example is written using Java 8 lambda expression, but can be use back to Java 6
3. The library doesn't cover all the use case of coroutines

##### Simple background work usage

```Java
Work work = Workers.backgroundWork(() -> {
        return doSomeBackgroundWork();
    }).transform(CoroutineContexts.BACKGROUND, (value) -> {
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

##### Merge multiple background works

```Java
Work work = Workers.mergeBackgroundWork(() -> {
        // simulate intensive work
    }, () -> {
        // simulate another intensive work
    }).merge((int1, int2) -> {
        // This block will be executed when both works are completed
        return int1 + int2;
    }).onSuccess((value) -> {
        // This is running on UI thread
        mTextView.setText(value);
        return Unit.INSTANCE;
    }).onError(e -> {
        Log.d(TAG, "onError");
        Toast.makeText(this, "error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        return Unit.INSTANCE;
    }).start();

```

##### Use Producer

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
```

##### Usage
```Groovy

dependencies {
    compile 'com.wenhui:coroutines-adapter:0.3.1'
}

```
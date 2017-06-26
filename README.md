# CoroutinesAdapter
Adapter Kotlin coroutines for Android usage


### Example

NOTE: all example is written using Java 8 lambda expression

##### Simple usage
```Java
    newBackgroundWork(() -> {
            Log.d(TAG, "enter with background thread: " + getCurrentThreadName());
            return doLengthlyWork();
        }).onSuccess((value) -> {
            Log.d(TAG, "onSuccess");
            // Do work when background work done. This is running on UI thread
            return Unit.INSTANCE;
        }).onError(throwable -> {
            Log.d(TAG, "onError");
            // Do work when background work has exeception. This is running on UI thread
            return Unit.INSTANCE;
        }).setStartDelay(2000).start();
```

##### Transform response to different value

```Java
    newBackgroundWork(() -> {
            Log.d(TAG, "enter with background thread: " + getCurrentThreadName());
            return doLengthlyWork();
        }).transform(CONTEXT_BG, (value) -> {
            Log.d(TAG, "enter background transformation: " + getCurrentThreadName());
            // Transform value to a new value
            return newValue;
        }).onSuccess((value) -> {
            Log.d(TAG, "onSuccess");
            // Do work when background work done. This is running on UI thread
            return Unit.INSTANCE;
        }).onError(throwable -> {
            Log.d(TAG, "onError");
            // Do work when background work has exeception. This is running on UI thread
            return Unit.INSTANCE;
        }).setStartDelay(2000).start();
```

##### Specific Context when doing transformation
```Java
1. CONTEXT_BG -> Doing working in background
2. CONTEXT_UI -> Doing working in UI thread
3. CONTEXT_NON_CANCELLABLE -> Indicate the work can't be cancelled
```
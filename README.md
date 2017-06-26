# CoroutinesAdapter
Adapter Kotlin coroutines for Java usage


### Example

```Java
    newBackgroundWork(() -> {
            Log.d(TAG, "enter with background thread: " + getCurrentThreadName());
            return doLengthlyWork();
        }).transform((value) -> {
            Log.d(TAG, "enter background transformation: " + getCurrentThreadName());
            // Transform value to a new value
            return newValue;
        }).onSuccess((value) -> {
            Log.d(TAG, "onSuccess");
            // Do work when background work done. This is running on UI thread
            return Unit.INSTANCE;
        }).onError(e -> {
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
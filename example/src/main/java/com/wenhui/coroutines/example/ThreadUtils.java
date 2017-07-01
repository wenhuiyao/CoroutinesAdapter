package com.wenhui.coroutines.example;

import com.wenhui.coroutines.experimental.CoroutineContexts;
import com.wenhui.coroutines.experimental.Producer;
import com.wenhui.coroutines.experimental.Producers;
import kotlin.Unit;

/**
 * Created by wyao on 6/30/17.
 */

public class ThreadUtils {

    public static void sleep(long ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignore) {
        }
    }

    public static String getCurrentThreadName(){
        return Thread.currentThread().getName();
    }

    private void produce() {
        Producer producer = Producers.consumeBy((Integer element) -> {
            // do intensive work in the background
            ThreadUtils.sleep(1000);
            int result = element % 5;
            return result; // result
        }).filter(element -> {
            return element % 2 == 0; // only interesting in any even numbers
        }).operate(result -> {
            // consume the data, like saving it to database
            ThreadUtils.sleep(200);
            return Unit.INSTANCE; // must return this
        }).transform(CoroutineContexts.UI, element ->  {
            // optionally, data can be consumed on UI thread
            return "Consume " + element;
        }).onSuccess(element -> {
            // callback on UI thread
            return Unit.INSTANCE;
        }).onError(throwable -> {
            // error callback
            return Unit.INSTANCE;
        }).build();

//        producer.produce(element);
        producer.close(null);
    }

}

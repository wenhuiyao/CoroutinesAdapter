package com.wenhui.coroutines.example;

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

}

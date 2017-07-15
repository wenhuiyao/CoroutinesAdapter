package com.wenhui.coroutines.example;


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



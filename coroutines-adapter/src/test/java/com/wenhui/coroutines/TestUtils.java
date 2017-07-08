package com.wenhui.coroutines;



public class TestUtils {

    public static void sleep(long ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

}

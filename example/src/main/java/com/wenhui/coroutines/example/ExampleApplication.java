package com.wenhui.coroutines.example;

import android.app.Application;
import android.support.annotation.NonNull;
import com.squareup.leakcanary.LeakCanary;
import com.wenhui.coroutines.Configuration;
import com.wenhui.coroutines.CoroutinesAdapter;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        configureCoroutinesAdapter();
    }

    private void configureCoroutinesAdapter() {
        Configuration config = new Configuration();
        config.setExecutor(Executors.newScheduledThreadPool(3, new ThreadFactory()));

        CoroutinesAdapter.config(config);
    }

    private class ThreadFactory implements java.util.concurrent.ThreadFactory {

        private AtomicInteger threadNo = new AtomicInteger();

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "Example-Thread-" + threadNo.incrementAndGet());
        }
    }
}

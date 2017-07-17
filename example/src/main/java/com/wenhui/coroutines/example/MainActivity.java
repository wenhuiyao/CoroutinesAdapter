package com.wenhui.coroutines.example;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.wenhui.coroutines.CoroutineContexts;
import com.wenhui.coroutines.Producer;
import com.wenhui.coroutines.Producers;
import com.wenhui.coroutines.WorkManager;
import com.wenhui.coroutines.BackgroundWorks;
import com.wenhui.coroutines.example.books.GoogleBooks;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * Examples showcase kotlin coroutines adapter
 */

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";
    private WorkManager mWorkManager = new WorkManager();
    private TextView mTextView;
    private Producer<Integer> producer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.textView);

        View button = findViewById(R.id.simpleBackgroundWorkButton);
        button.setOnClickListener((v) -> onSimpleBackgroundWorkClick());

        View button1 = findViewById(R.id.producerWorkButton);
        button1.setOnClickListener(v -> {
            onProducerButtonClick();
        });

        View button2 = findViewById(R.id.simpleMergeWorkButton);
        button2.setOnClickListener(v -> {
            onSimpleMergeWorkButtonClick();
        });

        View retrofitButton = findViewById(R.id.retrofitButton);
        retrofitButton.setOnClickListener(v -> {
            doRetrofitWork();
        });

        View cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            mTextView.setText("Cancel current work");
            cancelCurrentWork();
        });

    }

    /**
     * Showcase simple background work flow
     */
    private void onSimpleBackgroundWorkClick() {
        cancelCurrentWork();

        mTextView.setText("start simple background work");
        BackgroundWorks.createBackgroundWork(() -> {
            // simulate intensive work
            final int sleep = 2000;
            ThreadUtils.sleep(sleep);
            return sleep;
        }).transform(CoroutineContexts.BACKGROUND, data -> {
            // Optionally, transform the data to different data
            // and this is running in background
            ThreadUtils.sleep(1000);
            return "Sleep " + data + " ms";
        }).onSuccess((value) -> {
            // This is running on UI thread
            mTextView.setText(value);
        }).onError(e -> {
            Log.d(TAG, "onError");
            Toast.makeText(this, "error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }).setStartDelay(1000).start().manageBy(mWorkManager);

    }

    /**
     * Showcase merge multiple background works
     */
    private void onSimpleMergeWorkButtonClick() {
        cancelCurrentWork();

        final String tag = "Merge";
        mTextView.setText("start merge work");
        BackgroundWorks.mergeBackgroundWorks(() -> {
            // simulate intensive work
            final int sleep = 2000;
            logThreadMessage(tag, "Start merge work 1");
            ThreadUtils.sleep(sleep);
            logThreadMessage(tag, "End merge work 1");
            return sleep;
        }, () -> {
            // simulate another intensive work
            final int sleep = 1000;
            logThreadMessage(tag, "Start merge work 2");
            ThreadUtils.sleep(sleep);
            logThreadMessage(tag, "End merge work 2");
            return sleep;
        }).merge((int1, int2) -> {
            logThreadMessage(tag, "Merge 2 works");
            // This block will be executed when both works are completed
            return int1 + int2;
        }).transform(CoroutineContexts.BACKGROUND, data -> {
            // Optionally, transform the data to different data
            // and this is running in background
            logThreadMessage(tag, "Transform work");
            ThreadUtils.sleep(2000);
            return "Total sleep " + data + " ms";
        }).consume(CoroutineContexts.UI, data -> {
            logThreadMessage(tag, "Consume work");
            Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
        }).onSuccess((value) -> {
            // This is running on UI thread
            logThreadMessage(tag, "on work success: " + value);
            mTextView.setText(value);
        }).onError(e -> {
            logThreadMessage(tag, "On work error");
            Toast.makeText(this, "error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }).start().manageBy(mWorkManager);
    }

    private void onProducerButtonClick() {
        mTextView.setText("Start producer work");

        if (producer == null || !producer.isActive()) {
            producer = producer();
        }
        // Produce element that will be consumed by the above defined consumer
        for (int i = 0; i < 20; i++) {
            producer.produce(i);
        }
    }

    /**
     * Showcase producer work flow
     */
    private Producer<Integer> producer() {
        final String tag = "Producer";
        // You can use Producers.consumeBy() to consume only the last element sent
        return Producers.consumeByPool((Integer element) -> {
            // do intensive work in the background
            logThreadMessage(tag, "Start consumer work (" + element + ")");
            ThreadUtils.sleep(2000);
            logThreadMessage(tag, "End consumer work (" + element + ")");
            return element % 10; // result
        }).filter(CoroutineContexts.UI, element -> {
            boolean pass = element % 2 == 0; // only interesting in any even numbers
            if (!pass) {
                // This is running on UI thread, so we can show toast message
                Toast.makeText(this, "Filter element: " + element, Toast.LENGTH_SHORT).show();
                logThreadMessage(tag, "Filter element " + element);
            }
            return pass;
        }).consume(result -> {
            logThreadMessage(tag, "Consume element " + result);
            // optionally, consume the data, e.g. save it to database
            ThreadUtils.sleep(300);
        }).transform(element -> {
            logThreadMessage(tag, "Transform element (" + element + ")");
            return "Consume " + element;
        }).onSuccess(element -> {
            // callback on UI thread
            logThreadMessage(tag, "On success: " + element);
            mTextView.setText(element);
        }).onError(exception -> {
            // error callback on UI thread
            logThreadMessage(tag, "On error: " + exception.getMessage());
        }).start().manageBy(mWorkManager);
    }

    private void cancelCurrentWork() {
        if (mWorkManager.hasActiveWork()) {
            mWorkManager.cancelAllWorks();
        }
    }

    /**
     * Showcase custom background work
     */
    private void doRetrofitWork() {
        mTextView.setText("Start requesting google books");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com")
                .addConverterFactory(MoshiConverterFactory.create())
                .build();

        final RetrofitService retrofitService = retrofit.create(RetrofitService.class);
        final Call<GoogleBooks> call = retrofitService.getGoogleBook("isbn:0747532699");

        // use the custom background work we create
        BackgroundWorks.createBackgroundWork(new RetrofitWork<>(call)).transform(books -> {
            return books.getItems().get(0).getVolumeInfo();
        }).onSuccess(item -> {
            mTextView.setText(item.getTitle());
        }).onError(error -> {
            error.printStackTrace();
            Toast.makeText(this, "request error", Toast.LENGTH_SHORT).show();
        }).start().manageBy(mWorkManager);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWorkManager.cancelAllWorks();
    }

    private static void logThreadMessage(String tag, String message){
        Log.d(TAG, "[" + ThreadUtils.getCurrentThreadName()+"]: " + "[" + tag + "] " + message);
    }
}

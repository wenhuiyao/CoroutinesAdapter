package com.wenhui.coroutines.example;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.wenhui.coroutines.example.books.GoogleBooks;
import com.wenhui.coroutines.experimental.BackgroundWorkManager;
import com.wenhui.coroutines.experimental.CoroutineContexts;
import com.wenhui.coroutines.experimental.Producer;
import com.wenhui.coroutines.experimental.Producers;
import com.wenhui.coroutines.experimental.Workers;
import kotlin.Unit;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import java.util.Random;

/**
 * Examples showcase kotlin coroutines adapter
 */

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";
    private BackgroundWorkManager mWorkManager = new BackgroundWorkManager();
    private Random mRandom = new Random(System.currentTimeMillis());
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
        Workers.createBackgroundWork(() -> {
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
            return Unit.INSTANCE;
        }).onError(e -> {
            Log.d(TAG, "onError");
            Toast.makeText(this, "error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return Unit.INSTANCE;
        }).setStartDelay(1000).start().manageBy(mWorkManager);

    }

    /**
     * Showcase merge multiple background works
     */
    private void onSimpleMergeWorkButtonClick() {
        cancelCurrentWork();

        mTextView.setText("start merge work");
        Workers.mergeBackgroundWork(() -> {
            // simulate intensive work
            final int sleep = 2000;
            ThreadUtils.sleep(sleep);
            return sleep;
        }, () -> {
            // simulate another intensive work
            final int sleep = 1000;
            ThreadUtils.sleep(sleep);
            return sleep;
        }).merge((int1, int2) -> {
            // This block will be executed when both works are completed
            return int1 + int2;
        }).transform(CoroutineContexts.BACKGROUND, data -> {
            // Optionally, transform the data to different data
            // and this is running in background
            ThreadUtils.sleep(2000);
            return "Total sleep " + data + " ms";
        }).consume(CoroutineContexts.UI, data -> {
            Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
            return Unit.INSTANCE;
        }).onSuccess((value) -> {
            // This is running on UI thread
            mTextView.setText(value);
            return Unit.INSTANCE;
        }).onError(e -> {
            Log.d(TAG, "onError");
            Toast.makeText(this, "error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return Unit.INSTANCE;
        }).start().manageBy(mWorkManager);
    }

    private void onProducerButtonClick() {
        cancelCurrentWork();
        mTextView.setText("Start producer work");
        int randomInt = mRandom.nextInt(100);
        if (producer == null || !producer.isActive()) {
            producer = producer();
        }
        // Produce element that will be consumed by the above defined consumer
        producer.produce(randomInt);
    }

    /**
     * Showcase producer work flow
     */
    private Producer<Integer> producer() {
        return Producers.consumeBy((Integer element) -> {
            // do intensive work in the background
            ThreadUtils.sleep(1000);
            return element % 5; // result
        }).filter(CoroutineContexts.UI, element -> {
            boolean pass = element % 2 == 0; // only interesting in any even numbers
            if (!pass) {
                // This is running on UI thread, so we can show toast message
                Toast.makeText(this, "Filter element: " + element, Toast.LENGTH_SHORT).show();
            }
            return pass;
        }).consume(result -> {
            // optionally, consume the data, e.g. save it to database
            ThreadUtils.sleep(200);
            return Unit.INSTANCE; // must return this
        }).transform(CoroutineContexts.UI, element -> {
            // optionally, data can be transformed on UI thread
            return "Consume " + element;
        }).onSuccess(element -> {
            // callback on UI thread
            mTextView.setText(element);
            return Unit.INSTANCE;
        }).onError(exception -> {
            // error callback on UI thread
            Log.d(TAG, "onError: " + exception.getMessage());
            return Unit.INSTANCE;
        }).start().manageBy(mWorkManager);
    }

    private void cancelCurrentWork() {
        if (mWorkManager.hasActiveWorks()) {
            mWorkManager.cancelAllWorks();
        }
    }

    /**
     * Showcase custom background work
     */
    private void doRetrofitWork(){
        mTextView.setText("Start requesting google books");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com")
                .addConverterFactory(MoshiConverterFactory.create())
                .build();

        final RetrofitService retrofitService = retrofit.create(RetrofitService.class);
        final Call<GoogleBooks> call = retrofitService.getGoogleBook("isbn:0747532699");

        // use the custom background work we create
        Workers.createBackgroundWork(new RetrofitWork<>(call)).transform(books ->  {
            return books.getItems().get(0).getVolumeInfo();
        }).onSuccess(item -> {
            mTextView.setText(item.getTitle());
            return Unit.INSTANCE;
        }).onError(error -> {
            error.printStackTrace();
            Toast.makeText(this, "request error", Toast.LENGTH_SHORT).show();
            return Unit.INSTANCE;
        }).start().manageBy(mWorkManager);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWorkManager.cancelAllWorks();
    }
}

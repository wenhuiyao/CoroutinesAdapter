package com.wenhui.coroutines.example;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.wenhui.coroutines.experimental.BackgroundWorkManager;
import com.wenhui.coroutines.experimental.CoroutineContexts;
import com.wenhui.coroutines.experimental.Coroutines;
import com.wenhui.coroutines.experimental.Producer;
import com.wenhui.coroutines.experimental.Producers;
import kotlin.Unit;

import java.util.Random;

/**
 * Created by wyao on 6/24/17.
 */

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";
    private BackgroundWorkManager mWorkManager = new BackgroundWorkManager();
    private Random mRandom = new Random(System.currentTimeMillis());
    private TextView mTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        View button = findViewById(R.id.simpleBackgroundWorkButton);
        button.setOnClickListener((v) -> onClick());

        View button1 = findViewById(R.id.producerWorkButton);
        Producer<Integer> producer = producer();
        button1.setOnClickListener(v -> {
            int randomInt = mRandom.nextInt(100);
            producer.produce(randomInt);
        });

        mTextView = (TextView) findViewById(R.id.textView);
    }

    private void onClick() {
        if (mWorkManager.hasActiveWorks()) {
            mWorkManager.cancelAllWorks();
            mTextView.setText("Cancel current work");
            return;
        }

        mTextView.setText("start background work");
        Coroutines.backgroundWork(() -> {
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
        }).setStartDelay(1000).manageBy(mWorkManager).start();

    }

    private Producer<Integer> producer() {
        return Producers.consumeBy((Integer element) -> {
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
            mTextView.setText(element);
            return Unit.INSTANCE;
        }).manageBy(mWorkManager).build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWorkManager.cancelAllWorks();
    }
}
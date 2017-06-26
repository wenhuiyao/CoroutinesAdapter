package com.wenhui.coroutines.example;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.wenhui.coroutines.experimental.Coroutines;
import com.wenhui.coroutines.experimental.Working;
import com.wenhui.ktexampleapp.coroutines.BackgroundWorkManager;
import kotlin.Unit;

import static com.wenhui.coroutines.experimental.Coroutines.newBackgroundWork;

/**
 * Created by wyao on 6/24/17.
 */

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";
    private BackgroundWorkManager mJobManager = new BackgroundWorkManager();
    private TextView mTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        View button = findViewById(R.id.button);
        button.setOnClickListener((v)-> onClick());

        mTextView = (TextView)findViewById(R.id.textView);
    }

    private void onClick() {
        if (mJobManager.hasActiveJobs()) {
            mJobManager.cancelJobs();
            mTextView.setText("Cancel current work");
            return;
        }

        mTextView.setText("start background work");
        Working working = newBackgroundWork(() -> {
            Log.d(TAG, "enter with background thread: " + getCurrentThreadName());
            final int count = 1000;
            startBackgroundWork(count);
            return count;
        }).transform((count) -> {
            Log.d(TAG, "enter background transformation: " + getCurrentThreadName());
            return startBackgroundTransformation(count);
        }).transform(Coroutines.CONTEXT_NON_CANCELLABLE, (count) -> {
            Log.d(TAG, "enter non cancellable transformation: " + getCurrentThreadName());
            return count;
        }).transform(Coroutines.CONTEXT_UI, (count) -> {
            Log.d(TAG, "enter UI transformation: " + getCurrentThreadName());
            mTextView.setText("transform in UI Thread");
            return "transformed " + (count * 2);
        }).transform((value) -> {
            Log.d(TAG, "enter back to background transformation: " + getCurrentThreadName());
            startBackgroundTransformation(100);
            return "[second transform]: " + value;
        }).onSuccess((value) -> {
            Log.d(TAG, "onSuccess");
            mTextView.setText("final value =" + value);
            return Unit.INSTANCE;
        }).onError(e -> {
            Log.d(TAG, "onError");
            Toast.makeText(this, "error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return Unit.INSTANCE;
        }).setStartDelay(2000).start();

        mJobManager.addActiveWork(working);
    }

    private void startBackgroundWork(int count){
        try {
            int sleep = count;
            Log.d(TAG, "[with background] sleep " + sleep + "ms");
            Thread.sleep(sleep);
        } catch (InterruptedException ignored) {
        }
    }

    private int startBackgroundTransformation(int count){
        try {
            int sleep = count;
            Log.d(TAG, "[transform background] sleep " + sleep + "ms");
            Thread.sleep(sleep);
        } catch (InterruptedException ignored) {
        }
        return count;
    }

    private static String getCurrentThreadName(){
        return Thread.currentThread().getName();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mJobManager.cancelJobs();
    }
}

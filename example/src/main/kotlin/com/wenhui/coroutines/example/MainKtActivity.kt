package com.wenhui.coroutines.example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.wenhui.coroutines.CoroutineContexts
import com.wenhui.coroutines.and
import com.wenhui.coroutines.from
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


class MainKtActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        simpleBackgroundWorkButton.setOnClickListener {
            onSimpleBackgroundWorkClick()
        }

        simpleMergeWorkButton.setOnClickListener {
            onSimpleMergeWorkButtonClick()
        }

        retrofitButton.setOnClickListener {
            doRetrofitWork()
        }
    }

    private fun onSimpleBackgroundWorkClick() {
        textView.text = "Start simple background work"
        from {
            Thread.sleep(1000)
            1000
        }.consume {
            Thread.sleep(200)
        }.onSuccess {
            Log.d(TAG, "onSuccess: $it")
        }.onError {
            Log.d(TAG, "onError: ${it.message}")
        }.start()
    }

    private fun onSimpleMergeWorkButtonClick() {
        and({
            ThreadUtils.sleep(1000)
            "Merge "
        }, {
            ThreadUtils.sleep(2000)
            1000
        }).merge { str, int ->
            "$str $int"
        }.consume(CoroutineContexts.UI) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }.onSuccess {
            textView.text = it
        }.onError {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        }.start()
    }

    private fun doRetrofitWork() {
        textView.text = "Start requesting google books"
        val retrofit = Retrofit.Builder()
                .baseUrl("https:www.googleapis.com")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

        val retrofitService = retrofit.create(RetrofitService::class.java)
        val call = retrofitService.getGoogleBook("isbn:0747532699")

        from(RetrofitWork(call)).transform {
            it.items[0].volumeInfo
        }.onSuccess {
            textView.text = it.title
        }.onError {
            Toast.makeText(this, "request error", Toast.LENGTH_SHORT).show()
        }.start()
    }
}
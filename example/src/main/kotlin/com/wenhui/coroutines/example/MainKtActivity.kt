package com.wenhui.coroutines.example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.wenhui.coroutines.from
import kotlinx.android.synthetic.main.activity_main.*


class MainKtActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        simpleBackgroundWorkButton.setOnClickListener {
            from {
                Thread.sleep(1000)
                1000
            }.consume {
                Thread.sleep(200)
            }.onSuccess {
                Log.d("MainActivity", "onSuccess: $it")
            }.onError {
                Log.d("MainActivity", "onError: ${it.message}" )
            }.start()
        }
    }
}
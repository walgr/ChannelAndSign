package com.wpf.jiagu

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TestMainActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this))
    }
}
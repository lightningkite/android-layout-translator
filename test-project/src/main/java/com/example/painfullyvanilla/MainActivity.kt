package com.example.painfullyvanilla

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewbinding.ViewBinding
import com.example.painfullyvanilla.databinding.TestEditBinding
import com.example.painfullyvanilla.databinding.TestSystemEdgesBinding
import com.lightningkite.safeinsets.SafeInsetsInterceptor
import io.github.inflationx.viewpump.ViewPump
import io.github.inflationx.viewpump.ViewPumpContextWrapper

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewPump.init(ViewPump.builder().addInterceptor(SafeInsetsInterceptor).build())
        val xml = TestSystemEdgesBinding.inflate(layoutInflater)
        setContentView(xml.root)
    }
}
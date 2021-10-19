package com.example.painfullyvanilla

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.ViewPumpAppCompatDelegate
import com.example.painfullyvanilla.databinding.TestSystemEdgesBinding
import com.lightningkite.safeinsets.SafeInsetsInterceptor
import dev.b3nedikt.viewpump.ViewPump

class MainActivity : AppCompatActivity() {
    private var appCompatDelegate: AppCompatDelegate? = null
    override fun getDelegate(): AppCompatDelegate {
        if (appCompatDelegate == null) {
            appCompatDelegate = ViewPumpAppCompatDelegate(
                super.getDelegate(),
                this
            )
        }
        return appCompatDelegate!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewPump.init(SafeInsetsInterceptor)
        val xml = TestSystemEdgesBinding.inflate(layoutInflater)
        setContentView(xml.root)
    }
}
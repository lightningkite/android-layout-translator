package com.lightningkite.androidruntime

import android.annotation.SuppressLint
import com.lightningkite.safeinsets.R
import dev.b3nedikt.viewpump.InflateResult
import dev.b3nedikt.viewpump.Interceptor

object SafeInsetsInterceptor: Interceptor {
    @SuppressLint("ResourceType")
    override fun intercept(chain: Interceptor.Chain): InflateResult {
        val result = chain.proceed(chain.request())
        if(result.view == null) return result
        val t = result.context.theme.obtainStyledAttributes(result.attrs, intArrayOf(
            R.attr.safeInsets,
            R.attr.safeInsetsSizing,
            R.attr.safeInsetsBoth
        ), 0, 0)
        t.getInt(0, 0).takeIf { it != 0 }?.let { result.view!!.safeInsets(it) }
        t.getInt(1, 0).takeIf { it != 0 }?.let { result.view!!.safeInsetsSizing(it) }
        t.getInt(2, 0).takeIf { it != 0 }?.let { result.view!!.safeInsetsBoth(it) }
        t.recycle()
        return result
    }

}

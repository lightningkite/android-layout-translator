package com.lightningkite.safeinsets

import io.github.inflationx.viewpump.InflateResult
import io.github.inflationx.viewpump.Interceptor

object SafeInsetsInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): InflateResult {
        val result = chain.proceed(chain.request())
        if(result.view == null) return result
        val t = result.context.theme.obtainStyledAttributes(result.attrs, intArrayOf(R.attr.systemEdges), 0, 0)
        val value = t.getInt(0, 0)
        t.recycle()
        if(value != 0) {
            result.view!!.safeInsets(value)
        }
        return result
    }

}

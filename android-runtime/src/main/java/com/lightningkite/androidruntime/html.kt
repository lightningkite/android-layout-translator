package com.lightningkite.androidruntime

import android.os.Build
import android.text.Html
import android.widget.TextView

fun TextView.setTextHtml(html: String) {
    this.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    } else {
        Html.fromHtml(html)
    }
}
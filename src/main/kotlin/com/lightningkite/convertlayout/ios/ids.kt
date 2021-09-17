package com.lightningkite.convertlayout.ios

private val idCharOptions = "0123456789QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm"
fun generateId(): String = buildString {
    repeat(3) { append(idCharOptions.random()) }
    append('-')
    repeat(2) { append(idCharOptions.random()) }
    append('-')
    repeat(3) { append(idCharOptions.random()) }
}
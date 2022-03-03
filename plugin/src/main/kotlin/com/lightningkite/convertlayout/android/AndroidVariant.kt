package com.lightningkite.convertlayout.android

data class AndroidVariant(
    val widerThan: Int? = null
): Comparable<AndroidVariant> {
    val parts: List<String> get() {
        val parts = ArrayList<String>()
        widerThan?.let { parts += "w${it}dp" }
        return parts
    }
    override fun toString(): String {
        return parts.joinToString("-")
    }
    val suffix: String get() = parts.takeUnless { it.isEmpty() }?.joinToString("-", "-") ?: ""
    val camelCaseSuffix: String get() = parts.takeUnless { it.isEmpty() }?.joinToString("") ?: ""
    companion object {
        fun parse(string: String): AndroidVariant {
            val parts = string.split('-')
            return AndroidVariant(
                widerThan = parts.find { it.startsWith('w') }?.let {
                    it.removePrefix("w").takeWhile { it.isDigit() }.toIntOrNull()
                }
            )
        }
    }

    override fun compareTo(other: AndroidVariant): Int {
        return -(this.widerThan ?: 0).compareTo(other.widerThan ?: 0)
    }
}
package com.lightningkite.convertlayout.util

class DeferMap<K, V>(val from: List<Map<K, V>>): Map<K, V> {
    override val entries: Set<Map.Entry<K, V>>
        get() = from.asReversed().reduce { a, b -> a + b }.entries
    override val keys: Set<K>
        get() = from.flatMap { it.keys }.toSet()
    override val size: Int
        get() = from.sumBy { it.size }
    override val values: Collection<V>
        get() = from.flatMap { it.values }

    override fun containsKey(key: K): Boolean = from.any { it.containsKey(key) }

    override fun containsValue(value: V): Boolean = from.any { it.containsValue(value) }

    override fun get(key: K): V? = from.asSequence()
        .firstOrNull { it.containsKey(key) }
        ?.get(key)

    override fun isEmpty(): Boolean = from.all { it.isEmpty() }
}

infix fun <K, V> Map<K, V>.addLazy(other: Map<K, V>): DeferMap<K, V> = DeferMap(listOf(other, this))
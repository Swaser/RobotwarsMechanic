package com.noser.robotwars.mechanic

class Detailed<T>
private constructor(val value: T,
                    val details: List<String>) {

    operator fun component1(): T = value
    operator fun component2(): List<String> = details

    fun <U> map(f: (T) -> U): Detailed<U> = flatMap { none(f(value)) }

    fun <U> flatMap(f: (T) -> Detailed<U>): Detailed<U> {

        val temp = f(value)
        val combinedDetails = details.toMutableList()
        combinedDetails.addAll(temp.details)
        return Detailed(temp.value, combinedDetails)
    }

    fun addDetail(detail: String): Detailed<T> = flatMap { single(it) { detail } }

    companion object {

        fun <T> none(value: T): Detailed<T> = Detailed(value, emptyList())

        fun <T> single(value: T, detail: (T) -> String): Detailed<T> = Detailed(value, listOf(detail(value)))

        fun <U, V> lift(f: (U) -> Detailed<V>): (Detailed<U>) -> Detailed<V> = { it.flatMap(f) }
    }
}
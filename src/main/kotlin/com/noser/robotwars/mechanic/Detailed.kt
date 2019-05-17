package com.noser.robotwars.mechanic

class Detailed<T>
private constructor(val value: T,
                    private val details: List<String>) {

    operator fun component1(): T = value
    operator fun component2(): List<String> = details

    fun <U> map(f: (T) -> U): Detailed<U> = Detailed(f(value), details)

    fun addDetail(detail: String): Detailed<T> = Detailed(value, details.toMutableList().apply { add(detail) })

    fun <U> flatMap(f: (T) -> Detailed<U>): Detailed<U> {

        val temp = f(value)
        val combinedDetails = details.toMutableList()
        combinedDetails.addAll(temp.details)
        return Detailed(temp.value, combinedDetails)
    }

    companion object {

        fun empty(detail: () -> String): Detailed<Unit> = Detailed(Unit, listOf(detail()))

        fun <T> none(value: T): Detailed<T> = Detailed(value, emptyList())

        fun <T> single(value: T, detail: (T) -> String): Detailed<T> = Detailed(value, listOf(detail(value)))
    }
}
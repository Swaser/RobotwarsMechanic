package com.noser.robotwars.mechanic

class Detailed<T>
private constructor(val value: T,
                    val details: List<String>) {

    operator fun component1(): T = value
    operator fun component2(): List<String> = details

    fun addDetail(detail: String): Detailed<T> =
        Detailed(value, details.toMutableList().apply { add(detail) })

    fun addDetails(someDetails: Iterable<String>): Detailed<T> =
        Detailed(value, details.toMutableList().apply { addAll(someDetails) })

    fun <U> map(f: (T) -> U): Detailed<U> = Detailed(f(value), details)

    fun mapDetails(t: (String) -> String) = Detailed(value, details.map(t))

    fun <U> flatMap(f: (T) -> Detailed<U>): Detailed<U> {

        val temp = f(value)
        val combinedDetails = details.toMutableList()
        combinedDetails.addAll(temp.details)
        return Detailed(temp.value, combinedDetails)
    }

    companion object {

        fun <T> none(value: T): Detailed<T> = Detailed(value, emptyList())

        fun <T> single(value: T, detail: String): Detailed<T> = Detailed(value, listOf(detail))

        fun <T> many(value: T, details: List<String>): Detailed<T> = Detailed(value, details)

        fun <T> many(value: T, vararg details: String): Detailed<T> = Detailed(value, details.toList())
    }
}
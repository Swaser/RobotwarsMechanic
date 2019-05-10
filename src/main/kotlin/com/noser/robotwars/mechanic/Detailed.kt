package com.noser.robotwars.mechanic

class Detailed<T>(val value: T,
                  val details: List<String>) {

    operator fun component1() : T = value
    operator fun component2() : List<String> = details

    fun <U> flatMap(f: (T) -> Detailed<U>): Detailed<U> {

        val temp = f(value)
        val combinedDetails = details.toMutableList()
        combinedDetails.addAll(temp.details)
        return Detailed(temp.value, combinedDetails)
    }
}
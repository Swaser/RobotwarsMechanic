package com.noser.robotwars.mechanic


interface Decoupler<U> {

    fun <V> later(job: (U) -> V): Decoupler<V>

}


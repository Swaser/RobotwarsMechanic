package com.noser.robotwars.mechanic


interface Async<U> {

    fun forEach(f : (U) -> Unit)

    fun <V> map(f: (U) -> V): Async<V>

    fun <V> flatMap(f : (U) -> Async<V>) : Async<V>
}


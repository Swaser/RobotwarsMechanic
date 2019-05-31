package com.noser.robotwars.mechanic


interface Async<U> {

    fun done(u: U): Async<U>

    fun exception(t: Throwable): Async<U>

    fun <V> map(f: (U) -> V): Async<V>

    fun <V> flatMap(f: (U) -> Async<V>): Async<V>

    fun finally(f : (U,Throwable?) -> Unit)
}


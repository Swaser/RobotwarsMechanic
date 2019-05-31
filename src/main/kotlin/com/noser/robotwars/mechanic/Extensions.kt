package com.noser.robotwars.mechanic

object Extensions {

    fun <A, B, C, D> Pair<A, B>.flatMap(f: (A, B) -> Pair<C, D>): Pair<C, D> {
        return f(this.first, this.second)
    }

    fun <U, V> map(f: (U) -> V): (Iterable<U>) -> Iterable<V> = { us -> us.map(f) }

    fun <U, V> flatMap(f: (U) -> Iterable<V>): (Iterable<U>) -> Iterable<V> = { us -> us.flatMap(f) }

    infix fun <U, V, W> ((V) -> W).after(f: (U) -> V): (U) -> W = { u -> this(f(u)) }

    infix fun <U, V, W> ((U) -> V).before(f: (V) -> W): (U) -> W = { u -> f(this(u)) }
}
package com.noser.robotwars.mechanic

object Utils {

    fun <A, B, C> flipArgs(f: (A, B) -> C): (B, A) -> C = { b: B, a: A -> f(a, b) }

    fun <A, B, C> curry(f: (A, B) -> C): (A) -> ((B) -> C) = { a: A ->
        { b: B -> f(a, b) }
    }

    fun <A, B, C, D> curry(f: (A, B, C) -> D): (A) -> ((B) -> ((C) -> D)) = { a: A ->
        { b: B ->
            { c: C ->
                f(a, b, c)
            }
        }
    }

}
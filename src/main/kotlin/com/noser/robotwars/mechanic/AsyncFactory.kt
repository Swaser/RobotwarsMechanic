package com.noser.robotwars.mechanic

interface AsyncFactory {

    fun <A> supplyAsync(supplier : () -> A) : Async<A>

    fun <A> direct(a : A) : Async<A>

    fun <A> deferred() : Async<A>

}
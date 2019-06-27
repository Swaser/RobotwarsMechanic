package com.noser.robotwars.mechanic

interface AsyncFactory {

    fun <A> supplyOne(supplier : () -> A) : Observable<A>

    fun <A> just(a : A) : Observable<A>

    fun <A> source() : Source<A>
}
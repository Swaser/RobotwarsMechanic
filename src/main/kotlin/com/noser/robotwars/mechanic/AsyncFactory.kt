package com.noser.robotwars.mechanic

import io.reactivex.Observable
import io.reactivex.subjects.Subject

interface AsyncFactory {

    fun <A> supplyOne(supplier : () -> A) : Observable<A>

    fun <A> just(a : A) : Observable<A>

    fun <A> source() : Subject<A>
}
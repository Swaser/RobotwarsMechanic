package com.noser.robotwars.mechanic

interface AsyncSubject<T> : Async<T>, AsyncListener<T>

interface AsyncFactory {

    fun <E> later(supplier : () -> E) : Async<E>

    fun <E> just(element : E) : Async<E>

    fun <E> subject() : AsyncSubject<E>
}
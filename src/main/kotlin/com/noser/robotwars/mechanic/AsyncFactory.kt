package com.noser.robotwars.mechanic

import java.util.concurrent.Flow

interface AsyncFactory {

    fun <E> later(supplier: () -> E): Flow.Publisher<E>

    fun <E> just(element: E): Flow.Publisher<E>

    fun <E> subject(): Flow.Processor<E, E>
}
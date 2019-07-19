package com.noser.robotwars.mechanic

import java.util.concurrent.Flow

interface AsyncFactory {

    fun <E> later(supplier: () -> E): Flow.Publisher<E>

    fun <E> just(element: E): Flow.Publisher<E>

    fun <E> subject(): Flow.Processor<E, E>


    companion object {

        fun <E> subscriber(onNext: (E) -> Unit,
                           onComplete: () -> Unit = {},
                           onError: (Throwable) -> Unit = { println("huch: ${it.message}") },
                           onSubscribe: (Flow.Subscription) -> Unit = { it.request(Long.MAX_VALUE) }) =
            object : Flow.Subscriber<E> {
                override fun onComplete() {
                    onComplete()
                }

                override fun onSubscribe(subscription: Flow.Subscription) {
                    onSubscribe(subscription)
                }

                override fun onNext(item: E) {
                    onNext(item)
                }

                override fun onError(throwable: Throwable) {
                    onError(throwable)
                }
            }

    }
}
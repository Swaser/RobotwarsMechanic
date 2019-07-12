package com.noser.robotwars.mechanic

interface AsyncListener<E> {

    fun onNext(element: E) {}

    fun onError(throwable: Throwable) {}

    fun onComplete() {}
}

interface Async<E> {

    fun <F> flatMap(mapper: (E) -> Async<F>): Async<F>

    fun subscribe(listener: AsyncListener<E>): Async<E> =
        subscribe(listener::onNext, listener::onError, listener::onComplete)

    fun subscribe(onNext: (E) -> Unit,
                  onError: (Throwable) -> Unit = {},
                  onComplete: () -> Unit = {}): Async<E>
}

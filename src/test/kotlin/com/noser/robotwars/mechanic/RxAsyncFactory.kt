package com.noser.robotwars.mechanic

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CompletableFuture

object RxAsyncFactory : AsyncFactory {

    private class RxAsync<E>(private val observable: Observable<E>) : Async<E> {

        override fun <F> flatMap(mapper: (E) -> Async<F>): Async<F> =
            RxAsync(observable.flatMap { toObservable(mapper(it)) })

        override fun subscribe(onNext: (E) -> Unit,
                               onError: (Throwable) -> Unit,
                               onComplete: () -> Unit): Async<E> {

            observable.subscribe(onNext, onError, onComplete)
            return this
        }

        companion object {

            fun <E> toObservable(async: Async<E>): Observable<E> = Observable
                .create<E> { emitter ->
                    async.subscribe({ emitter.onNext(it) },
                                    { emitter.onError(it) },
                                    { emitter.onComplete() })
                }
        }
    }

    override fun <A> later(supplier: () -> A): Async<A> {

        val observable = Observable.create { emitter: ObservableEmitter<A> ->
            CompletableFuture.supplyAsync(supplier).whenComplete { someA, throwable ->
                if (throwable == null) {
                    emitter.onNext(someA)
                    emitter.onComplete()
                } else {
                    emitter.onError(throwable)
                }
            }
        }.cache()

        return RxAsync(observable)
    }

    override fun <E> just(element: E): Async<E> = RxAsync(Observable.just(element))

    override fun <E> subject(): AsyncSubject<E> {

        return object : AsyncSubject<E> {

            val subject = PublishSubject.create<E>()

            override fun onNext(element: E) {
                subject.onNext(element)
            }

            override fun onError(throwable: Throwable) {
                subject.onError(throwable)
            }

            override fun onComplete() {
                subject.onComplete()
            }

            override fun <F> flatMap(mapper: (E) -> Async<F>): Async<F> {
                return RxAsync<F>(subject.flatMap { RxAsync.toObservable(mapper(it)) })
            }

            override fun subscribe(onNext: (E) -> Unit,
                                   onError: (Throwable) -> Unit,
                                   onComplete: () -> Unit): Async<E> {

                subject.subscribe(onNext, onError, onComplete)
                return this
            }
        }
    }
}



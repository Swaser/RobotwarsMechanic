package com.noser.robotwars.mechanic

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.slf4j.LoggerFactory
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

object CFAsyncFactory : AsyncFactory {

    private val logger = LoggerFactory.getLogger(CFAsyncFactory::class.java)

    override fun <A> supplyOne(supplier: () -> A): Observable<A> {
        return Observable.create { emitter: ObservableEmitter<A> ->
            CompletableFuture.supplyAsync(supplier).whenComplete { a, t ->
                println("whenComplete called")
                if (t == null) {
                    emitter.onNext(a)
                    emitter.onComplete()
                } else {
                    emitter.onError(t)
                }
            }
        }.cache()
    }

    override fun <A> just(a: A): Observable<A> {
        return Observable.just(a)
    }

    override fun <A> source(): Subject<A> {
        return PublishSubject.create()
    }


}



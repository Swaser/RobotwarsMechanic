package com.noser.robotwars.mechanic

import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import org.reactivestreams.FlowAdapters
import org.reactivestreams.Publisher
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow

object RxAsyncFactory : AsyncFactory {

    override fun <A> later(supplier: () -> A): Flow.Publisher<A> {

        val observable: Publisher<A> = Observable.create { emitter: ObservableEmitter<A> ->
            CompletableFuture.supplyAsync(supplier).whenComplete { someA, throwable ->
                if (throwable == null) {
                    emitter.onNext(someA)
                    emitter.onComplete()
                } else {
                    emitter.onError(throwable)
                }
            }
        }.cache().toFlowable(BackpressureStrategy.BUFFER)

        return FlowAdapters.toFlowPublisher(observable)
    }

    override fun <E> just(element: E): Flow.Publisher<E> = FlowAdapters
        .toFlowPublisher(Single.just(element).toFlowable())

    override fun <E> subject(): Flow.Processor<E, E> = FlowAdapters
        .toFlowProcessor(PublishProcessor.create())
}



package com.noser.robotwars.mechanic

import org.slf4j.LoggerFactory
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

object CFAsyncFactory : AsyncFactory {

    private val logger = LoggerFactory.getLogger(CFAsyncFactory::class.java)

    private val counter = AtomicInteger(0)
    private val executor: Executor = ThreadPoolExecutor(10,
                                                        100,
                                                        60L,
                                                        TimeUnit.SECONDS,
                                                        LinkedBlockingQueue(),
                                                        ThreadFactory {
                                                            logger.info("Thread created for CFPool : ${counter.incrementAndGet()}")
                                                            Thread(it).apply {
                                                                isDaemon = true
                                                                name = "CFAsyncThread-${counter.get()}"
                                                            }
                                                        })

    override fun <A> supplyOne(supplier: () -> A): Observable<A> {
        return CFObservable(CompletableFuture.supplyAsync(Supplier(supplier), executor))
    }

    override fun <A> just(a: A): Observable<A> {
        return CFObservable(CompletableFuture.completedFuture(a))
    }

    override fun <A> source(): Source<A> {

        val cfAsync = CFObservable(CompletableFuture<A>())

        return object : Source<A> {

            override fun <V> map(f: (A) -> V): Observable<V> = cfAsync.map(f)

            override fun <V> flatMap(f: (A) -> Observable<V>): Observable<V> = cfAsync.flatMap(f)

            override fun observe(observer: Observer<A>): Observable<A> = cfAsync.observe(observer)

            override fun push(a: A) {
                cfAsync.complete(a)
            }

            override fun done() {
                // nothing to be done for CF
            }

            override fun pushException(e: Exception) {
                cfAsync.completeExceptionally(e)
            }
        }
    }

    private class CFObservable<U>(private val cf: CompletableFuture<U>) : Observable<U> {

        override fun observe(observer: Observer<U>): Observable<U> {
            cf.handle { u, t ->
                if (t != null) {
                    observer.onException(if (t is Exception) t else AsyncException(t))
                    observer.onDone()
                } else {
                    observer.onNext(u)
                    observer.onDone()
                }
            }
            return this
        }

        fun complete(u: U) {
            cf.complete(u)
        }

        fun completeExceptionally(e: Exception) {
            cf.completeExceptionally(e)
        }

        override fun <V> map(f: (U) -> V): Observable<V> {
            return CFObservable(cf.thenApplyAsync(f))
        }

        override fun <V> flatMap(f: (U) -> Observable<V>): Observable<V> {

            val res = source<V>()

            return res
        }
    }
}



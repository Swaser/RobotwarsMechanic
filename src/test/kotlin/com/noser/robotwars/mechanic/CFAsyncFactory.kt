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

    override fun <A> supplyAsync(supplier: () -> A): Async<A> {
        return CFAsync(CompletableFuture.supplyAsync(Supplier(supplier), executor))
    }

    override fun <A> direct(a: A): Async<A> {
        return CFAsync(CompletableFuture.completedFuture(a))
    }

    override fun <A> deferred(): Async<A> {
        return CFAsync(CompletableFuture())
    }

    private class CFAsync<U>(private val cf: CompletableFuture<U>) : Async<U> {

        override fun done(u: U): Async<U> {
            cf.complete(u)
            return this
        }

        override fun exception(t: Throwable): Async<U> {
            cf.completeExceptionally(t)
            return this
        }

        override fun <V> map(f: (U) -> V): Async<V> {
            return CFAsync(cf.thenApplyAsync(f))
        }

        override fun <V> flatMap(f: (U) -> Async<V>): Async<V> {

            val res = deferred<V>()

            finally { u, t ->
                if (t !== null) {
                    res.exception(t)
                } else {
                    f(u).finally { v, t2 ->
                        if (t2 != null) res.exception(t2) else res.done(v)
                    }
                }
            }

            return res
        }

        override fun finally(f: (U, Throwable?) -> Unit) {
            cf.whenComplete(f)
        }
    }
}



package com.noser.robotwars.mechanic

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Function
import java.util.function.Supplier

class ExcutorAsync<U>
private constructor(private val cf: CompletableFuture<U>) : Async<U> {

    override fun forEach(f: (U) -> Unit) {
        cf.thenAccept(f)
    }

    override fun <V> map(f: (U) -> V): Async<V> {

        return ExcutorAsync(cf.thenApplyAsync(Function { someU -> f(someU) }, executors))
    }

    override fun <V> flatMap(f: (U) -> Async<V>): Async<V> {

        val resCf = CompletableFuture<V>()

        forEach { someU -> f(someU).forEach { resCf.complete(it) } }

        return ExcutorAsync(resCf)
    }

    companion object {

        fun <U> some(someU: U): Async<U> = ExcutorAsync(CompletableFuture.completedFuture(someU))

        fun <U> supply(job: () -> U): Async<U> =
            ExcutorAsync(CompletableFuture.supplyAsync(Supplier(job), executors))

        private val executors: Executor = Executors.newCachedThreadPool {
            Thread(it).apply {
                isDaemon = true
            }
        }
    }

}


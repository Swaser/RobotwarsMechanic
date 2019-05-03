package com.noser.robotwars.mechanic

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Function
import java.util.function.Supplier

class ExcutorDecoupler<U>
private constructor(private val cf: CompletableFuture<U>) : Decoupler<U> {

    override fun <V> later(job: (U) -> V): Decoupler<V> {

        return ExcutorDecoupler(cf.thenApplyAsync(Function { someU -> job(someU) }, executors))
    }

    companion object {

        fun <U> some(someU: U): Decoupler<U> = ExcutorDecoupler(CompletableFuture.completedFuture(someU))

        fun <U> supply(job: () -> U): Decoupler<U> =
            ExcutorDecoupler(CompletableFuture.supplyAsync(Supplier(job), executors))

        private val executors: Executor = Executors.newCachedThreadPool {
            Thread(it).apply {
                isDaemon = true
            }
        }
    }

}


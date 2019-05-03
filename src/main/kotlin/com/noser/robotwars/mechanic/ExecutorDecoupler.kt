package com.noser.robotwars.mechanic

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object ExcutorDecoupler : Decoupler {

    private val executors: Executor = Executors.newCachedThreadPool {
        Thread(it).apply {
            isDaemon = true
        }
    }

    override fun decouple(job: () -> Unit): Decoupled {
        return ExecutorDecoupled(CompletableFuture.runAsync(Runnable(job), executors))
    }
}

class ExecutorDecoupled(private val future: CompletableFuture<Void>) : Decoupled {
    override fun whenDone(job: () -> Unit) {
        future.thenAccept { job() }
    }
}

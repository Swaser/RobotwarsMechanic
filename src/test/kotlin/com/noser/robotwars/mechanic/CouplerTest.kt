package com.noser.robotwars.mechanic

import io.reactivex.Observer
import org.junit.Test
import kotlin.test.assertEquals

class CouplerTest {

    @Volatile
    var a: Int = -10

    @Test
    fun testSupplyOne() {

        val observable = CFAsyncFactory.supplyOne { 3 }
        observable.subscribe { println(it) }
        observable.subscribe { println(it) }

        Thread.sleep(200L)
    }

    @Test
    fun veryNoStackOverflow() {

        a = -10
        val source = CFAsyncFactory.source<Int>()
        source.subscribe { println(it) }
        conduct(10_000, source)

        Thread.sleep(3_000)

        assertEquals(0, a)
    }

    private fun conduct(i: Int, o: Observer<Int>) {

        if (i >= 0)
            CFAsyncFactory
                .supplyOne { i }
                .subscribe({
                               a = it
                               o.onNext(it)
                               conduct(it - 1, o)
                           },
                           { println(it.message) })

    }
}


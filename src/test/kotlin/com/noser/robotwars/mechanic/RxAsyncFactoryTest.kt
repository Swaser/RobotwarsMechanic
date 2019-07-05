package com.noser.robotwars.mechanic

import org.junit.Test
import kotlin.test.assertEquals

class RxAsyncFactoryTest {

    @Volatile
    var a: Int = -10

    @Test
    fun testSupplyOne() {

        val observable = RxAsyncFactory.later { 3 }
        observable.subscribe({ println(it) })
        observable.subscribe({ println(it) })

        Thread.sleep(200L)
    }

    @Test
    fun veryNoStackOverflow() {

        a = -10
        val subject = RxAsyncFactory.subject<Int>()
        subject.subscribe({ println(it) })
        conduct(1_000, subject)

        Thread.sleep(3_000)

        assertEquals(0, a)
    }

    private fun conduct(i: Int, o: AsyncListener<Int>) {

        if (i >= 0)
            RxAsyncFactory
                .later { i }
                .subscribe({
                               a = it
                               o.onNext(it)
                               conduct(it - 1, o)
                           },
                           { println(it.message) })

    }
}


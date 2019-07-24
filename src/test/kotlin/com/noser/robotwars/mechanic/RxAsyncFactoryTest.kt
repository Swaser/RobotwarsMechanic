package com.noser.robotwars.mechanic

import org.junit.Test
import java.util.concurrent.Flow
import kotlin.test.assertEquals

class RxAsyncFactoryTest {

    @Volatile
    var a: Int = -10

    @Test
    fun testSupplyOne() {

        val observable = RxAsyncFactory.just(3)
        observable.subscribe(AsyncFactory.noBackpressureSubscriber({ println(it) }))
        observable.subscribe(AsyncFactory.noBackpressureSubscriber({ println(it) }))

        Thread.sleep(200L)
    }

    @Test
    fun veryNoStackOverflow() {

        a = -10
        val subject = RxAsyncFactory.subject<Int>()
        subject.subscribe(AsyncFactory.noBackpressureSubscriber({ println(it) }))
        conduct(1_000, subject)

        Thread.sleep(3_000)

        assertEquals(0, a)
    }

    private fun conduct(i: Int, o: Flow.Subscriber<Int>) {

        if (i >= 0)
            RxAsyncFactory
                .later { i }
                .subscribe(AsyncFactory.noBackpressureSubscriber({
                                                       a = it
                                                       o.onNext(it)
                                                       conduct(it - 1, o)
                                                   }))
    }
}


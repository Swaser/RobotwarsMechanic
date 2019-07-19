package com.noser.robotwars.mechanic

import org.junit.Test
import java.util.concurrent.Flow
import kotlin.test.assertEquals

class RxAsyncFactoryTest {

    val subscriber = object : Flow.Subscriber<Int> {
        override fun onComplete() {}

        override fun onSubscribe(subscription: Flow.Subscription) {}

        override fun onNext(item: Int) {
            println(item)
        }

        override fun onError(throwable: Throwable) {
            print(throwable.stackTrace.joinToString("\n"))
        }
    }

    @Volatile
    var a: Int = -10

    @Test
    fun testSupplyOne() {


        val observable = RxAsyncFactory.later { 3 }
        observable.subscribe(subscriber)
        observable.subscribe(subscriber)

        Thread.sleep(200L)
    }

    @Test
    fun veryNoStackOverflow() {

        a = -10
        val subject = RxAsyncFactory.subject<Int>()
        subject.subscribe(subscriber)
        conduct(1_000, subject)

        Thread.sleep(3_000)

        assertEquals(0, a)
    }

    private fun conduct(i: Int, o: Flow.Subscriber<Int>) {

        if (i >= 0)
            RxAsyncFactory
                .later { i }
                .subscribe(object : Flow.Subscriber<Int> {

                    override fun onComplete() {
                        println(a)
                    }

                    override fun onSubscribe(subscription: Flow.Subscription) {}

                    override fun onNext(item: Int) {
                        a = item
                        o.onNext(item)
                        conduct(item -1, o)
                    }

                    override fun onError(throwable: Throwable) {
                        print(throwable.stackTrace.joinToString("\n"))
                    }
                })
    }
}


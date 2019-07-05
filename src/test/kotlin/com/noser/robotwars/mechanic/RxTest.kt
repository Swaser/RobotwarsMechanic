package com.noser.robotwars.mechanic

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class RxTest {

    @Test
    fun testSubscribeOn() {

        val subject = PublishSubject.create<Int>()

        Single.create<Int> { it.onSuccess(3) }

        Thread {
            (1..10).forEach {
                Thread.sleep(1000)
                subject.onNext(it)
            }
            subject.onComplete()
        }.start()

        subject
            .subscribe {
                println("first ${Thread.currentThread().name} $it")
            }

        Thread.sleep(3000)
        subject
            .subscribe({
                if (it == 6) throw RuntimeException()
                println("second ${Thread.currentThread().name} $it")
            }, { t->
                println(t.message)
            })

        Thread.sleep(10000)
    }
}
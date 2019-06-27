package com.noser.robotwars.mechanic

interface Observable<U> {

    fun <V> map(f: (U) -> V): Observable<V>

    fun <V> flatMap(f: (U) -> Observable<V>): Observable<V>

    fun observe(observer: Observer<U>): Observable<U>
}

interface Observer<U> {

    fun onNext(u: U)

    fun onDone()

    fun onException(e: Exception)
}

interface Source<U> : Observable<U> {

    fun push(u: U)

    fun done()

    fun pushException(e: Exception)
}

class AsyncException(t: Throwable) : RuntimeException(t)
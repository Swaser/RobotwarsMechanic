package com.noser.robotwars.mechanic.bout

import io.reactivex.subjects.PublishSubject
import java.util.*

class PendingResponse(val uuid: UUID) {

    val subject: PublishSubject<Move> = PublishSubject.create()

    fun moveResponseReceived(move: Move) {
        subject.onNext(move)
        subject.onComplete()
    }

    companion object {
        fun none() = PendingResponse(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        fun new() = PendingResponse(UUID.randomUUID())
    }
}

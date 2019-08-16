package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.Detailed
import java.util.*

class PendingMoveRequest(val detailedArena: Detailed<Arena>,
                         asyncFactory: AsyncFactory) {

    val uuid: UUID = UUID.randomUUID()

    val subject = asyncFactory.subject<Pair<Detailed<Arena>,Move>>()

    fun moveResponseReceived(move: Move) {
        subject.onNext(Pair(detailedArena, move))
        subject.onComplete()
    }

}

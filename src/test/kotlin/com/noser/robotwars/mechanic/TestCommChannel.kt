package com.noser.robotwars.mechanic

import com.noser.robotwars.mechanic.bout.Arena
import com.noser.robotwars.mechanic.bout.Move
import com.noser.robotwars.mechanic.tournament.CommChannel
import com.noser.robotwars.mechanic.tournament.Competitor

class TestCommChannel: CommChannel {

    var competitor: Competitor? = null

    override fun nextMove(arena: Arena): Move? {
        return Move(competitor!!,
             listOf(),
            0,
            null,
            0,
            null)
    }

    override fun publishResult(arena: Arena, winner: Competitor?) {

    }

    override fun publishErrorHappened(arena: Arena, throwable: Throwable) {

    }
}
package com.noser.robotwars.mechanic

import com.noser.robotwars.mechanic.bout.Arena
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.bout.Move
import com.noser.robotwars.mechanic.tournament.CommChannel
import com.noser.robotwars.mechanic.tournament.Competitor
import com.noser.robotwars.mechanic.tournament.Tournament

class TestCommChannel: CommChannel {

    var competitor: Competitor? = null

    override fun nextMove(arena: Arena): Move? {
        return Move(0,
             listOf(),
            0,
            null,
            0,
            null)
    }

    override fun publishResult(arena: Arena, winner: Competitor) {

    }

    override fun publishErrorHappened(arena: Arena, throwable: Throwable) {

    }

    override fun notifyBout(bout: Bout) {

    }

    override fun notifyTournament(tournament: Tournament) {

    }

    override fun disconnect() {

    }
}
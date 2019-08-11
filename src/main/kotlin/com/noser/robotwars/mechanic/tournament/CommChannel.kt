package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.Arena
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.bout.MoveRequest

interface CommChannel {

    /**
     * notify competitor of a change in a bout
     */
    fun notifyBout(bout: Bout)

    /**
     * notify competitor of a change in a tournament
     */
    fun notifyTournament(tournament: Tournament)

    fun nextMove(request: MoveRequest)

    fun publishResult(arena: Arena, winner : Competitor)

    fun publishErrorHappened(arena: Arena, throwable: Throwable)
}
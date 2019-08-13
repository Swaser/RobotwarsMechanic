package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.Arena
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.bout.MoveRequest

interface CommChannel {

    fun notify(bout: Bout)

    fun notify(tournament: Tournament)

    fun nextMove(request: MoveRequest)

    fun publishResult(arena: Arena, winner : Competitor)

    fun publishErrorHappened(arena: Arena, throwable: Throwable)
}
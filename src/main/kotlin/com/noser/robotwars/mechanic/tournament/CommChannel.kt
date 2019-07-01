package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.Arena
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.bout.Move

interface CommChannel {

    /**
     * notify competitor of a change in a bout
     */
    fun notify(bout: Bout)

    /**
     * If move == null (timeout or any other problem) then the CommChannel should be closed
     */
    fun nextMove(arena: Arena): Move?

    /** null means draw */
    fun publishResult(arena: Arena, winner: Competitor?)

    fun publishErrorHappened(arena: Arena, throwable: Throwable)
}
package com.noser.robotwars.mechanic

class CommChannel {

    /**
     * If move == null (timeout or any other problem) then the CommChannel should be closed
     */
    fun nextMove(arena: Arena) : Move? = TODO()

    fun publishResult(arena: Arena, winner : Player) {

        TODO()
    }
}
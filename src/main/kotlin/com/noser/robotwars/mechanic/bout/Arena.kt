package com.noser.robotwars.mechanic.bout

data class Arena(val activePlayer: Player,
                 val robots: List<Robot>,
                 val bounds: Bounds,
                 val terrain: Grid<Terrain>,
                 val effects: Effects) {

    fun determineWinner(): Player? = TODO()
}

package com.noser.robotwars.mechanic

data class Arena(
    val activePlayer: Player,
    val robots: (Player) -> Robot,
    val terrain: Grid<Terrain>,
    val effects: Grid<List<Effect>>
) {
    companion object {
        fun apply(arena: Arena, move: Move) : Arena = TODO()

        fun determineWinner(arena: Arena) : Player? = TODO()
    }
}
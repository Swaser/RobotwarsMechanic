package com.noser.robotwars.mechanic.bout

import kotlin.random.Random

data class Position(val row: Int,
                    val col: Int) {

    fun move(direction: Direction,
             rows: Int,
             cols: Int): Position? =
        when (direction) {
            Direction.N -> if (row > 0) Position(row - 1, col) else null
            Direction.E -> if (col > 0) Position(row, col - 1) else null
            Direction.S -> if (row < rows) Position(row + 1, col) else null
            Direction.W -> if (col < cols) Position(row, col + 1) else null
        }

    companion object {
        fun random(rows: Int, cols: Int, random: Random) =
            Position(random.nextInt(rows), random.nextInt(cols))
    }
}
package com.noser.robotwars.mechanic.bout

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class Position(
    val row: Int,
    val col: Int
) {

    fun move(
        direction: Direction,
        rows: Int,
        cols: Int
    ): Position =
        when (direction) {
            Direction.N -> Position(max(0, row - 1), col)
            Direction.E -> Position(row, min(cols, col + 1))
            Direction.S -> Position(min(rows, row + 1), col)
            Direction.W -> Position(row, max(0, col - 1))
        }

    companion object {
        fun random(rows: Int, cols: Int, random: Random) =
            Position(random.nextInt(rows), random.nextInt(cols))
    }
}
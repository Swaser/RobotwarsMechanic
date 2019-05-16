package com.noser.robotwars.mechanic.bout

import kotlin.math.abs
import kotlin.random.Random

data class Position(val row: Int,
                    val col: Int) {

    fun move(direction: Direction?, bounds: Bounds): Position? =
        when (direction) {
            Direction.N -> if (row - 1 in bounds.rows) Position(row - 1, col) else null
            Direction.E -> if (col - 1 in bounds.cols) Position(row, col - 1) else null
            Direction.S -> if (row + 1 in bounds.rows) Position(row + 1, col) else null
            Direction.W -> if (col + 1 in bounds.cols) Position(row, col + 1) else null
            else        -> null
        }

    fun distanceTo(other: Position) = abs(other.row - row) + abs(other.col - col)

    companion object {
        fun random(bounds: Bounds, random: Random) =
            Position(bounds.rows.first + random.nextInt(bounds.rows.last - bounds.rows.first),
                     bounds.cols.first + random.nextInt(bounds.cols.last - bounds.cols.first))
    }
}
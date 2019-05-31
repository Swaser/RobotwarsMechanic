package com.noser.robotwars.mechanic.bout

import kotlin.random.Random

data class Bounds(val rows: IntRange,
                  val cols: IntRange) {

    fun random(random: Random) = Position(rows.random(random), cols.random(random))

    val nRows = rows.last - rows.first
    val nCols = cols.last - cols.first

    val positions = rows.flatMap { r -> cols.map { c -> Position(r, c) } }
}
package com.noser.robotwars.mechanic.bout

import kotlin.random.Random

data class Bounds(val rows: IntRange,
                  val cols: IntRange) {

    val nRows = rows.last - rows.first + 1
    val nCols = cols.last - cols.first + 1

    val positions = rows.flatMap { r -> cols.map { c -> Position(r, c) } }
}
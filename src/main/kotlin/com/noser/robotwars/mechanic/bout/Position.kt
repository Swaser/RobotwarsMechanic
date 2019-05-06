package com.noser.robotwars.mechanic.bout

import kotlin.random.Random

data class Position(
    val row: Int,
    val col: Int
) {

    companion object {
        fun random(rows: Int, cols: Int, random: Random) =
            Position(random.nextInt(rows), random.nextInt(cols))
    }
}
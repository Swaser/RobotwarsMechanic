package com.noser.robotwars.mechanic.bout

import kotlin.random.Random

data class Position(
    val row: Int,
    val col: Int
) {

    companion object {
        fun random(n: Int, random: Random) =
            Position(random.nextInt(n), random.nextInt(n))
    }
}
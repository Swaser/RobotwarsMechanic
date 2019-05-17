package com.noser.robotwars.mechanic.bout

enum class Terrain(val movementCost : Int,
                   val attackPossible : Boolean,
                   val preposition : String) {

    GREEN(1, true, "onto"),
    WATER(2, false, "into"),
    ROCK(1000, false, "into");
}
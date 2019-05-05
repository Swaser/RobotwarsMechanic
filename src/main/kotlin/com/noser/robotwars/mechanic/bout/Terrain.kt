package com.noser.robotwars.mechanic.bout

enum class Terrain(val movementCost : Int,
                   val attackPossible : Boolean) {

    GREEN(1, true),
    WATER(2, false),
    ROCK(100, false);
}
package com.noser.robotwars.mechanic.tournament

class Tournament(
    val competitors: List<Competitor>,
    val arenaSize: Int,
    val startingEnergy: Int,
    val chanceForWater: Double,
    val chanceForRock: Double
)
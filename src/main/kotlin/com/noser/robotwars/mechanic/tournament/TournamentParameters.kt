package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.Bounds

data class TournamentParameters(val bounds : Bounds,
                                val startingEnergy: Int,
                                val chanceForWater: Double,
                                val chanceForRock: Double,
                                val maxEnergy: Int,
                                val startingHealth: Int,
                                val startingShield: Int,
                                val maxShield: Int)
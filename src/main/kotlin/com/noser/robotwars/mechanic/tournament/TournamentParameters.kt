package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.Bounds

data class TournamentParameters(val bounds: Bounds,
                                val startingEnergy: Int,
                                val maxEnergy: Int,
                                val chanceForWater: Double,
                                val chanceForRock: Double,
                                val chanceForBurnable: Double,
                                val chanceForEnergy: Double,
                                val startingHealth: Int,
                                val startingShield: Int,
                                val maxShield: Int)
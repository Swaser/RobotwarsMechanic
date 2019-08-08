package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.Bounds

data class TournamentParameters(val bounds: Bounds,
                                val energyRefillPerRound: Int,
                                val terrainWaterChance: Double,
                                val terrainRockChance: Double,
                                val effectBurnableChance: Double,
                                val effectEnergyChance: Double,
                                val effectEnergyMax: Int,
                                val robotHealthInitial: Int,
                                val robotEnergyInitial: Int,
                                val robotEnergyMax: Int,
                                val robotShieldInitial: Int,
                                val robotShieldMax: Int)
package com.noser.robotwars.mechanic.tournament

data class CompetitorStatistics(val uuid: String,
                                val name: String,
                                val wins: Int) {

    fun inc(): CompetitorStatistics {
        return CompetitorStatistics(uuid, name, wins.inc())
    }
}
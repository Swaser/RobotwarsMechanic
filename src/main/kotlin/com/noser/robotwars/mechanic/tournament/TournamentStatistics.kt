package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.Bout

class TournamentStatistics {

    private val stats: MutableMap<String, CompetitorStatistics> = mutableMapOf()

    fun addNewResult(bout: Bout) {
        bout.competitors.forEach { competitor ->
            val uuid = competitor.uuid.toString()
            stats.putIfAbsent(uuid, CompetitorStatistics(uuid, competitor.name, 0))
            stats.computeIfPresent(uuid) { compUuid, compStats ->
                if (compUuid == bout.winner?.uuid?.toString()) {
                    compStats.inc()
                } else {
                    compStats
                }
            }
        }
    }

    fun getAggregatedStats(): List<CompetitorStatistics> {
        return stats.values.toList()
    }
}
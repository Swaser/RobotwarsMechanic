package com.noser.robotwars.mechanic.tournament

import java.util.*

data class BoutResult(val boutId: UUID,
                      val competitors: Map<Competitor, Int>,
                      val winner: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BoutResult

        if (boutId != other.boutId) return false

        return true
    }

    override fun hashCode(): Int {
        return boutId.hashCode()
    }
}

data class TournamentStatistics(private val boutResults: Set<BoutResult>) {
}
package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.team.Team
import java.util.*

/**
 * Represents an active agent in a Tournament. Could be an AI or a human.
 */
data class Competitor(val id: UUID,
                      val team: Team,
                      val commChannel: CommChannel) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Competitor

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
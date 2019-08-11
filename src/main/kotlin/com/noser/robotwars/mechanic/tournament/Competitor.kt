package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.Arena
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.bout.Move
import com.noser.robotwars.mechanic.bout.MoveRequest
import java.util.*

/**
 * Represents an active agent in a Tournament. Could be an AI or a human.
 */
open class Competitor(open val uuid: UUID,
                      open val name: String,
                      private val commChannel: CommChannel) {

    fun notify(bout: Bout) {
        commChannel.notifyBout(bout)
    }

    fun notify(tournament: Tournament) {
        commChannel.notifyTournament(tournament)
    }

    fun nextMove(request: MoveRequest) {
        return commChannel.nextMove(request)
    }

    fun publishResult(arena: Arena,
                      winner: Competitor) {
        commChannel.publishResult(arena, winner)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Competitor

        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    override fun toString(): String {
        return name
    }
}
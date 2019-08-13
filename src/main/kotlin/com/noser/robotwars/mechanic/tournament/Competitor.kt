package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.Arena
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.bout.MoveRequest
import java.util.UUID

/**
 * Represents an active agent in a Tournament. Could be an AI or a human.
 */
class Competitor(val uuid: UUID,
                 val name: String,
                 private val commChannel: CommChannel) {

    @Volatile
    private var killed: Boolean = false

    fun notify(bout: Bout) {
        if(!killed) {
            commChannel.notify(bout)
        }
    }

    fun notify(tournament: Tournament) {
        if(!killed) {
            commChannel.notify(tournament)
        }
    }

    fun nextMove(request: MoveRequest): Boolean {
        if(!killed) {
            commChannel.nextMove(request)
        }
        return !killed
    }

    fun publishResult(arena: Arena,
                      winner: Competitor) {
        if(!killed) {
            commChannel.publishResult(arena, winner)
        }
    }

    fun kill() {
        killed = true
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
package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.MoveRequest
import java.util.*

/**
 * Represents an active agent in a Tournament. Could be an AI or a human.
 */
class Competitor(val uuid: UUID,
                 val name: String,
                 private val commChannel: CommChannel) {

    fun copy() = Competitor(uuid, name, commChannel)

    @Volatile
    private var killed: Boolean = false

    fun nextMove(request: MoveRequest): Boolean {
        if(!killed) {
            commChannel.nextMove(request)
        }
        return !killed
    }

    fun kill() {
        killed = true
    }

    fun isKilled(): Boolean = killed

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
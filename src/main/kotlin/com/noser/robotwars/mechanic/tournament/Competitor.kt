package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.Arena
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.bout.Move
import java.util.*

data class Competitor(val uuid: UUID,
                      val name: String,
                      private val commChannel: CommChannel) {

    @Volatile
    private var harakiri: Boolean = false

    fun harakiri() {
        harakiri = true
    }

    fun notify(bout: Bout) {
        commChannel.notify(bout)
    }

    fun nextMove(arena: Arena): Move? {
        return when {
            harakiri -> null
            else -> commChannel.nextMove(arena)
        }
    }

    fun publishResult(arena: Arena,
                      winner: Competitor?) {
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
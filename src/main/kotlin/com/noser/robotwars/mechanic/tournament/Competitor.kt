package com.noser.robotwars.mechanic.tournament

import java.util.*

class Competitor(val id: UUID,
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
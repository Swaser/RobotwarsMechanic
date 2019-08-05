package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.bout.Bout

interface TournamentChangeListener {
    fun notifyTournamentChanged(tournament: Tournament)
    fun notifyBoutChanged(bout: Bout)
}

package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.Extensions.before
import com.noser.robotwars.mechanic.Extensions.forEach
import com.noser.robotwars.mechanic.bout.Bounds
import com.noser.robotwars.mechanic.bout.Bout

data class TournamentParameters(val bounds : Bounds,
                                val startingEnergy: Int,
                                val chanceForWater: Double,
                                val chanceForRock: Double,
                                val maxEnergy: Int,
                                val startingHealth: Int,
                                val startingShield: Int,
                                val maxShield: Int)

class Tournament(private val competitors: List<Competitor>,
                 val parameters: TournamentParameters,
                 private val asyncFactory: AsyncFactory) {

    /** call this fun repeatedly until tournament done */
    fun startNextRoundOfBouts() {

        (this::findFreeCompetitors before
        this::findStartableBouts before
        forEach(this::startBout))()
    }

    @Synchronized
    private fun startBout(bout: Bout): Unit {
        registerBoutStarted(bout)
        bout.conductBout(asyncFactory)
            .finally { _, throwable ->
                if (throwable == null) {
                    registerBoutEnded(bout)
                } else {
                    TODO()
                }
            }
    }

    @Synchronized
    private fun registerBoutStarted(bout: Bout) {

        bout.competitors().forEach(this::markCompetitorOccupied)

        // do other stuff like an entry into the current bouts table or so

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Synchronized
    private fun registerBoutEnded(bout: Bout) {

        bout.competitors().forEach(this::markCompetitorFree)

        // do other stuff like an entry into the completed bout table

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Synchronized
    fun findFreeCompetitors(): List<Competitor> = TODO()

    @Synchronized
    fun findStartableBouts(competitors: List<Competitor>): List<Bout> = TODO()

    @Synchronized
    fun markCompetitorOccupied(competitor: Competitor) {
        TODO()
    }

    @Synchronized
    fun markCompetitorFree(competitor: Competitor) {
        TODO()
    }
}
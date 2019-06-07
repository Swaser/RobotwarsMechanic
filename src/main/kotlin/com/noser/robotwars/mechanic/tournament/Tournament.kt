package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.Extensions.before
import com.noser.robotwars.mechanic.Extensions.forEach
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.bout.Player

class Tournament(competitors: Set<Competitor>,
                 val parameters: TournamentParameters,
                 private val asyncFactory: AsyncFactory) {

    private val openBouts: MutableList<Bout> =
        competitors
            .flatMap { c1 -> competitors.map { c2 -> Pair(c1, c2) } }
            .filter { (c1, c2) -> c1 != c2 }
            .map { (c1, c2) -> Bout(makePlayers(c1, c2), this) }
            .toMutableList()

    private val completedBouts: MutableList<Bout> = mutableListOf()

    private val notPlaying: MutableSet<Competitor> = competitors.toMutableSet()

    private val playing: MutableSet<Competitor> = mutableSetOf()

    /** call this fun repeatedly until tournament done */
    fun startNextRoundOfBouts() {

        (this::findStartableBouts before
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
    fun findStartableBouts(): List<Bout> {
        val res: MutableList<Bout> = mutableListOf()
        for ((c1, c2) in generatePairs(notPlaying)) {
            
        }
        TODO()
    }

    @Synchronized
    fun markCompetitorOccupied(competitor: Competitor) {
        TODO()
    }

    @Synchronized
    fun markCompetitorFree(competitor: Competitor) {
        TODO()
    }

    companion object {

        private fun makePlayers(c1: Competitor, c2: Competitor): (Player) -> Competitor {
            return {
                when (it) {
                    Player.YELLOW -> c1
                    Player.BLUE -> c2
                }
            }
        }

        private fun <X> generatePairs(xs : Iterable<X>) : Sequence<Pair<X,X>> {

            val xList = xs.toList()
            if (xList.size < 2) return emptySequence()

            return xs
                .asSequence()
                .flatMap { x -> xs.asSequence().map { y -> Pair(x,y) } }
                .filter { (x,y) -> x != y }
        }

    }
}
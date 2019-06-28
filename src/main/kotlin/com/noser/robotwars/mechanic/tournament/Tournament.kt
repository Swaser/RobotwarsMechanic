package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.bout.Player

class Tournament(asyncFactory: AsyncFactory,
                 competitors: Set<Competitor>,
                 val parameters: TournamentParameters) {

    private val openBouts: MutableSet<Bout> =
        generatePairs(competitors.asSequence())
            .map { (c1, c2) -> Bout(asyncFactory, makePlayers(c1, c2), this) }
            .toMutableSet()

    private val runningBouts = mutableSetOf<Bout>()

    private val completedBouts = mutableSetOf<Bout>()

    private val notPlaying = competitors.toMutableSet()

    fun getStatistics(): TournamentStatistics = TODO()

    /** call this fun repeatedly until tournament done */
    fun startNextRoundOfBouts(asyncFactory: AsyncFactory) {

        asyncFactory.supplyOne {
            findStartableBouts().forEach { startBout(it) }
        }
    }

    @Synchronized
    fun findStartableBouts(): List<Bout> {

        val res: MutableList<Bout> = mutableListOf()
        val players = notPlaying.toMutableSet()
        for (openBout in openBouts) {
            if (players.size < 2) continue
            if (players.containsAll(openBout.competitors)) {
                res.add(openBout)
                players.removeAll(openBout.competitors)
            }
        }

        return res
    }

    @Synchronized
    private fun startBout(bout: Bout) {
        registerBoutStarted(bout)
        bout.conductBout()
            .subscribe({ /* ignore onNext() */ },
                       { /* TODO onError */ },
                       { registerBoutEnded(bout) })
    }

    @Synchronized
    private fun registerBoutStarted(bout: Bout) {

        markCompetitorPlaying(bout.competitors)

        runningBouts.add(bout)
        openBouts.remove(bout)
    }

    @Synchronized
    private fun registerBoutEnded(bout: Bout) {

        markCompetitorNotPlaying(bout.competitors)

        runningBouts.remove(bout)
        completedBouts.add(bout)

        updateStatistics(bout)
    }

    private fun updateStatistics(bout: Bout) {
        check(bout.state.winner() != null)

        // TODO update statistics so it can easily be displayed
    }

    @Synchronized
    fun markCompetitorPlaying(competitors: Collection<Competitor>) {

        check(notPlaying.containsAll(competitors))

        notPlaying.removeAll(competitors)
    }

    @Synchronized
    fun markCompetitorNotPlaying(competitors: Collection<Competitor>) {

        check(competitors.none { notPlaying.contains(it) })

        notPlaying.addAll(competitors)
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

        private fun <X> generatePairs(xs: Sequence<X>) = xs
            .flatMap { x -> xs.map { y -> Pair(x, y) } }
            .filter { (x, y) -> x != y }
    }
}
package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.bout.Bout

class Tournament(asyncFactory: AsyncFactory,
                 competitors: Set<Competitor>,
                 val parameters: TournamentParameters) {

    private val openBouts: MutableSet<Bout> =
        generatePairs(competitors.asSequence())
            .map { (c1, c2) -> Bout(asyncFactory, listOf(c1, c2), this) }
            .toMutableSet()

    private val runningBouts = mutableSetOf<Bout>()

    private val completedBouts = mutableSetOf<Bout>()

    private val notPlaying = competitors.toMutableSet()

    fun getStatistics(): TournamentStatistics = TODO()

    /** call this fun repeatedly until tournament done */
    fun startNextRoundOfBouts(asyncFactory: AsyncFactory) {

        asyncFactory.later {
            findStartableBouts().forEach { startBout(it) }
        }
    }

    @Synchronized
    fun findStartableBouts(): List<Bout> {

        val res: MutableList<Bout> = mutableListOf()
        val toAssign = notPlaying.toMutableSet()
        for (openBout in openBouts) {
            if (toAssign.size < 2) continue
            if (toAssign.containsAll(openBout.competitors)) {
                res.add(openBout)
                toAssign.removeAll(openBout.competitors)
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

        check(bout.arena.winner != null)

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

        private fun <X> generatePairs(xs: Sequence<X>) = xs
            .flatMap { x -> xs.map { y -> Pair(x, y) } }
            .filter { (x, y) -> x != y }
    }
}
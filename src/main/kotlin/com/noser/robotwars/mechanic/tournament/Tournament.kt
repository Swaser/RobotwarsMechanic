package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.bout.Bout
import java.util.*

class Tournament(val tournamentName: String,
                 val competitors: Set<Competitor>,
                 boutGenerator: (Set<Competitor>) -> Set<Bout>) {

    val uuid: UUID = UUID.randomUUID()

    private val openBouts = boutGenerator(competitors).toMutableSet()

    private val runningBouts = mutableSetOf<Bout>()

    private val completedBouts = mutableSetOf<Bout>()

    private val notPlaying = mutableSetOf<Competitor>()

    fun getStatistics(): TournamentStatistics = TODO()

    /** call this fun repeatedly until tournament done */
    fun startNextRoundOfBouts(asyncFactory: AsyncFactory) {

        asyncFactory.supplyAsync {
            notPlaying.addAll(competitors)
            findStartableBouts().forEach { startBout(it, asyncFactory) }
        }
    }

    @Synchronized
    fun findStartableBouts(): List<Bout> {

        val startableBouts = mutableListOf<Bout>()
        val idleCompetitors = notPlaying.toMutableSet()
        for (openBout in openBouts) {
            if (idleCompetitors.size < 2) break // less than 2 competitors not playing anymore, stop searching for playable bouts
            if (idleCompetitors.containsAll(openBout.competitors)) {
                startableBouts.add(openBout)
                idleCompetitors.removeAll(openBout.competitors)
            }
        }

        return startableBouts
    }

    @Synchronized
    private fun startBout(bout: Bout, asyncFactory: AsyncFactory) {
        registerBoutStarted(bout)
        bout.conductBout(asyncFactory)
            .finally { _, throwable ->
                if (throwable == null) {
                    registerBoutEnded(bout)
                } else {
                    // TODO put it into some intermediate state
                    // TODO allow the reason to be analyzed and the bout to be retried OR the bout to be resolved manually
                }
            }
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
        check(bout.arena.hasAWinner())

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

    fun getAllBouts(): List<Bout> {
        return openBouts.union(runningBouts).union(completedBouts).toList()
    }
}
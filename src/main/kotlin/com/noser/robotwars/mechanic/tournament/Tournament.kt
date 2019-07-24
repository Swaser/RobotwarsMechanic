package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.bout.Arena
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.bout.BoutState
import com.noser.robotwars.mechanic.tournament.TournamentState.OPEN
import com.noser.robotwars.mechanic.tournament.TournamentState.STARTED
import java.util.*
import java.util.concurrent.Flow

class Tournament(private val asyncFactory: AsyncFactory,
                 val tournamentName: String,
                 private val boutGenerator: (Set<Competitor>) -> Set<Bout>) {

    val uuid: UUID = UUID.randomUUID()

    @Volatile
    var state: TournamentState = OPEN
        private set

    val competitors = mutableSetOf<Competitor>()

    private val openBouts = mutableSetOf<Bout>()

    private val runningBouts = mutableSetOf<Bout>()

    private val completedBouts = mutableSetOf<Bout>()

    private val notPlaying = mutableSetOf<Competitor>()

    private val subject = asyncFactory.subject<Bout>()

    private fun observe(): Flow.Processor<Bout, Bout> = subject

    fun getStatistics(): TournamentStatistics = TODO()

    /** call this fun repeatedly until tournament done */
    fun startNextRoundOfBouts() {
        val findStartableBouts = findStartableBouts()
        if (findStartableBouts.isEmpty()) {
            updateTournamentState(TournamentState.FINISHED)
        } else {
            findStartableBouts.forEach { startBout(it) }
        }
    }

    private fun updateTournamentState(tournamentState: TournamentState) {
        state = tournamentState
        notifyTournamentUpdated(competitors.toList(), this)
    }

    fun start(): Flow.Processor<Bout, Bout> {
        check(state == OPEN)
        check(competitors.size >= 2)

        notPlaying.addAll(competitors)
        openBouts.addAll(boutGenerator(competitors))

        updateTournamentState(STARTED)

        startNextRoundOfBouts()

        return observe()
    }

    fun isOpen(): Boolean {
        return state == OPEN
    }

    fun isStarted(): Boolean {
        return state == STARTED
    }

    fun addCompetitor(competitor: Competitor) {
        check(state == OPEN)
        competitors.add(competitor)
    }

    @Synchronized
    private fun findStartableBouts(): List<Bout> {

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
    private fun startBout(bout: Bout) {

        registerBoutStarted(bout)

        bout.conductBout()
            .subscribe(object : Flow.Subscriber<Pair<BoutState, Detailed<Arena>>> {
                override fun onComplete() {
                    registerBoutEnded(bout)
                }

                override fun onSubscribe(subscription: Flow.Subscription) {
                    subscription.request(Long.MAX_VALUE)
                }

                override fun onNext(item: Pair<BoutState, Detailed<Arena>>) = Unit
                override fun onError(throwable: Throwable?) = Unit // TODO
            })
    }

    @Synchronized
    private fun registerBoutStarted(bout: Bout) {

        markCompetitorPlaying(bout.competitors)

        runningBouts.add(bout)
        openBouts.remove(bout)

        notifyBoutUpdated(bout.competitors, bout)
    }

    @Synchronized
    private fun registerBoutEnded(bout: Bout) {

        markCompetitorNotPlaying(bout.competitors)

        runningBouts.remove(bout)
        completedBouts.add(bout)

        notifyBoutUpdated(bout.competitors, bout)

        updateStatistics(bout)

        if(runningBouts.isNotEmpty()) {
            startNextRoundOfBouts()
            subject.onNext(bout)
        } else {
            subject.onComplete()
        }
    }

    private fun notifyBoutUpdated(competitors: List<Competitor>,
                                  bout: Bout) {
        competitors.forEach { it.notify(bout) }

        //TODO also notify spectators spectators[boutUuid]?.forEach { it.notify(bout) }
    }

    private fun notifyTournamentUpdated(competitors: List<Competitor>,
                                        tournament: Tournament) {
        competitors.forEach { it.notify(tournament) }

        //TODO also notify spectators spectators[tournamentUuid]?.forEach { it.notify(tournament) }
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

    fun getAllBouts(): List<Bout> {
        return openBouts.union(runningBouts).union(completedBouts).toList()
    }

    fun hasCompleted(): Boolean {
        return openBouts.isEmpty()
    }
}
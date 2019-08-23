package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.tournament.TournamentState.FINISHED
import com.noser.robotwars.mechanic.tournament.TournamentState.OPEN
import com.noser.robotwars.mechanic.tournament.TournamentState.STARTED
import java.util.*

class Tournament(
    private val asyncFactory: AsyncFactory,
    val name: String,
    val parameters: TournamentParameters,
    private val listener: TournamentChangeListener,
    private val pairingsGenerator: (Set<Competitor>) -> Set<List<Competitor>>
) {

    val uuid: UUID = UUID.randomUUID()

    @Volatile
    var state: TournamentState = OPEN
        private set

    private val statistics: TournamentStatistics = TournamentStatistics()

    private val competitors = mutableSetOf<Competitor>()

    private val openBouts = mutableSetOf<Bout>()

    private val runningBouts = mutableSetOf<Bout>()

    private val completedBouts = mutableSetOf<Bout>()

    private val subject = asyncFactory.subject<Tournament>()

    fun observe() = subject

    fun getStatistics(): TournamentStatistics = statistics

    fun addCompetitor(competitor: Competitor) {
        check(state == OPEN)
        competitors.add(competitor)
        subject.onNext(this)
    }

    fun getAllCompetitors(): List<Competitor> {
        return competitors.toList()
    }

    fun getAllBouts(): List<Bout> {
        return openBouts.union(runningBouts)
            .union(completedBouts)
            .toList()
    }

    fun start() {
        check(state == OPEN)
        check(competitors.size >= 2)

        updateTournamentState(STARTED)

        openBouts.addAll(generateBouts(competitors))

        startNextRoundOfBouts()
    }

    fun isOpen(): Boolean {
        return state == OPEN
    }

    fun isStarted(): Boolean {
        return state == STARTED
    }

    fun hasCompleted(): Boolean {
        return state == FINISHED
    }

    private fun updateTournamentState(tournamentState: TournamentState) {
        state = tournamentState
    }

    private fun generateBouts(competitors: MutableSet<Competitor>): Collection<Bout> {
        return pairingsGenerator(competitors).map { competitorList ->
            Bout(
                asyncFactory, createClones(competitorList), parameters
            )
        }
    }

    private fun createClones(competitorList: List<Competitor>) = competitorList.map { it.copy() }

    /** call this fun repeatedly until tournament done */
    private fun startNextRoundOfBouts() {
        val findStartableBouts = findStartableBouts()
        if (findStartableBouts.isNotEmpty()) {
            findStartableBouts.forEach { startBout(it) }
        }
    }

    @Synchronized
    private fun findStartableBouts(): List<Bout> {

        val startableBouts = mutableListOf<Bout>()
        val idleCompetitors = competitors.subtract(runningBouts.flatMap { it.competitors })
            .toMutableSet()

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
            .subscribe(
                AsyncFactory.noBackpressureSubscriber(onNext = {
                    notifyBoutChanged(it.first)
                    it.second.forEach(::println)
                }, onComplete = {
                    getAllBouts().find { it.uuid == bout.uuid }?.let {
                        registerBoutEnded(it)
                        notifyBoutChanged(it)
                    }
                })
            )
    }

    @Synchronized
    private fun registerBoutStarted(bout: Bout) {

        runningBouts.add(bout)
        openBouts.remove(bout)

        subject.onNext(this)
    }

    @Synchronized
    private fun registerBoutEnded(bout: Bout) {

        runningBouts.remove(bout)
        completedBouts.add(bout)

        updateStatistics(bout)

        subject.onNext(this)

        when {
            openBouts.isNotEmpty() -> startNextRoundOfBouts()
            runningBouts.isNotEmpty() -> Unit
            else -> registerTournamentEnded()
        }
    }

    private fun registerTournamentEnded() {
        updateTournamentState(FINISHED)
        subject.onComplete()
    }

    private fun notifyBoutChanged(bout: Bout) {
        listener.notifyBoutChanged(bout)
    }

    private fun updateStatistics(bout: Bout) {
        check(bout.arena.winner != null)
        statistics.addNewResult(bout)
    }

    fun finish() {
        openBouts.forEach { it.finish() }
        runningBouts.forEach { it.finish() }
    }
}
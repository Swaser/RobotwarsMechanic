package com.noser.robotwars.mechanic.tournament

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.bout.Bout
import com.noser.robotwars.mechanic.tournament.TournamentState.FINISHED
import com.noser.robotwars.mechanic.tournament.TournamentState.OPEN
import com.noser.robotwars.mechanic.tournament.TournamentState.STARTED
import java.util.*
import java.util.concurrent.Flow

class Tournament(private val asyncFactory: AsyncFactory,
                 val name: String,
                 val parameters: TournamentParameters,
                 private val listener: TournamentChangeListener,
                 private val boutGenerator: (Set<Competitor>) -> Set<List<Competitor>>) {

    val uuid: UUID = UUID.randomUUID()

    @Volatile
    var state: TournamentState = OPEN
        private set

    private val statistics: TournamentStatistics = TournamentStatistics()

    private val competitors = mutableSetOf<Competitor>()

    private val openBouts = mutableSetOf<Bout>()

    private val runningBouts = mutableSetOf<Bout>()

    private val completedBouts = mutableSetOf<Bout>()

    private val notPlaying = mutableSetOf<Competitor>()

    private val subject = asyncFactory.subject<Bout>()

    private fun observe(): Flow.Processor<Bout, Bout> = subject

    fun getStatistics(): TournamentStatistics = statistics

    /** call this fun repeatedly until tournament done */
    fun startNextRoundOfBouts() {
        asyncFactory.later {
            val findStartableBouts = findStartableBouts()
            if (findStartableBouts.isNotEmpty()) {
                findStartableBouts.forEach { startBout(it) }
            }
        }.subscribe(AsyncFactory.noBackpressureSubscriber({},{},{},{}))
    }

    private fun updateTournamentState(tournamentState: TournamentState) {
        state = tournamentState
        notifyTournamentUpdated(this)
    }

    fun start(): Flow.Processor<Bout, Bout> {
        check(state == OPEN)
        check(competitors.size >= 2)

        notPlaying.addAll(competitors)
        openBouts.addAll(generateBouts(competitors))

        updateTournamentState(STARTED)

        startNextRoundOfBouts()

        return observe()
    }

    private fun generateBouts(competitors: MutableSet<Competitor>): Collection<Bout> {
        return boutGenerator(competitors).map { Bout(asyncFactory, it, parameters) }
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

    fun addCompetitor(competitor: Competitor) {
        check(state == OPEN)
        competitors.add(competitor)
        notifyTournamentUpdated(this)
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

        bout.conductBout().subscribe(AsyncFactory.noBackpressureSubscriber({}, {registerBoutEnded(bout)}, {}, {}))
    }

    @Synchronized
    private fun registerBoutStarted(bout: Bout) {

        markCompetitorPlaying(bout.competitors)

        runningBouts.add(bout)
        openBouts.remove(bout)

        notifyBoutUpdated(bout)
    }

    @Synchronized
    private fun registerBoutEnded(bout: Bout) {

        markCompetitorNotPlaying(bout.competitors)

        runningBouts.remove(bout)
        completedBouts.add(bout)

        updateStatistics(bout)

        notifyBoutUpdated(bout)

        if(openBouts.union(runningBouts).isNotEmpty()) {
            startNextRoundOfBouts()
            subject.onNext(bout)
        } else {
            updateTournamentState(FINISHED)
            subject.onComplete()
        }
    }

    private fun notifyBoutUpdated(bout: Bout) {
        listener.notifyBoutChanged(bout)
    }

    private fun notifyTournamentUpdated(tournament: Tournament) {
        listener.notifyTournamentChanged(tournament)
    }

    private fun updateStatistics(bout: Bout) {
        check(bout.arena.winner != null)
        statistics.addNewResult(bout)
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

    fun getCompetitors(): List<Competitor> {
        return competitors.toList()
    }
}
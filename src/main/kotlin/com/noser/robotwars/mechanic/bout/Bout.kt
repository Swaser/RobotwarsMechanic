package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.none
import com.noser.robotwars.mechanic.Detailed.Companion.single
import com.noser.robotwars.mechanic.bout.Moves.applyMove
import com.noser.robotwars.mechanic.tournament.Competitor
import com.noser.robotwars.mechanic.tournament.TournamentParameters
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.UUID
import java.util.concurrent.Flow
import kotlin.random.Random

/**
 * The bout gets its own unique uuid so it can easily identified
 */
class Bout(private val asyncFactory: AsyncFactory,
           val competitors: List<Competitor>,
           val parameters: TournamentParameters) {

    init {
        check(competitors.size >= 2) { "Bout must have at least 2 competitors" }
    }

    val uuid: UUID = UUID.randomUUID()

    private var pendingResponse = PendingResponse.none()

    @Volatile
    var state: BoutState = BoutState.REGISTERED
        private set

    var winner: Competitor? = null
        private set

    private val subject = asyncFactory.subject<Pair<BoutState, Detailed<Arena>>>()

    @Volatile
    lateinit var arena: Arena
        private set

    val deathnote: MutableList<Int> = mutableListOf()

    fun getArenaOrNull(): Arena? {
        return if (::arena.isInitialized) arena else null
    }

    fun conductBout(): Flow.Publisher<Pair<BoutState, Detailed<Arena>>> {
        conductBoutRecursive()
        return subject
    }

    private val stillRunningObserver: Flow.Subscriber<Bout> = AsyncFactory.noBackpressureSubscriber(
        onNext = {
            if (deathnote.isNotEmpty()) {
                executeDeathnote()
            } else {
                conductBoutRecursive()
            }
        },
        onComplete = {},
        onError = { subject.onError(it) },
        onSubscribe = {}
    )

    private val moveRequestPendingObserver: Observer<Move> = object : Observer<Move> {
        override fun onComplete() {}

        override fun onSubscribe(subscription: Disposable) {}

        override fun onNext(move: Move) {
            applyMoveResponse(move)

            if (deathnote.isNotEmpty()) {
                executeDeathnote()
            } else {
                conductBoutRecursive()
            }
        }

        override fun onError(e: Throwable) {}
    }

    private val resolvedObserver: Flow.Subscriber<Bout> = AsyncFactory.noBackpressureSubscriber(
        onNext = {},
        onComplete = { subject.onComplete() },
        onError = { subject.onError(it) },
        onSubscribe = {}
    )

    private fun conductBoutRecursive() {

        when (state) {

            BoutState.REGISTERED -> asyncFactory
                .later { start(parameters) }
                .subscribe(stillRunningObserver)

            BoutState.STARTED -> nextMove().subscribe(moveRequestPendingObserver)

            else -> {
                winner = arena.winner?.let { competitors[it] }
                val winner = checkNotNull(winner)
                asyncFactory
                    .later {
                        competitors.forEach {
                            try {
                                it.publishResult(arena, winner)
                            } catch (e: Exception) {
                                // TODO what to do here
                            }
                        }
                        this@Bout
                    }
                    .subscribe(resolvedObserver)
            }
        }
    }

    fun executeDeathnote() {
        asyncFactory
            .later {
                deathnote.forEach { player ->
                    val detailedAfterDeathnote =
                        single(arena) {"Tick off player $it from deathnote"}
                        .flatMap { arena.killRobot(player) }
                    subject.onNext(Pair(state, detailedAfterDeathnote))
                }
                this
            }.subscribe(stillRunningObserver)
    }

    private fun start(parameters: TournamentParameters): Bout {

        val random = Random(System.currentTimeMillis())
        val terrain = createFreshTerrain(parameters, random)
        val effects = createEffects(parameters, terrain, random)
        val robots = (0 until competitors.size)
            .fold<Int, MutableList<Robot>>(mutableListOf()) { list, player ->
                    val robot = Robot(player,
                                      createUniquePosition(parameters.bounds, random, list.map(Robot::position), terrain, effects),
                                      parameters.robotEnergyInitial,
                                      parameters.robotEnergyMax,
                                      parameters.robotHealthInitial,
                                      parameters.robotShieldInitial,
                                      parameters.robotShieldMax)
                    list.add(robot)
                    list
        }

        arena = Arena(0, robots, parameters.bounds, terrain, effects)

        state = BoutState.STARTED
        subject.onNext(Pair(state, none(arena)))
        return this
    }

    private fun createUniquePosition(bounds: Bounds,
                                     random: Random,
                                     robots: List<Position>,
                                     terrain: Grid<Terrain>,
                                     effects: Grid<Effect>): Position {

        val possiblePositions = bounds.positions
            .filter { !robots.contains(it) }
            .filter { terrain[it] == Terrain.GREEN }
            .filter { effects[it] == Effect.none() }

        if (possiblePositions.isEmpty()) throw IllegalStateException("Not enough free space for robots to place them all")

        return possiblePositions.random(random)
    }

    private fun nextMove(): PublishSubject<Move> {

        val detailedBeforeMove = arena.addEnergyTo(arena.activePlayer, parameters.energyRefill)

        arena = detailedBeforeMove.value

        pendingResponse = PendingResponse.new()

        val moveRequest = createMoveRequest(pendingResponse.uuid.toString(), uuid.toString(), arena)

        requestMove(moveRequest)

        return pendingResponse.subject
    }

    fun moveResponse(move: Move) {
        val expectedRequestUuid = pendingResponse.uuid.toString()
        when {
            move.requestId != expectedRequestUuid -> single(arena) { "Received answer for invalid move request" }
            else -> pendingResponse.moveResponseReceived(move)
        }
    }

    private fun applyMoveResponse(move: Move) {
        val detailedAfterMove = none(arena)
            .flatMap { applyMove(move)(it) }
            .map { it.nextPlayer() }

        arena = detailedAfterMove.value

        arena.winner?.also {
            state = BoutState.FINISHED
        }

        subject.onNext(Pair(state, detailedAfterMove))
    }

    private fun createMoveRequest(currentMoveRequestUuid: String,
                                  boutUuid: String,
                                  anArena: Arena): MoveRequest {
        return MoveRequest(currentMoveRequestUuid,
                           boutUuid,
                           anArena,
                           competitors.map { competitors.indexOf(it) to it }.toMap())
    }

    private fun requestMove(request: MoveRequest) {
        competitors[request.arena.activePlayer].nextMove(request)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bout

        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    fun kill(uuid: String) {
        val competitor = competitors.firstOrNull { it.uuid.toString() == uuid }
        if(competitor != null) {
            deathnote.add(competitors.indexOf(competitor))
            competitor.kill()
        }
    }

    companion object {

        private fun createFreshTerrain(parameters: TournamentParameters, random: Random): Grid<Terrain> {
            return Grid(parameters.bounds) {
                val rnd = random.nextDouble()
                when {
                    rnd < parameters.terrainWaterChance -> Terrain.WATER
                    rnd < parameters.terrainRockChance -> Terrain.ROCK
                    else -> Terrain.GREEN
                }
            }
        }

        private fun createEffects(parameters: TournamentParameters, terrain: Grid<Terrain>, random: Random): Grid<Effect> {
            return terrain.mapAll { _, aTerrain ->
                if (aTerrain == Terrain.GREEN && random.nextDouble() < parameters.effectBurnableChance)
                    Effect.burnable()
                else if (aTerrain != Terrain.ROCK && random.nextDouble() < parameters.effectEnergyChance)
                    Effect.energy(random.nextInt(parameters.effectEnergyMax) + 1)
                else
                    Effect.none()
            }
        }
    }
}
package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.none
import com.noser.robotwars.mechanic.Detailed.Companion.single
import com.noser.robotwars.mechanic.bout.Moves.applyMove
import com.noser.robotwars.mechanic.tournament.Competitor
import com.noser.robotwars.mechanic.tournament.TournamentParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Flow
import kotlin.random.Random

/**
 * The bout gets its own unique uuid so it can easily be identified
 */
class Bout(private val asyncFactory: AsyncFactory,
           val competitors: List<Competitor>,
           val parameters: TournamentParameters) {

    private val logger: Logger = LoggerFactory.getLogger("Bout")

    init {
        check(competitors.size >= 2) { "Bout must have at least 2 competitors" }
    }

    val uuid: UUID = UUID.randomUUID()

    @Volatile
    var state: BoutState = BoutState.REGISTERED
        private set

    var winner: Competitor? = null
        private set

    @Volatile
    lateinit var arena: Arena
        private set

    private val subject = asyncFactory.subject<Pair<BoutState, Detailed<Arena>>>()

    private var pendingMoveRequest: PendingMoveRequest? = null

    private val deathnote: MutableList<Int> = mutableListOf()

    fun getArenaOrNull(): Arena? {
        return if (::arena.isInitialized) arena else null
    }

    fun conductBout(): Flow.Publisher<Pair<BoutState, Detailed<Arena>>> {
        conductBoutRecursive()
        return subject
    }

    private val startedObserver: Flow.Subscriber<Bout> = AsyncFactory.noBackpressureSubscriber(
        onNext = { doNextStep() },
        onComplete = {},
        onError = { subject.onError(it) },
        onSubscribe = {}
    )

    private val moveRequestPendingObserver: Flow.Subscriber<Pair<Detailed<Arena>, Move>> = AsyncFactory.noBackpressureSubscriber(
        onNext = {
            applyMoveResponse(it.first, it.second)
            doNextStep()
        },
        onComplete = {},
        onError = { subject.onError(it) },
        onSubscribe = {}
    )

    private val finishedObserver: Flow.Subscriber<Bout> = AsyncFactory.noBackpressureSubscriber(
        onNext = {},
        onComplete = { subject.onComplete() },
        onError = { subject.onError(it) },
        onSubscribe = {}
    )

    private fun conductBoutRecursive() = when (state) {

        BoutState.REGISTERED -> asyncFactory
            .later { start(parameters) }
            .subscribe(startedObserver)

        BoutState.STARTED -> sendMoveRequest()
            .subscribe(moveRequestPendingObserver)

        BoutState.FINISHED -> {
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
                .subscribe(finishedObserver)
        }
    }

    private fun doNextStep() {
        if (deathnote.isNotEmpty()) {
            executeDeathnote()
        } else {
            conductBoutRecursive()
        }
    }

    private fun executeDeathnote() {
        asyncFactory
            .later {
                deathnote.forEach { player ->
                    val detailedAfterDeathnote =
                        single(arena) {"Tick off player $player from deathnote"}
                        .flatMap { arena.killRobot(player) }
                    arena = detailedAfterDeathnote.value
                    subject.onNext(Pair(state, detailedAfterDeathnote))
                }
                deathnote.clear()
                this@Bout
            }.subscribe(startedObserver)
    }

    private fun getSeed(parameters: TournamentParameters): Long {
        val seed = parameters.randomSeed ?: System.currentTimeMillis()
        logger.debug("Just for the record, using seed $seed")
        return seed
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

    private fun start(parameters: TournamentParameters): Bout {
        val random = Random(getSeed(parameters))
        parameters.randomSeed = random.nextLong()
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

        subject.onNext(Pair(state, single(arena){ "Bout ($uuid) has started" }))
        return this
    }

    private fun sendMoveRequest(): Flow.Publisher<Pair<Detailed<Arena>, Move>> {

        // refill energy of active player
        val detailedBeforeMove = arena.addEnergyTo(arena.activePlayer, parameters.energyRefill)

        // remember the current state
        val pendingRequest = PendingMoveRequest(detailedBeforeMove, asyncFactory)
        pendingMoveRequest = pendingRequest

        // create and send move request
        val request = MoveRequest(pendingRequest.uuid.toString(),
                                  uuid.toString(),
                                  pendingRequest.detailedArena.value,
                                  competitors.map { competitors.indexOf(it) to it }.toMap())
        competitors[request.arena.activePlayer].nextMove(request)

        // give them something to listen to
        return pendingRequest.subject
    }

    fun receiveMoveResponse(move: Move) {
        val pendingRequest = pendingMoveRequest
        when {
            pendingRequest == null -> logger.warn("Received answer without pending move request: $move")
            move.requestId != pendingRequest.uuid.toString() -> logger.warn("Received answer for invalid move request: $move")
            else -> {
                pendingMoveRequest = null
                pendingRequest.moveResponseReceived(move)
            }
        }
    }

    private fun applyMoveResponse(detailedBeforeMove: Detailed<Arena>, move: Move) {
        val detailedAfterMove = detailedBeforeMove
            .flatMap { applyMove(move)(it) }
            .map { it.nextPlayer() }

        arena = detailedAfterMove.value

        arena.winner?.also {
            state = BoutState.FINISHED
        }

        subject.onNext(Pair(state, detailedAfterMove))
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
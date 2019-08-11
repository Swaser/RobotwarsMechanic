package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.single
import com.noser.robotwars.mechanic.bout.Moves.applyMove
import com.noser.robotwars.mechanic.tournament.Competitor
import com.noser.robotwars.mechanic.tournament.TournamentParameters
import java.util.*
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

    private val stillRunningObserver = object : Flow.Subscriber<Bout> {
        override fun onSubscribe(subscription: Flow.Subscription) {
            subscription.request(Long.MAX_VALUE)
        }

        override fun onComplete() {}
        override fun onError(throwable: Throwable) = subject.onError(throwable)
        override fun onNext(bout: Bout) {
            if(deathnote.isNotEmpty()) {
                executeDeathnote()
            } else {
                conductBoutRecursive()
            }
        }
    }

    private val resolvedObserver = object : Flow.Subscriber<Bout> {

        override fun onNext(item: Bout) {}

        override fun onSubscribe(subscription: Flow.Subscription) {
            subscription.request(Long.MAX_VALUE)
        }

        override fun onComplete() = subject.onComplete()
        override fun onError(throwable: Throwable) = subject.onError(throwable)
    }

    private fun conductBoutRecursive() {

        when (state) {

            BoutState.REGISTERED -> asyncFactory
                .later { start(parameters) }
                .subscribe(stillRunningObserver)

            BoutState.STARTED -> asyncFactory
                .later { nextMove() }
                .subscribe(stillRunningObserver)

            else -> {
                winner = arena.winner?.let{ competitors[it] }
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
        subject.onNext(Pair(state, Detailed.none(arena)))
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

        if(possiblePositions.isEmpty()) throw IllegalStateException("Not enough free space for robots to place them all")

        return possiblePositions.random(random)
    }

    private fun nextMove(): Bout {

        val detailedAfterMove = arena
            .addEnergyTo(arena.activePlayer, parameters.energyRefill)
            .flatMap { anArena ->
                val currentMoveRequestId = UUID.randomUUID()
                val aMove = getMove(createMoveRequest(currentMoveRequestId, anArena))
                when {
                    aMove == null -> anArena.killRobot(anArena.activePlayer)
                    aMove.requestId != currentMoveRequestId.toString() -> {
                        single(anArena){"Received answer for invalid move request"}
                    }
                    else -> applyMove(aMove)(anArena)
                }
            }
            .map { it.nextPlayer() }

        arena = detailedAfterMove.value

        arena.winner
            ?.also {
                state = BoutState.FINISHED
            }

        subject.onNext(Pair(state, detailedAfterMove))

        return this
    }

    private fun createMoveRequest(currentMoveRequestId: UUID, anArena: Arena): MoveRequest {
        return MoveRequest(currentMoveRequestId.toString(),
                           anArena,
                           competitors.map { competitors.indexOf(it) to it }.toMap())
    }

    private fun getMove(request: MoveRequest): Move? {
        return competitors[request.arena.activePlayer]
            .nextMove(request)
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

    fun harakiri(uuid: String) {
        val competitor = competitors.first { it.uuid.toString() == uuid }
        val player = competitors.indexOf(competitor)
        deathnote.add(player)
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
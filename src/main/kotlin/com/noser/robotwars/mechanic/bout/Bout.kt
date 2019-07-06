package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Async
import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.AsyncListener
import com.noser.robotwars.mechanic.bout.BoutState.FINISHED
import com.noser.robotwars.mechanic.bout.BoutState.REGISTERED
import com.noser.robotwars.mechanic.bout.BoutState.STARTED
import com.noser.robotwars.mechanic.bout.Moves.applyMove
import com.noser.robotwars.mechanic.tournament.Competitor
import com.noser.robotwars.mechanic.tournament.TournamentParameters
import java.lang.IllegalStateException
import java.util.*
import kotlin.random.Random

/**
 * The bout gets its own unique id so it can easily be identified
 */
class Bout(private val asyncFactory: AsyncFactory,
           val competitors: List<Competitor>,
           val tournamentParameters: TournamentParameters) {

    val uuid: UUID = UUID.randomUUID()

    @Volatile
    var boutState: BoutState = REGISTERED
        private set

    @Volatile
    lateinit var arena: Arena
        private set

    fun getArenaOrNull(): Arena? {
        return if (::arena.isInitialized) arena else null
    }

    private val subject = asyncFactory.subject<Bout>()

    private fun observe(): Async<Bout> = subject

    fun conductBout(): Async<Bout> {
        conductBoutRecursive()
        return observe()
    }

    private val stillRunningObserver = object : AsyncListener<Bout> {
        override fun onComplete() {}
        override fun onError(throwable: Throwable) = subject.onError(throwable)
        override fun onNext(element: Bout) {
            subject.onNext(element)
            conductBoutRecursive()
        }
    }

    private val resolvedObserver = object : AsyncListener<Bout> {
        override fun onComplete() = subject.onComplete()
        override fun onNext(element: Bout) = subject.onNext(element)
        override fun onError(throwable: Throwable) = subject.onError(throwable)
    }

    private fun conductBoutRecursive() {

        when (boutState) {

            REGISTERED -> asyncFactory
                .later { start(tournamentParameters) }
                .subscribe(stillRunningObserver)

            STARTED    -> asyncFactory
                .later { nextMove() }
                .subscribe(stillRunningObserver)

            else                 -> {
                val winner = arena.getWinner()
                    asyncFactory
                    .later {
                        competitors.forEach {
                            it.publishResult(arena, winner)
                        }
                        this@Bout
                    }
                    .subscribe(resolvedObserver)
            }
        }
    }

    private fun start(parameters: TournamentParameters): Bout {

        val random = Random(System.currentTimeMillis())
        val terrain = createFreshTerrain(parameters, random)
        val effects = terrain.mapAll { _, aTerrain ->
            if (aTerrain == Terrain.GREEN && random.nextDouble() < parameters.chanceForBurnable) Effect.burnable()
            else if (aTerrain != Terrain.ROCK && random.nextDouble() < parameters.chanceForEnergy) Effect.energy(random.nextInt(
                parameters.maxEnergy - 1) + 1)
            else Effect.none()
        }
        val robots = competitors.fold<Competitor, MutableList<Robot>>(mutableListOf()) { list, competitor ->
            val robot = Robot(competitor,
                              createUniquePosition(parameters.bounds, random, list.map(Robot::position), terrain, effects),
                              parameters.startingEnergy,
                              parameters.maxEnergy,
                              parameters.startingHealth,
                              parameters.startingShield,
                              parameters.maxShield)
            list.add(robot)
            list
        }

        arena = Arena(competitors.first(), robots, parameters.bounds, terrain, effects)

        boutState = STARTED
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

        val move = arena.activeCompetitor.nextMove(arena)

        if (move == null) {
            val (afterMove, messages) = arena.harakiri(arena.activeCompetitor)
            arena = afterMove
            messages.forEach{ println(it) }
        } else {
            val (afterMove, messages) = applyMove(move)(arena)
            arena = afterMove
            messages.forEach{ println(it) }
        }

        arena = arena.advanceCompetitor()

        competitors.forEach { it.notify(this) }

        boutState = when {
            arena.hasAWinner() -> FINISHED
            else -> boutState
        }
        return this
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

    fun getActivecompetitorUuid(): String? {
        return if(::arena.isInitialized) arena.activeCompetitor.uuid.toString() else null
    }

    companion object {

        private fun createFreshTerrain(parameters: TournamentParameters, random: Random): Grid<Terrain> {
            return Grid(parameters.bounds) {
                val rnd = random.nextDouble()
                when {
                    rnd < parameters.chanceForWater -> Terrain.WATER
                    rnd < parameters.chanceForRock  -> Terrain.ROCK
                    else                            -> Terrain.GREEN
                }
            }
        }
    }
}
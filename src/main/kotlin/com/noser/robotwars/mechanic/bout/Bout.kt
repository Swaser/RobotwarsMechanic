package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Async
import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.bout.BoutState.FINISHED
import com.noser.robotwars.mechanic.bout.BoutState.REGISTERED
import com.noser.robotwars.mechanic.bout.BoutState.STARTED
import com.noser.robotwars.mechanic.bout.Moves.applyMove
import com.noser.robotwars.mechanic.tournament.Competitor
import com.noser.robotwars.mechanic.tournament.TournamentParameters
import java.util.*
import kotlin.random.Random

/**
 * The bout gets its own unique id so it can easily be identified
 */
class Bout(val competitors: List<Competitor>,
           val tournamentParameters: TournamentParameters) {

    val uuid: UUID = UUID.randomUUID()

    var activeCompetitor: Competitor = competitors.first()
        private set

    @Volatile
    var boutState: BoutState = REGISTERED
        private set

    @Volatile
    lateinit var arena: Arena
        private set

    fun getArenaOrNull(): Arena? {
        return if (::arena.isInitialized) arena else null
    }

    fun conductBout(asyncFactory: AsyncFactory): Async<Competitor?> {

        val res = asyncFactory.deferred<Competitor?>()

        fun go() {
            when (boutState) {

                REGISTERED ->
                    asyncFactory
                        .supplyAsync { start(tournamentParameters) }
                        .map { go() }
                        .finally { _, throwable -> if (throwable != null) res.exception(throwable) }

                STARTED ->
                    asyncFactory
                        .supplyAsync { nextMove() }
                        .map { go() }
                        .finally { _, throwable -> if (throwable != null) res.exception(throwable) }

                else -> {
                    val winner = arena.getWinner()
                    asyncFactory
                        .supplyAsync {
                            competitors.forEach {
                                it.publishResult(arena, winner)
                            }
                        }
                        .finally { _, _ -> res.done(winner) }
                }
            }
        }

        go()
        return res
    }

    private fun start(parameters: TournamentParameters) {

        val random = Random(System.currentTimeMillis())
        val terrain = createFreshTerrain(parameters, random)

        arena = Arena(
            competitors.toMutableList(),
            competitors
                .fold(mutableListOf()) { list, competitor ->
                    val robot = Robot(competitor,
                                      createUniquePosition(parameters.bounds, random, list.map(Robot::position)),
                                      parameters.startingEnergy,
                                      parameters.maxEnergy,
                                      parameters.startingHealth,
                                      parameters.startingShield,
                                      parameters.maxShield)
                    list.add(robot)
                    list
                },
            parameters.bounds,
            terrain,
            terrain.mapAll { _, aTerrain ->
                if (aTerrain == Terrain.GREEN && random.nextDouble() < parameters.chanceForBurnable)
                    Effect.burnable()
                else if (aTerrain != Terrain.ROCK && random.nextDouble() < parameters.chanceForEnergy)
                    Effect.energy(random.nextInt(parameters.maxEnergy - 1) + 1)
                else
                    Effect.none()
            }
        )

        boutState = STARTED
    }

    private fun createUniquePosition(bounds: Bounds,
                                     random: Random,
                                     existingPositions: List<Position>): Position {

        var pos: Position
        do {
            pos = bounds.random(random)
        } while (existingPositions.contains(pos))

        return pos
    }

    private fun nextMove() {

        val move = activeCompetitor.nextMove(arena)

        if (move == null) {
            val (afterMove, messages) = arena.harakiri(activeCompetitor)
            arena = afterMove
            messages.forEach{ println(it) }
        } else {
            val (afterMove, messages) = applyMove(move)(arena)
            arena = afterMove
            messages.forEach{ println(it) }
        }

        activeCompetitor = getNextCompetitor()

        boutState = when {
            arena.hasAWinner() -> FINISHED
            else -> boutState
        }
    }

    private fun getNextCompetitor(): Competitor {
        val remainingCompetitors = competitors.filter { activeCompetitor == it || arena.findRobot(it).health > 0 }
        val nextIndex = remainingCompetitors
            .indexOf(activeCompetitor)
            .inc()
            .rem(remainingCompetitors.size)
        return remainingCompetitors[nextIndex]
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

    companion object {

        private fun createFreshTerrain(parameters: TournamentParameters, random: Random): Grid<Terrain> {
            return Grid(parameters.bounds) {
                val rnd = random.nextDouble()
                when {
                    rnd < parameters.chanceForWater -> Terrain.WATER
                    rnd < parameters.chanceForRock -> Terrain.ROCK
                    else -> Terrain.GREEN
                }
            }
        }
    }
}
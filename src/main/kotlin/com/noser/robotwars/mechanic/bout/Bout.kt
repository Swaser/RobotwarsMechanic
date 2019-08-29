package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.bout.Moves.applyMove
import com.noser.robotwars.mechanic.tournament.Competitor
import com.noser.robotwars.mechanic.tournament.Tournament
import com.noser.robotwars.mechanic.tournament.TournamentParameters
import java.util.*
import java.util.concurrent.Flow
import kotlin.random.Random

/**
 * The bout gets its own unique id so it can easily identified
 */
class Bout(private val asyncFactory: AsyncFactory,
           val competitors: List<Competitor>,
           private val tournament: Tournament,
           @Volatile var state: BoutState = BoutState.REGISTERED) {

    init {
        check(competitors.size >= 2) { "Bout must have at least 2 competitors" }
    }

    private val id: UUID = UUID.randomUUID()

    private val subject = asyncFactory.subject<Pair<BoutState, Detailed<Arena>>>()

    @Volatile
    lateinit var arena: Arena

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
            conductBoutRecursive()
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
                .later { start(tournament.parameters) }
                .subscribe(stillRunningObserver)

            BoutState.STARTED -> asyncFactory
                .later { nextMove() }
                .subscribe(stillRunningObserver)

            else -> {
                val winner = arena.winner
                asyncFactory
                    .later {
                        competitors.forEach {
                            try {
                                it.commChannel.publishResult(arena, winner)
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

    private fun start(parameters: TournamentParameters): Bout {

        val random = Random(System.currentTimeMillis())
        val (terrain, robots) = generateTerrainAndRobots(parameters, random)

        arena = Arena(
            0,
            robots,
            parameters.bounds,
            terrain,
            terrain.mapAll { _, aTerrain ->
                if (aTerrain == Terrain.GREEN && random.nextDouble() < 0.05)
                    if (random.nextDouble() < 0.5) Effect.burnable() else Effect.fire()
                else if (aTerrain != Terrain.ROCK && random.nextDouble() < 0.05)
                    Effect.energy(random.nextInt(10) + 1)
                else
                    Effect.none()
            }
        )

        state = BoutState.STARTED
        subject.onNext(Pair(state, Detailed.none(arena)))
        return this
    }

    private fun generateTerrainAndRobots(
        parameters: TournamentParameters,
        random: Random
    ): Pair<Grid<Terrain>, List<Robot>> {

        var terrain : Grid<Terrain>
        var robots : List<Robot>

        do {
            terrain = createFreshTerrain(parameters, random)
            robots = (0 until competitors.size)
                .fold(mutableListOf()) { list, player ->
                    val robot = Robot(
                        player,
                        createUniquePosition(parameters.bounds, random, list.map(Robot::position)),
                        parameters.startingEnergy,
                        parameters.maxEnergy,
                        parameters.startingHealth,
                        parameters.startingShield,
                        parameters.maxShield
                    )
                    list.add(robot)
                    list
                }

            val costs = djiekstra(terrain, robots[0].position)
            val allReachable = robots.drop(1).fold(1 as Int?) { memo, robot ->
                memo?.let { aMemo -> costs[robot.position]?.let { it + aMemo } }
            } != null

        } while (!allReachable)

        return Pair(terrain, robots)
    }

    private data class DEntry(val cost : Int,
                              val dirs : List<Direction>)

    private fun djiekstra(terrain : Grid<Terrain>, pos : Position) : Grid<Int?> {

        val seen : MutableMap<Position, DEntry> = mutableMapOf()
        val candidates : MutableList<Pair<Position,DEntry>> = mutableListOf(Pair(pos,DEntry(0, emptyList())))

        while (candidates.isNotEmpty()) {

            val candidate = candidates.minBy { it.second.cost }!!
            val (currentPos,currentCosts) = candidate
            candidates.remove(candidate)

            for (direction in Direction.values()) {
                val nextPos = currentPos.move(direction, terrain.bounds)
                if (nextPos != null && terrain[nextPos] != Terrain.ROCK) {
                    val nextCosts = currentCosts.cost + terrain[nextPos].movementCost
                    val seenCosts = seen[nextPos]
                    if (seenCosts == null || nextCosts < seenCosts.cost) {
                        val dentry = DEntry(nextCosts, currentCosts.dirs + direction)
                        seen[nextPos] = dentry
                        candidates.add(Pair(nextPos,dentry))
                    }
                }
            }
        }

        return Grid(terrain.bounds) { aPos -> seen[aPos]?.cost  }
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

    private fun nextMove(): Bout {

        val detailedAfterMove = arena
            .addEnergyTo(arena.activePlayer, tournament.parameters.energyRefill)
            .flatMap { anArena ->
                getMove(anArena)
                    ?.let { aMove -> applyMove(aMove)(anArena) } ?: anArena.killRobot(anArena.activePlayer)
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

    private fun getMove(it: Arena): Move? {
        return competitors[it.activePlayer]
            .commChannel
            .nextMove(it)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bout

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
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
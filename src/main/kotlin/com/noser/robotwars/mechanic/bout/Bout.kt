package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Async
import com.noser.robotwars.mechanic.AsyncFactory
import com.noser.robotwars.mechanic.bout.Moves.applyMove
import com.noser.robotwars.mechanic.tournament.Competitor
import com.noser.robotwars.mechanic.tournament.Tournament
import com.noser.robotwars.mechanic.tournament.TournamentParameters
import kotlin.random.Random

class Bout(private val competitors: (Player) -> Competitor,
           private val tournament: Tournament,
           @Volatile var state: BoutState = BoutState.REGISTERED) {

    @Volatile
    lateinit var arena: Arena

    fun competitors(): List<Competitor> = listOf(competitors(Player.YELLOW), competitors(Player.BLUE))

    fun start(parameters: TournamentParameters) {

        val random = Random(System.currentTimeMillis())
        val terrain = createFreshTerrain(parameters, random)

        arena = Arena(
            Player.YELLOW,
            Player.values()
                .fold(mutableListOf()) { list, player ->
                    val robot = Robot(player,
                                      createUniquePosition(parameters.bounds, random, list.map(Robot::position)),
                                      parameters.startingEnergy,
                                      parameters.maxEnergy,
                                      parameters.startingHealth,
                                      parameters.startingShield,
                                      parameters.maxShield)
                    list.add(robot)
                    list
                }
            ,
            parameters.bounds,
            terrain,
            terrain.mapAll { _, aTerrain ->
                if (aTerrain == Terrain.GREEN && random.nextDouble() < 0.05)
                    Effect.burnable()
                else if (aTerrain != Terrain.ROCK && random.nextDouble() < 0.05)
                    Effect.energy(random.nextInt(10) + 1)
                else
                    Effect.none()
            }
        )

        state = BoutState.STARTED
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

    fun conductBout(asyncFactory: AsyncFactory): Async<Player?> {

        val res = asyncFactory.deferred<Player?>()

        fun go() {
            when (state) {

                BoutState.REGISTERED ->
                    asyncFactory
                        .supplyAsync { start(tournament.parameters) }
                        .map { go() }
                        .finally { _, throwable -> if (throwable != null) res.exception(throwable) }

                BoutState.STARTED ->
                    asyncFactory
                        .supplyAsync { nextMove() }
                        .map { go() }
                        .finally { _, throwable -> if (throwable != null) res.exception(throwable) }

                else -> {
                    val winner = state.winner()
                    asyncFactory
                        .supplyAsync {
                            Player.values().forEach {
                                competitors(it).commChannel.publishResult(arena, winner)
                            }
                        }
                        .finally { _, _ -> res.done(winner) }
                }
            }
        }

        go()
        return res
    }

    private fun nextMove() {

        val move = competitors(arena.activePlayer)
            .commChannel
            .nextMove(arena)

        if (move == null) {
            state = when (arena.activePlayer) {
                Player.YELLOW -> BoutState.BLUE_WINS
                else -> BoutState.YELLOW_WINS
            }
        } else {
            val (afterMove, messages) = applyMove(move)(arena)
            arena = afterMove
            // TODO advance active player
            // TODO do something with the messages
            val winner = afterMove.determineWinner()
            if (winner != null) {
                state = when (winner) {
                    Player.YELLOW -> BoutState.YELLOW_WINS
                    else -> BoutState.BLUE_WINS
                }
            }
        }
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
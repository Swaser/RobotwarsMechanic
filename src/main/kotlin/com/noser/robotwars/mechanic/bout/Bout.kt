package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Async
import com.noser.robotwars.mechanic.tournament.Competitor
import com.noser.robotwars.mechanic.tournament.Tournament
import kotlin.random.Random

class Bout(
    private val competitors: (Player) -> Competitor,
    private val tournament: Tournament,
    @Volatile var state: BoutState = BoutState.REGISTERED
) {

    @Volatile
    lateinit var arena: Arena

    fun competitors(): List<Competitor> = listOf(
        competitors(Player.YELLOW),
        competitors(Player.BLUE)
    )

    fun start(arenaSize: Int,
              startingEnergy: Int,
              maxEnergy: Int,
              startingHealth: Int,
              startingShield: Int,
              maxShield: Int) {

        val random = Random(System.currentTimeMillis())
        val terrain = createFreshTerrain(arenaSize, random)

        arena = Arena(
            Player.YELLOW,
            Player.values()
                .fold(mutableListOf()) { list, player ->
                    val robot = Robot(player,
                                      createUniquePosition(arenaSize, arenaSize, random, list.map(Robot::position)),
                                      startingEnergy,
                                      maxEnergy,
                                      startingHealth,
                                      startingShield,
                                      maxShield)
                    list.add(robot)
                    list
                }
            ,
            terrain,
            Effects(terrain.mapAll { _, _, aTerrain ->
                if (aTerrain == Terrain.GREEN && random.nextDouble() < 0.05)
                    Effect.burnable()
                else if (aTerrain != Terrain.ROCK && random.nextDouble() < 0.05)
                    Effect.energy(random.nextInt(10) + 1)
                else
                    Effect.none()
            })
        )

        state = BoutState.STARTED
    }

    private fun createUniquePosition(rows: Int,
                                     cols: Int,
                                     random: Random,
                                     existingPositions: List<Position>): Position {

        var pos: Position
        do {
            pos = Position.random(rows, cols, random)
        } while (existingPositions.contains(pos))

        return pos
    }

    private fun conductBout(asyncProvider: (() -> Unit) -> Async<Unit>) {

        when (state) {

            BoutState.REGISTERED ->
                asyncProvider {
                    start(tournament.arenaSize,
                          tournament.startingEnergy,
                          tournament.maxEnergy,
                          tournament.startingHealth,
                          tournament.startingShield,
                          tournament.maxShield)
                }.map {
                    conductBout(asyncProvider)
                }

            BoutState.STARTED ->
                asyncProvider {
                    nextMove()
                }.map {
                    conductBout(asyncProvider)
                }

            else -> asyncProvider {
                publishResult()
            }
        }
    }

    private fun publishResult() {
        competitors(Player.YELLOW)
            .commChannel
            .publishResult(
                arena,
                state.winner() ?: throw IllegalArgumentException("Bout not yet finished")
            )
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
            val (afterMove, messages) = Arenas.applyMove(arena, move)
            arena = afterMove
            // TODO advance active player
            // TODO do something with the messages
            val winner = Arenas.determineWinner(arena)
            if (winner != null) {
                state = when (winner) {
                    Player.YELLOW -> BoutState.YELLOW_WINS
                    else -> BoutState.BLUE_WINS
                }
            }
        }
    }

    private fun createFreshTerrain(
        arenaSize: Int,
        random: Random
    ): Grid<Terrain> {
        return Grid(arenaSize, arenaSize) { _, _ ->
            val rnd = random.nextDouble()
            when {
                rnd < tournament.chanceForWater -> Terrain.WATER
                rnd < tournament.chanceForRock -> Terrain.ROCK
                else -> Terrain.GREEN
            }
        }
    }
}
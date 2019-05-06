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

    fun start(
        arenaSize: Int,
        startingEnergy: Int
    ) {
        val random = Random(System.currentTimeMillis())
        val terrain = createFreshTerrain(arenaSize, random)

        arena = Arena(
            Player.YELLOW,
            Player.values()
                .fold(mutableListOf()) { list, player ->
                    val robot = Robot(
                        player,
                        createUniqePosition(arenaSize, arenaSize, random, list.map(Robot::position)),
                        startingEnergy
                    )
                    list.add(robot)
                    list
                }
            ,
            terrain,
            terrain.mapAll { _, _, aTerrain ->
                val effects = mutableListOf<Effect>()
                if (aTerrain == Terrain.GREEN && random.nextDouble() < 0.05)
                    effects.add(Effect.Burnable())
                if (aTerrain != Terrain.ROCK && random.nextDouble() < 0.05)
                    effects.add(Effect.Energy(random.nextInt(10)))
                effects
            }
        )

        state = BoutState.STARTED
    }

    private fun createUniqePosition(
        rows: Int,
        cols: Int,
        random: Random,
        existingPositions: List<Position>
    ): Position {

        var pos: Position
        do {
            pos = Position.random(rows, cols, random)
        } while (existingPositions.contains(pos))

        return pos
    }

    private fun conductBout(
        asyncProvider: (() -> Unit) -> Async<Unit>
    ) {

        when (state) {

            BoutState.REGISTERED ->
                asyncProvider {
                    start(tournament.arenaSize, tournament.startingEnergy)
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
            .getCommunicationChannel()
            .publishResult(
                arena,
                state.winner() ?: throw IllegalArgumentException("Bout not yet finished")
            )
    }

    private fun nextMove() {
        val move = competitors(arena.activePlayer)
            .getCommunicationChannel()
            .nextMove(arena)
        if (move == null) {
            state = when (arena.activePlayer) {
                Player.YELLOW -> BoutState.BLUE_WINS
                else -> BoutState.YELLOW_WINS
            }
        } else {
            arena = Arena.apply(arena, move)
            val winner = Arena.determineWinner(arena)
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
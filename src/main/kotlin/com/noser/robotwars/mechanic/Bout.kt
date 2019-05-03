package com.noser.robotwars.mechanic

import kotlin.random.Random

class Bout(
    private val competitors: (Player) -> Competitor,
    private val decouplerFactory: () -> Decoupler<Unit>,
    @Volatile
    var state: BoutState = BoutState.REGISTERED
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
        val yellowPos = Position.random(arenaSize, random)
        var bluePos: Position
        do {
            bluePos = Position.random(arenaSize, random)
        } while (bluePos == yellowPos)

        val terrain = createFreshTerrain(arenaSize, random)

        arena = Arena(
            Player.YELLOW,
            { player ->
                when (player) {
                    Player.YELLOW -> Robot(player, yellowPos, startingEnergy)
                    else -> Robot(player, bluePos, startingEnergy)
                }
            },
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

    private fun conductBout(tournament: Tournament) {

        when (state) {
            BoutState.REGISTERED -> decouplerFactory()
                .later {
                    start(16, 10)
                }.later { conductBout(tournament) }

            BoutState.STARTED -> decouplerFactory()
                .later {
                    nextMove()
                }.later { conductBout(tournament) }

            else -> decouplerFactory()
                .later {
                    publishResult(tournament)
                }
        }
    }

    private fun publishResult(tournament: Tournament) {
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
                rnd < 0.05 -> Terrain.WATER
                rnd < 0.1 -> Terrain.ROCK
                else -> Terrain.GREEN
            }
        }
    }
}
package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.AsyncFactory
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
           private val getCompetitor: (Player) -> Competitor,
           private val tournament: Tournament,
           @Volatile var state: BoutState = BoutState.REGISTERED) {

    private val id: UUID = UUID.randomUUID()

    private val subject = asyncFactory.subject<Bout>()

    @Volatile
    lateinit var arena: Arena

    val competitors = listOf(getCompetitor(Player.YELLOW), getCompetitor(Player.BLUE))

    fun observe(): Flow.Publisher<Bout> = subject

    fun conductBout(): Flow.Publisher<Bout> {
        conductBoutRecursive()
        return observe()
    }

    private val stillRunningObserver = object : Flow.Subscriber<Bout> {
        override fun onSubscribe(subscription: Flow.Subscription) {
            subscription.request(Long.MAX_VALUE)
        }

        override fun onComplete() {}
        override fun onError(throwable: Throwable) = subject.onError(throwable)
        override fun onNext(bout: Bout) {
            subject.onNext(bout)
            conductBoutRecursive()
        }
    }

    private val resolvedObserver = object : Flow.Subscriber<Bout> {
        override fun onSubscribe(subscription: Flow.Subscription) {
            subscription.request(Long.MAX_VALUE)
        }

        override fun onComplete() = subject.onComplete()
        override fun onNext(bout: Bout) = subject.onNext(bout)
        override fun onError(throwable: Throwable) = subject.onError(throwable)
    }

    private fun conductBoutRecursive() {

        when (state) {

            BoutState.REGISTERED -> asyncFactory
                .later { start(tournament.parameters) }
                .subscribe(stillRunningObserver)

            BoutState.STARTED    -> asyncFactory
                .later { nextMove() }
                .subscribe(stillRunningObserver)

            else                 -> {
                val winner = state.winner()
                asyncFactory
                    .later {
                        Player.values().forEach {
                            try {
                                getCompetitor(it).commChannel.publishResult(arena, winner)
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
        return this
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

        val move = getCompetitor(arena.activePlayer)
            .commChannel
            .nextMove(arena)

        if (move == null) {
            state = when (arena.activePlayer) {
                Player.YELLOW -> BoutState.BLUE_WINS
                else          -> BoutState.YELLOW_WINS
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
                    else          -> BoutState.BLUE_WINS
                }
            }
        }
        return this
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
                    rnd < parameters.chanceForRock  -> Terrain.ROCK
                    else                            -> Terrain.GREEN
                }
            }
        }

    }
}
package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.TestCommChannel
import com.noser.robotwars.mechanic.tournament.Competitor
import org.junit.Ignore
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class MoveTest {

    @Test
    @Ignore
    fun testApplyTo() {

        val bounds = Bounds(0..10, 0..10)

        val channelYellow = TestCommChannel()
        val competitorYellow = Competitor(UUID.randomUUID(), "Yellow", "Team 1", channelYellow)
        channelYellow.competitor = competitorYellow

        val channelBlue = TestCommChannel()
        val competitorBlue = Competitor(UUID.randomUUID(), "Blue", "Team 2", channelYellow)
        channelBlue.competitor = competitorBlue

        val competitors = mutableListOf(competitorYellow, competitorBlue)
        val robots = listOf(Robot(competitorYellow, Position(0, 0), 6, 10, 3, 0, 3),
                            Robot(competitorBlue, Position(2, 1), 10, 10, 3, 1, 3))

        val arena = Arena(competitors[0],
                          robots,
                          bounds,
                          Grid(bounds) { pos ->
                              if (pos == Position(3, 1)) Terrain.ROCK else Terrain.GREEN
                          },
                          Grid(bounds) { pos ->
                              if (pos == Position(2, 1)) Effect.burnable() else Effect.none()
                          })

        val move = Move(competitors[0],
                        listOf(Direction.N, Direction.N, Direction.E, Direction.S, Direction.W),
                        4,
                        Direction.S, 2,
                        Direction.S)

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        assertEquals(listOf(), messages)
    }


}
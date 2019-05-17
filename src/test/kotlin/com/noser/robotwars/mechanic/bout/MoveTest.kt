package com.noser.robotwars.mechanic.bout

import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class MoveTest {

    @Test
    @Ignore
    fun testApplyTo() {

        val bounds = Bounds(0..10, 0..10)
        val robots = listOf(Robot(Player.YELLOW, Position(0, 0), 6, 10, 3, 0, 3),
                            Robot(Player.BLUE, Position(2, 1), 10, 10, 3, 1, 3))

        val arena = Arena(Player.YELLOW,
                          robots,
                          bounds,
                          Grid(bounds) { pos ->
                              if (pos == Position(3, 1)) Terrain.ROCK else Terrain.GREEN
                          },
                          Grid(bounds) { pos ->
                              if (pos == Position(2, 1)) Effect.burnable() else Effect.none()
                          })

        val move = Move(Player.YELLOW,
                        listOf(Direction.N, Direction.N, Direction.E, Direction.S, Direction.W),
                        4,
                        Direction.S, 2,
                        Direction.S)

        val (updated, messages) = move.applyTo(arena)

        messages.forEach(::println)

        assertEquals(listOf(), messages)
    }


}
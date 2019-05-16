package com.noser.robotwars.mechanic.bout

import org.junit.Test
import kotlin.test.assertEquals

class MoveTest {


    @Test
    fun testApplyTo() {

        val bounds = Bounds(0..10, 0..10)
        val robots = listOf(Robot(Player.YELLOW, Position(0, 0), 10, 10, 3, 0, 3),
                            Robot(Player.BLUE, Position(9, 9), 10, 10, 3, 0, 3))

        val arena = Arena(Player.YELLOW,
                          robots,
                          bounds,
                          Grid(bounds) { Terrain.GREEN },
                          Effects(Grid(bounds) { Effect.NONE }))

        val move = Move(Player.YELLOW,
                        listOf(Direction.N, Direction.N, Direction.E, Direction.S, Direction.W),
                        4,
                        Direction.S, 2,
                        Direction.N)

        val (updated, messages) = move.applyTo(arena)

        assertEquals(listOf(""), messages)
    }


}
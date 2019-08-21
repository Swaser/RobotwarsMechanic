package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.bout.Direction.*
import org.junit.Test
import kotlin.test.assertEquals

class MoveTest {

    private fun checkRobot(
        updated: Arena,
        player: Int,
        row: Int,
        col: Int,
        energy: Int,
        shield: Int,
        health: Int
    ) {
        assertEquals(row, updated.robots[player].position.row)
        assertEquals(col, updated.robots[player].position.col)
        assertEquals(energy, updated.robots[player].energy)
        assertEquals(shield, updated.robots[player].shield)
        assertEquals(health, updated.robots[player].health)
    }

    @Test
    fun testPass() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            null,
            0,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 10, 10, 10)
    }

    @Test
    fun testApplyDirectionsUnrestrained() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(E, E, E, E),
            0,
            null,
            0,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 4, 6, 10, 10)
    }

    @Test
    fun testApplyDirectionsIntoWall() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(E, E, E, E, E),
            0,
            null,
            0,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 4, 6, 10, 10)
    }

    @Test
    fun testApplyDirectionsIntoRock() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { pos ->
                if (pos == Position(0, 3)) Terrain.ROCK else Terrain.GREEN
            },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(E, E, E),
            0,
            null,
            0,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 2, 8, 10, 10)
    }

    @Test
    fun testApplyDirectionsIntoRobot() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(
            Robot(0, Position(0, 0), 10, 10, 10, 10, 10),
            Robot(1, Position(0, 3), 10, 10, 10, 10, 10)
        )

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(E, E, E),
            0,
            null,
            0,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 2, 8, 10, 10)
        checkRobot(updated, 1, 0, 3, 10, 10, 10)
    }

    @Test
    fun testApplyDirectionsOverWater() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { pos ->
                if (pos == Position(0, 3)) Terrain.WATER else Terrain.GREEN
            },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(E, E, E, E),
            0,
            null,
            0,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 4, 5, 10, 10)
    }

    @Test
    fun testApplyShootingMiss() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(
            Robot(0, Position(0, 0), 10, 10, 10, 10, 10),
            Robot(1, Position(1, 0), 10, 10, 10, 10, 10)
        )

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            E,
            1,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 9, 10, 10)
        checkRobot(updated, 1, 1, 0, 10, 10, 10)
    }

    @Test
    fun testApplyShootingHit1Shield() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(
            Robot(0, Position(0, 0), 10, 10, 10, 10, 10),
            Robot(1, Position(1, 0), 10, 10, 10, 10, 10)
        )

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            S,
            1,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 9, 10, 10)
        checkRobot(updated, 1, 1, 0, 10, 9, 10)
    }

    @Test
    fun testApplyShootingHit5Shield() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(
            Robot(0, Position(0, 0), 10, 10, 10, 10, 10),
            Robot(1, Position(1, 0), 10, 10, 10, 10, 10)
        )

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            S,
            5,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 5, 10, 10)
        checkRobot(updated, 1, 1, 0, 10, 5, 10)
    }

    @Test
    fun testApplyShootingHit1Health() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(
            Robot(0, Position(0, 0), 10, 10, 10, 10, 10),
            Robot(1, Position(1, 0), 10, 10, 10, 0, 10)
        )

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            S,
            1,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 9, 10, 10)
        checkRobot(updated, 1, 1, 0, 10, 0, 9)
    }

    @Test
    fun testApplyLoadShield() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 10, 0, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            4,
            null,
            0,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 6, 4, 10)
    }

    @Test
    fun testApplyLoadShieldTooMuch() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 5, 10, 10, 0, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            6,
            null,
            0,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 0, 5, 10)
    }

    @Test
    fun testApplyRammingMiss() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            null,
            0,
            S
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 9, 10, 10)
    }

    @Test
    fun testApplyRammingWall() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            null,
            0,
            W
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 9, 10, 10)
    }

    @Test
    fun testApplyRammingRock() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { pos ->
                if (pos == Position(0, 1)) Terrain.ROCK else Terrain.GREEN
            },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            null,
            0,
            E
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 9, 10, 10)
    }

    @Test
    fun testApplyRammingRobotEmpty() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(
            Robot(0, Position(0, 0), 10, 10, 10, 10, 10),
            Robot(1, Position(0, 1), 10, 10, 10, 10, 10)
        )

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            null,
            0,
            E
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 9, 10, 10)
        checkRobot(updated, 1, 0, 2, 10, 9, 10)
    }

    @Test
    fun testApplyRammingRobotWall() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(
            Robot(0, Position(0, 3), 10, 10, 10, 10, 10),
            Robot(1, Position(0, 4), 10, 10, 10, 10, 10)
        )

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            null,
            0,
            E
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 3, 9, 10, 10)
        checkRobot(updated, 1, 0, 4, 10, 8, 10)
    }

    @Test
    fun testApplyRammingRobotRock() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(
            Robot(0, Position(0, 0), 10, 10, 10, 10, 10),
            Robot(1, Position(0, 1), 10, 10, 10, 10, 10)
        )

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { pos ->
                if (pos == Position(0, 2)) Terrain.ROCK else Terrain.GREEN
            },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            null,
            0,
            E
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 9, 10, 10)
        checkRobot(updated, 1, 0, 1, 10, 8, 10)
    }

    @Test
    fun testApplyRammingRobotRobot() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(
            Robot(0, Position(0, 0), 10, 10, 10, 10, 10),
            Robot(1, Position(0, 1), 10, 10, 10, 10, 10),
            Robot(2, Position(0, 2), 10, 10, 10, 10, 10)
        )

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            null,
            0,
            E
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 9, 10, 10)
        checkRobot(updated, 1, 0, 1, 10, 8, 10)
        checkRobot(updated, 2, 0, 2, 10, 9, 10)
    }

    @Test
    fun testWrongTurn() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            1,
            listOf(E, E, E, E),
            3,
            E,
            1,
            E
        )

        val (_, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        assertEquals(1, messages.size)
    }

    @Test
    fun testApplyDirectionsNoHealth() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 0, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(E, E, E, E),
            3,
            E,
            1,
            E
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 10, 10, 0)
    }

    @Test
    fun testApplyDirectionsNoEnergy() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 0, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(E, E, E, E),
            0,
            null,
            0,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 0, 10, 10)
    }

    @Test
    fun testShootingNoHealth() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 0, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            E,
            1,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 10, 10, 0)
    }

    @Test
    fun testShootingNoEnergy() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 0, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            E,
            1,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 0, 10, 10)
    }

    @Test
    fun testLoadShieldNoHealth() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 0, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            1,
            null,
            0,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 10, 10, 0)
    }

    @Test
    fun testLoadShieldNoEnergy() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 0, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            1,
            null,
            0,
            null
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 0, 10, 10)
    }

    @Test
    fun testRammingNoHealth() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 10, 10, 0, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            null,
            0,
            E
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 10, 10, 0)
    }

    @Test
    fun testRammingNoEnergy() {

        val bounds = Bounds(0 until 5, 0 until 5)
        val robots = listOf(Robot(0, Position(0, 0), 0, 10, 10, 10, 10))

        val arena = Arena(0,
            robots,
            bounds,
            Grid(bounds) { Terrain.GREEN },
            Grid(bounds) { Effect.none() })

        val move = Move(
            "",
            0,
            listOf(),
            0,
            null,
            0,
            E
        )

        val (updated, messages) = Moves.applyMove(move)(arena)

        messages.forEach(::println)

        checkRobot(updated, 0, 0, 0, 0, 10, 10)
    }
}
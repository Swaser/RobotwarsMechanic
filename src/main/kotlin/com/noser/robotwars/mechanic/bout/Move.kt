package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.lift
import com.noser.robotwars.mechanic.Detailed.Companion.none
import com.noser.robotwars.mechanic.Detailed.Companion.single
import com.noser.robotwars.mechanic.Extensions.before
import com.noser.robotwars.mechanic.tournament.Competitor

data class Move(val competitor: Competitor,
                val directions: List<Direction>,
                val loadShield: Int,
                val shootDirection: Direction?,
                val shootEnergy: Int,
                val ramDirection: Direction?) {

    fun applyDirections(arena: Arena): Detailed<Arena> {

        val (distanceTravelled, directionsApplied) = directions
            .fold(Pair(0, none(arena))) { (dist, memo), dir ->

                val robot = memo.value.findRobot(competitor)
                val newPos = robot.position.move(dir, arena.bounds)

                when {

                    robot.health <= 0 -> Pair(dist, memo.addDetail("$competitor has no health left."))

                    robot.energy <= 0 -> Pair(dist, memo.addDetail("$competitor has no energy left."))

                    newPos == null -> Pair(dist, memo.addDetail("$competitor cannot move $dir out of terrain."))

                    else -> {

                        val occupyingRobot: Robot? = memo.value.findRobot(newPos)
                        val terrain = memo.value.terrain[newPos]

                        when {
                            occupyingRobot != null -> Pair(dist, memo
                                .addDetail("$competitor cannot move $dir ${terrain.preposition} ${terrain.name} occupied by ${occupyingRobot.competitor}."))

                            robot.energy < terrain.movementCost -> Pair(dist, memo
                                .addDetail("$competitor doesn't have enough energy to move $dir ${terrain.preposition} ${terrain.name}"))

                            else -> Pair(dist + 1, memo.flatMap { it.moveTo(competitor, newPos, terrain.movementCost) })
                        }
                    }
                }
            }

        if (distanceTravelled == 0) {
            return directionsApplied.flatMap { it.applyEffects(competitor) }
        }

        return directionsApplied
    }

    fun applyLoadShield(arena: Arena): Detailed<Arena> {

        if (loadShield <= 0) return none(arena)

        val robot = arena.findRobot(competitor)
        return when {

            robot.health <= 0 -> single(arena) { "$competitor has no health left." }

            robot.energy <= 0 -> single(arena) { "$competitor has no energy left." }

            else -> arena.loadShield(competitor, loadShield)
        }
    }

    fun applyFireCannon(arena: Arena): Detailed<Arena> {

        if (shootDirection == null || shootEnergy <= 0) return none(arena)

        val robot = arena.findRobot(competitor)
        return when {

            robot.health <= 0 -> single(arena) { "$competitor has no health left." }

            robot.energy <= 0 -> single(arena) { "$competitor has no energy left." }

            else -> arena.resolveFiring(competitor, shootDirection, shootEnergy)
        }
    }

    fun applyRamming(arena: Arena): Detailed<Arena> {

        if (ramDirection == null) return none(arena)

        val robot = arena.findRobot(competitor)

        return when {

            robot.health <= 0 -> single(arena) { "$competitor has no health left." }

            robot.energy <= 0 -> single(arena) { "$competitor has no energy left." }

            else -> arena.resolveRamming(competitor, ramDirection)
        }
    }
}

object Moves {

    val applyMove: (Move) -> (Arena) -> Detailed<Arena> = { move ->
        { arena -> (applyDirections(move) before
                    lift(applyShieldLoading(move)) before
                    lift(applyFiring(move)) before
                    lift(applyRamming(move)))(arena)
        }
    }

    private fun applyDirections(move: Move): (Arena) -> Detailed<Arena> = { move.applyDirections(it) }

    private fun applyShieldLoading(move: Move): (Arena) -> Detailed<Arena> = { move.applyLoadShield(it) }

    private fun applyRamming(move: Move): (Arena) -> Detailed<Arena> = { move.applyRamming(it) }

    private fun applyFiring(move: Move): (Arena) -> Detailed<Arena> = { move.applyFireCannon(it) }
}
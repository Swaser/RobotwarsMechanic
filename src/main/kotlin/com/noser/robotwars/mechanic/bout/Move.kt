package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.none
import com.noser.robotwars.mechanic.Detailed.Companion.single

data class Move(
    val player: Player,
    val directions: List<Direction>,
    val loadShield: Int,
    val shootDirection: Direction?,
    val shootEnergy: Int,
    val ramDirection: Direction?
) {

    fun applyTo(arena: Arena): Detailed<Arena> {

        if (player != arena.activePlayer) {
            return single(arena, "It's not $player's turn (but ${arena.activePlayer}'s)")
        }

        return applyDirections(arena)
            .flatMap(this::applyLoadShield)
            .flatMap(this::applyFireCannon)
            .flatMap(this::applyRamming)
    }

    private fun applyDirections(arena: Arena): Detailed<Arena> =

        directions.fold(none(arena)) { detailed, dir ->

            val (robot, others) = getRobots(detailed.value)

            val newPos = robot.position.move(dir, arena.terrain.rows, arena.terrain.cols)
            val occupyingRobot: Robot? by lazy { others.find { it.position == newPos } }
            val terrain = detailed.value.terrain[newPos]

            when {

                robot.health <= 0 -> return detailed
                    .flatMap { single(it, "$player has no health left.") }

                newPos == robot.position -> detailed
                    .flatMap { single(it, "$player cannot move out of terrain.") }

                occupyingRobot != null -> detailed
                    .flatMap {
                        single(it, "$player cannot move into terrain occupied by ${occupyingRobot!!.player}.")
                    }

                robot.energy < terrain.movementCost -> detailed
                    .flatMap { single(it, "$player cannot move $dir into $terrain - not enough energy") }

                else ->
                    detailed.flatMap { anArena ->
                        single(robot.copy(position = newPos, energy = robot.energy - terrain.movementCost),
                               "$player moves $dir into $terrain.")
                            .flatMap { movedRobot ->
                                anArena.effects.applyTo(movedRobot)
                                    .flatMap { (anotherRobot, effects) ->
                                        none(Arena(player,
                                                   mutableListOf(anotherRobot).apply { addAll(others) },
                                                   anArena.terrain,
                                                   effects))
                                    }
                            }
                    }
            }
        }

    private fun applyLoadShield(arena: Arena): Detailed<Arena> {
        val (robot, others) = getRobots(arena)

        return when {
            loadShield <= 0 -> none(arena)

            robot.health <= 0 ->
                single(arena, "$player has no health left.")

            else -> {
                val (updated, amount) = robot.loadShield(loadShield)
                single(arena.copy(robots = mutableListOf(updated).apply { addAll(others) }),
                       "$player loads shield by $amount (desired $loadShield)")
            }
        }
    }


    private fun applyFireCannon(arena: Arena): Detailed<Arena> {

        val (robot, others) = getRobots(arena)

        when {
            shootDirection == null || shootEnergy <= 0 -> none(arena)

            robot.health <= 0 ->
                single(arena, "$player has no health left.")

            else -> {
                val (updated, amount) = robot.fireCannon(shootEnergy)
                for (hitPos in shotTrajectory(arena, updated.position, shootDirection)) {
                    // TODO determine damage and effect
                }

                TODO()
            }
        }

        TODO()
    }

    private fun shotTrajectory(arena: Arena, pos : Position, direction : Direction) : Sequence<Position> = TODO("take  into account terrain (rock)")

    private fun applyRamming(arena: Arena): Detailed<Arena> {

        TODO()
    }

    private fun getRobots(arena: Arena): Pair<Robot, List<Robot>> {

        val robot = arena.robots.find { it.player == player }
            ?: throw IllegalArgumentException("$player's Robot not found")

        val others = arena.robots.filter { it.player != player }

        return Pair(robot, others)
    }

}
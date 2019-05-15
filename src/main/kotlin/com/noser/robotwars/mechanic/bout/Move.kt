package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed

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
            return Detailed.single(arena, "It's not $player's turn (but ${arena.activePlayer}'s)")
        }

        return applyDirections(arena)
            .flatMap(this::applyLoadShield)
            .flatMap(this::applyFireCannon)
            .flatMap(this::applyRamming)
    }

    private fun applyDirections(arena: Arena): Detailed<Arena> {
        return directions
            .fold(Detailed.none(arena)) { detailed, dir ->

                val (robot, others) = getRobots(detailed.value)

                val newPos = robot.position.move(dir, arena.terrain.rows, arena.terrain.cols)
                val occupyingRobot: Robot? by lazy { others.find { it.position == newPos } }
                val terrain = detailed.value.terrain[newPos]

                when {

                    robot.health <= 0 -> return detailed.addDetail("$player has no health left.")

                    newPos == robot.position ->
                        detailed.addDetail("$player cannot move out of terrain.")

                    occupyingRobot != null ->
                        detailed.addDetail("$player cannot move into terrain occupied by ${occupyingRobot!!.player}.")

                    robot.energy < terrain.movementCost ->
                        detailed.addDetail("$player cannot move $dir into $terrain - not enough energy")

                    else ->
                        detailed.flatMap { anArena ->
                            Detailed
                                .single(robot.copy(position = newPos, energy = robot.energy - terrain.movementCost),
                                        "$player moves $dir into $terrain.")
                                .flatMap { aRobot ->
                                    anArena.effects.applyTo(aRobot)
                                        .map { (anotherRobot, effects) ->
                                            Arena(player,
                                                  mutableListOf(anotherRobot).apply { addAll(others) },
                                                  anArena.terrain,
                                                  effects)
                                        }
                                }
                        }
                }
            }
    }

    private fun applyLoadShield(arena: Arena): Detailed<Arena> {
        val (robot, others) = getRobots(arena)

        return when {
            robot.health <= 0 ->
                Detailed.single(arena, "$player has no health left.")

            loadShield > 0 -> {
                val (updated, amount) = robot.loadShield(loadShield)
                Detailed.single(arena.copy(robots = mutableListOf(updated).apply { addAll(others) }),
                                "$player loads shield by $amount (desired $loadShield)")
            }
            else -> Detailed.none(arena)
        }
    }


    fun applyFireCannon(arena: Arena): Detailed<Arena> {

        TODO()
    }

    fun applyRamming(arena: Arena): Detailed<Arena> {

        TODO()
    }

    private fun getRobots(arena: Arena): Pair<Robot, List<Robot>> {

        val robot = arena.robots.find { it.player == player }
            ?: throw IllegalArgumentException("$player's Robot not found")

        val others = arena.robots.filter { it.player != player }

        return Pair(robot, others)
    }

}
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

    private fun applyDirections(arena: Arena): Detailed<Arena> =
        directions
            .zip((0..10))
            .fold(Detailed.none(arena)) { detailed, (dir, i) ->

                val (robot, others) = getRobots(detailed.value)

                if (robot.health <= 0) {
                    return detailed.addDetail("applyDirections() step $i: $player has no health left. Terminating move.")
                }

                if (robot.energy <= 0) {

                    return detailed.addDetail("applyDirections() step $i: $player has no energy left. Terminating move.")
                }

                val newPos = robot.position.move(dir, arena.terrain.rows, arena.terrain.cols)


                val occupyingRobot: Robot? by lazy { others.find { it.position == newPos } }
                val terrain = detailed.value.terrain[newPos]

                when {
                    newPos == robot.position ->
                        detailed.addDetail("applyDirections() step $i: $player bumped into terrain edge.")

                    occupyingRobot != null ->
                        detailed.addDetail("applyDirections() step $i: $player bumped into ${occupyingRobot!!.player}.")

                    robot.energy < terrain.movementCost ->
                        detailed.addDetail("applyDirections() step $i: $player cannot move $dir into $terrain - not enough energy")

                    else ->
                        detailed.flatMap { anArena ->
                            Detailed
                                .single(robot.copy(position = newPos, energy = robot.energy - terrain.movementCost),
                                        "applyDirections() step $i: $player moved $dir.")
                                .flatMap { aRobot ->
                                    anArena.effects.applyTo(aRobot)
                                        .mapDetails { "applyDirections() step $i: $it" }
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

    fun applyLoadShield(arena: Arena): Detailed<Arena> {


        TODO()
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
package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.none
import com.noser.robotwars.mechanic.Detailed.Companion.single

data class Move(val player: Player,
                val directions: List<Direction>,
                val loadShield: Int,
                val shootDirection: Direction?,
                val shootEnergy: Int,
                val ramDirection: Direction?) {

    fun applyTo(arena: Arena): Detailed<Arena> {

        if (player != arena.activePlayer) {
            return single(arena) { "It's not $player's turn (but ${arena.activePlayer}'s)" }
        }

        return applyDirections(arena)
            .flatMap(this::applyLoadShield)
            .flatMap(this::applyFireCannon)
            .flatMap(this::applyRamming)
    }

    private fun applyDirections(arena: Arena): Detailed<Arena> =

        directions
            .fold(none(arena)) { memo, dir ->

                val robot = memo.value.findRobot(player)
                val newPos = robot.position.move(dir, arena.bounds)

                when {

                    robot.health <= 0 -> return memo.addDetail("$player has no health left.")

                    robot.energy <= 0 -> return memo.addDetail("$player has no energy left.")

                    newPos == null -> memo.addDetail("$player cannot move $dir out of terrain.")

                    else -> {

                        val occupyingRobot: Robot? = memo.value.findRobot(newPos)
                        val terrain = memo.value.terrain[newPos]

                        when {
                            occupyingRobot != null -> memo
                                .addDetail("$player cannot move $dir ${terrain.preposition} ${terrain.name} occupied by ${occupyingRobot.player}.")

                            robot.energy < terrain.movementCost -> memo
                                .addDetail("$player doesn't have enough energy to move $dir ${terrain.preposition} ${terrain.name}")

                            else -> memo
                                .flatMap { it.moveTo(player, newPos, terrain.movementCost) }
                        }
                    }
                }
            }

    private fun applyLoadShield(arena: Arena): Detailed<Arena> {

        val robot = arena.findRobot(player)
        return when {

            loadShield <= 0 -> none(arena)

            robot.health <= 0 -> single(arena) { "$player has no health left." }

            robot.energy <= 0 -> single(arena) { "$player has no energy left." }

            else -> arena.loadShield(player, loadShield)
        }
    }

    private fun applyFireCannon(arena: Arena): Detailed<Arena> {

        val robot = arena.findRobot(player)
        return when {

            shootDirection == null || shootEnergy <= 0 -> none(arena)

            robot.health <= 0 -> single(arena) { "$player has no health left." }

            robot.energy <= 0 -> single(arena) { "$player has no energy left." }

            else -> {
                arena.fireCannon(player, shootDirection, shootEnergy).flatMap { (fired, amount) ->
                    val trajectory = shotTrajectory(fired.findRobot(player).position, shootDirection, arena.bounds)
                    findRobotHit(fired, trajectory)?.let { fired.takeCannonDamage(it.player, amount) }
                    ?: single(fired) { "$player doesn't hit anyone" }
                }
            }
        }
    }

    private fun applyRamming(arena: Arena): Detailed<Arena> {

        val robot = arena.findRobot(player)

        return when {

            ramDirection == null -> none(arena)

            robot.health <= 0 -> single(arena) { "$player has no health left." }

            robot.energy <= 0 -> single(arena) { "$player has no energy left." }

            else -> arena.resolveRamming(player, ramDirection)
        }
    }
}

private fun shotTrajectory(pos: Position, direction: Direction, bounds: Bounds) =
    generateSequence(pos.move(direction, bounds)) {
        it.move(direction, bounds)
    }

private fun findRobotHit(arena: Arena, shotTrajectory: Sequence<Position>): Robot? {

    for (position in shotTrajectory) {
        val candidate = arena.findRobot(position)
        if (candidate != null) return candidate
    }

    return null
}
}
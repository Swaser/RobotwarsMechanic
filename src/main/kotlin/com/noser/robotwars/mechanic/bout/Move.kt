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

            if (robot.health <= 0) return detailed
                .flatMap { single(it, "$player has no health left.") }

            if (robot.energy <= 0) return detailed
                .flatMap { single(it, "$player has no energy left.") }

            val newPos = robot.position.move(dir, arena.bounds)

            if (newPos == null) {
                detailed.flatMap { single(it, "$player cannot move out of terrain.") }
            } else {
                val occupyingRobot: Robot? by lazy { others.find { it.position == newPos } }
                val terrain = detailed.value.terrain[newPos]

                when {
                    occupyingRobot != null              -> detailed.flatMap {
                        single(it, "$player cannot move $dir ${terrain.preposition} ${terrain.name}" +
                                " occupied by ${occupyingRobot!!.player}.")
                    }

                    robot.energy < terrain.movementCost -> detailed.flatMap {
                        single(it, "$player doesn't have enough energy to" +
                                " move $dir ${terrain.preposition} ${terrain.name}")
                    }

                    else                                ->
                        detailed.flatMap { anArena ->
                            single(robot.copy(position = newPos,
                                              energy = robot.energy - terrain.movementCost),
                                   "$player moves $dir ${terrain.preposition} ${terrain.name}.")
                                .flatMap { movedRobot ->
                                    anArena.effects.applyTo(movedRobot)
                                        .flatMap { (anotherRobot, effects) ->
                                            none(anArena.copy(robots = mutableListOf(anotherRobot).apply { addAll(others) },
                                                              effects = effects))
                                        }
                                }

                        }
                }
            }
        }

    private fun applyLoadShield(arena: Arena): Detailed<Arena> {
        val (robot, others) = getRobots(arena)

        return when {
            loadShield <= 0   -> none(arena)

            robot.health <= 0 -> single(arena, "$player has no health left.")

            robot.energy <= 0 -> single(arena, "$player has no energy left.")

            else              -> {
                val (updated, amount) = robot.loadShield(loadShield)
                single(arena.copy(robots = mutableListOf(updated).apply { addAll(others) }),
                       "$player loads shield by $amount (desired $loadShield)")
            }
        }
    }

    private fun applyFireCannon(arena: Arena): Detailed<Arena> {

        val (robot, others) = getRobots(arena)

        return when {

            shootDirection == null || shootEnergy <= 0 -> none(arena)

            robot.health <= 0                          -> single(arena, "$player has no health left.")

            robot.energy <= 0                          -> single(arena, "$player has no energy left.")

            else                                       -> {

                val (canonFired, amount) = robot.fireCannon(shootEnergy)
                single(canonFired, "$player fires $shootDirection with $amount energy")
                    .flatMap { _ ->
                        findRobotHit(others, shotTrajectory(canonFired.position,
                                                            shootDirection,
                                                            arena.bounds))
                            ?.let { aHitRobot ->
                                arena.effects.robotHit(aHitRobot)
                                    .flatMap { (otherWithEffect, effects) ->
                                        single(otherWithEffect.takeDamage(amount),
                                               "${otherWithEffect.player} takes $amount shot damage from $player")
                                            .flatMap { other ->
                                                none(
                                                    arena.copy(robots = mutableListOf(canonFired).apply {
                                                        addAll(others
                                                                   .map {
                                                                       when {
                                                                           it.position == other.position -> other
                                                                           else                          -> it
                                                                       }
                                                                   })
                                                    },
                                                               effects = effects)
                                                )
                                            }
                                    }
                            } ?: single(
                            arena.copy(robots = mutableListOf(canonFired).apply { addAll(others) }),
                            "$player doesn't hit anyone with cannon"
                        )
                    }
            }
        }
    }

    private fun shotTrajectory(pos: Position, direction: Direction, bounds: Bounds) =
        generateSequence(pos.move(direction, bounds)) {
            it.move(direction, bounds)
        }

    private fun applyRamming(arena: Arena): Detailed<Arena> {

        return none(getRobots(arena)).flatMap { (robot, others) ->
            when {
                ramDirection == null -> none(arena)

                robot.health <= 0    -> single(arena, "$player has no health left.")

                robot.energy <= 0    -> single(arena, "$player has no energy left.")

                else                 -> single(robot.copy(energy = robot.energy - 1),
                                               "$player rams ${ramDirection.name}").flatMap { rammer ->

                    val robots = mutableListOf(rammer)
                    when (val robotHit = others.find {
                        it.position == robot.position.move(ramDirection, arena.bounds)
                    }) {
                        null -> none(arena.copy(robots = robots.apply { addAll(others) }))

                        else -> single(robotHit.takeDamage(1),
                                       "${robotHit.player} is rammed for 1 damage").flatMap { rammed ->

                            val nextPos = rammed.position.move(ramDirection, arena.bounds)
                            val nextHit = others.find { it.position == nextPos }
                            when {

                                nextPos == null                        ->
                                    single(arena.copy(robots = robots.apply {
                                        addAll(others.map {
                                            when {
                                                it.position == rammed.position -> it.takeDamage(1)
                                                else                           -> it
                                            }
                                        })
                                    }),
                                           "${rammed.player} bumps into wall and takes 1 damage.")

                                arena.terrain[nextPos] == Terrain.ROCK ->
                                    single(arena.copy(robots = robots.apply {
                                        addAll(others.map {
                                            when {
                                                it.position == rammed.position -> it.takeDamage(1)
                                                else                           -> it
                                            }
                                        })
                                    }),
                                           "${rammed.player} bumps into rock and takes 1 damage.")

                                nextHit != null                        -> {
                                    single(arena.copy(robots = robots.apply {
                                        addAll(others.map {
                                            when {
                                                it.position == rammed.position -> it.takeDamage(1)
                                                it.position == nextPos         -> it.takeDamage(1)
                                                else                           -> it
                                            }
                                        })
                                    }),
                                           "${rammed.player} bumps into ${nextHit.player}. Both take 1 damage.")
                                }

                                else                                   ->
                                    single(rammed.copy(position = nextPos),
                                           "${rammed.player} is rammed $ramDirection").flatMap { moved ->
                                        arena.effects.applyTo(moved).flatMap { (effected, effects) ->
                                            none(arena.copy(robots = robots.apply {
                                                addAll(others.map {
                                                    when {
                                                        it.position == rammed.position -> effected
                                                        else                           -> it
                                                    }
                                                })
                                            }, effects = effects))
                                        }
                                    }

                            }
                        }
                    }
                }
            }
        }
    }

    private fun findRobotHit(others: List<Robot>, shotTrajectory: Sequence<Position>): Robot? {

        for (position in shotTrajectory) {
            val candidate = others.find { it.position == position }
            if (candidate != null) return candidate
        }

        return null
    }

    private fun getRobots(arena: Arena): Pair<Robot, List<Robot>> {

        val robot = arena.robots.find { it.player == player }
            ?: throw IllegalArgumentException("$player's Robot not found")

        val others = arena.robots.filter { it.player != player }

        return Pair(robot, others)
    }

}
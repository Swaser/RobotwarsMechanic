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
            val newPos = robot.position.move(dir, arena.bounds)
            when {

                robot.health <= 0 -> return detailed.flatMap { single(it, "$player has no health left.") }

                robot.energy <= 0 -> return detailed.flatMap { single(it, "$player has no energy left.") }

                newPos == null    -> detailed.flatMap { single(it, "$player cannot move $dir out of terrain.") }

                else              -> {

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
                                val moved = robot.copy(position = newPos, energy = robot.energy - terrain.movementCost)
                                single(moved,
                                       "$player moves $dir ${terrain.preposition} ${terrain.name} " +
                                               "(${moved.energy}/${moved.shield}/${moved.health}).")
                                    .flatMap { movedRobot ->
                                        anArena.effects.applyTo(movedRobot)
                                            .flatMap { (anotherRobot, effects) ->
                                                val robots = mutableListOf(anotherRobot).apply { addAll(others) }
                                                none(anArena.copy(robots = robots, effects = effects))
                                            }
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
                       "$player loads shield by $amount (desired $loadShield) " +
                               "(${updated.energy}/${updated.shield}/${updated.health}).")
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

                val (fired, amount) = robot.fireCannon(shootEnergy)
                single(fired, "$player fires $shootDirection with $amount energy " +
                        "(${fired.energy}/${fired.shield}/${fired.health})")
                    .flatMap { _ ->
                        findRobotHit(others, shotTrajectory(fired.position, shootDirection, arena.bounds))
                            ?.let { hit ->
                                single(hit.takeDamage(amount),
                                       "${hit.player} takes $amount damage").flatMap { shotResolved ->
                                    arena.effects.robotHit(shotResolved).flatMap { (effectsResolved, effects) ->
                                        val robots = mutableListOf(fired).apply {
                                            addAll(others.map {
                                                when {
                                                    it.position == effectsResolved.position -> effectsResolved
                                                    else                                    -> it
                                                }
                                            })
                                        }
                                        none(arena.copy(robots = robots, effects = effects))
                                    }
                                }
                            }
                            ?: single(arena.copy(robots = mutableListOf(fired).apply { addAll(others) }),
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

        val (robot, others) = getRobots(arena)
        return when {
            ramDirection == null -> none(arena)

            robot.health <= 0    -> single(arena, "$player has no health left.")

            robot.energy <= 0    -> single(arena, "$player has no energy left.")

            else                 -> {

                val rammer = robot.copy(energy = robot.energy - 1)
                single(rammer,
                       "$player rams ${ramDirection.name} (${rammer.energy}/${rammer.shield}/${rammer.health})").flatMap { _ ->

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
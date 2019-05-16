package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.many
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

            val newPos = robot.position.move(dir, arena.terrain.rows, arena.terrain.cols)

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
                                                            arena.terrain.rows,
                                                            arena.terrain.cols))
                            ?.let { aHitRobot ->
                                arena.effects.robotHit(aHitRobot)
                                    .flatMap { (otherWithEffect, effects) ->
                                        single(otherWithEffect.takeDamage(amount),
                                               "${otherWithEffect.player} takes $amount shot damage from $player")
                                            .flatMap { other ->
                                                none(
                                                    Arena(
                                                        player,
                                                        mutableListOf(canonFired).apply {
                                                            addAll(others.map {
                                                                if (it.position == other.position) other else it
                                                            })
                                                        },
                                                        arena.terrain,
                                                        effects
                                                    )
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

    private fun findRobotHit(others: List<Robot>, shotTrajectory: Sequence<Position>): Robot? {

        for (position in shotTrajectory) {
            val candidate = others.find { it.position == position }
            if (candidate != null) return candidate
        }

        return null
    }

    private fun shotTrajectory(pos: Position, direction: Direction, rows: Int, cols: Int) =
        generateSequence(pos.move(direction, rows, cols)) {
            it.move(direction, rows, cols)
        }

    private fun applyRamming(arena: Arena): Detailed<Arena> {

        val (robot, others) = getRobots(arena)

        val robotHit by lazy {
            findRobotHit(others, generateSequence(robot.position.move(ramDirection!!,
                                                                      arena.terrain.rows,
                                                                      arena.terrain.cols)) { null })
        }

        return when {

            ramDirection == null -> none(arena)

            robot.health <= 0    -> single(arena, "$player has no health left.")

            robot.energy <= 0    -> single(arena, "$player has no energy left.")

            robotHit == null     -> {
                // no one is hit -> simply loose energy
                val robots = mutableListOf(robot.copy(energy = robot.energy - 1)).apply { addAll(others) }
                single(arena.copy(robots = robots), "$player rams ${ramDirection.name} but doesn't hit anyone.")
            }

            else                 -> {

                val nextPos = robotHit!!.position.move(ramDirection, arena.terrain.rows, arena.terrain.cols)
                val nextRobotHit by lazy { others.find { it.position == nextPos!! } }

                if (nextPos == null || arena.terrain[nextPos] == Terrain.ROCK) {

                    // into wall or rock -> no one moves but robotHit takes an additional damage

                    val robots = mutableListOf(robot.copy(energy = robot.energy - 1)).apply {
                        addAll(others.map {
                            when {
                                it.position == robotHit!!.position -> it.takeDamage(2)
                                else                               -> it
                            }
                        }
                        )
                    }
                    val wallOrRock = nextPos?.let { "rock" } ?: "wall"
                    many(arena.copy(robots = robots),
                         "$player rams ${robotHit!!.player} ${ramDirection.name}",
                         "${robotHit!!.player} takes 2 damage as it is rammed into $wallOrRock")

                } else if (nextRobotHit != null) {

                    // another robot is hit -> no one moves but both take an additional damage
                    val robots = mutableListOf(robot.copy(energy = robot.energy - 1)).apply {
                        addAll(others.map {
                            when {
                                it.position == robotHit!!.position -> it.takeDamage(2)
                                it.position == nextPos             -> it.takeDamage(1)
                                else                               -> it
                            }
                        })
                    }
                    many(arena.copy(robots = robots),
                         "$player rams ${robotHit!!.player} ${ramDirection.name}",
                         "${robotHit!!.player} takes 2 damage as it is rammed into ${nextRobotHit!!.player}",
                         "${nextRobotHit!!.player} takes 1 damage as it is rammed by ${robotHit!!.player}")

                } else {

                    // no next robot hit (and cannot be solid terrain either - but could be fire)
                    arena.effects.applyTo(robotHit!!.takeDamage(1).copy(position = nextPos))
                        .flatMap { (updatedRobot, effects) ->

                            val robots = mutableListOf(robot.copy(energy = robot.energy - 1)).apply {
                                addAll(others.map {
                                    when {
                                        it.position == robotHit!!.position -> updatedRobot
                                        else                               -> it
                                    }
                                })
                            }
                            val updatedArena = arena.copy(robots = robots, effects = effects)
                            if (effects.isFire(nextPos)) {
                                many(updatedArena,
                                     "$player rams ${robotHit!!.player} ${ramDirection.name}",
                                     "${robotHit!!.player} takes 1 damage and is moved ${ramDirection.name}")
                            } else {
                                many(updatedArena,
                                     "$player rams ${robotHit!!.player} ${ramDirection.name}",
                                     "${robotHit!!.player} takes 2 damage and is moved ${ramDirection.name} into fire")
                            }
                        }
                }
            }
        }
    }

    private fun getRobots(arena: Arena): Pair<Robot, List<Robot>> {

        val robot = arena.robots.find { it.player == player }
            ?: throw IllegalArgumentException("$player's Robot not found")

        val others = arena.robots.filter { it.player != player }

        return Pair(robot, others)
    }

}
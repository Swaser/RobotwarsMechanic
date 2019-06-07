package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.none
import com.noser.robotwars.mechanic.Detailed.Companion.single

data class Arena(val activePlayer: Player,
                 private val robots: List<Robot>,
                 val bounds: Bounds,
                 val terrain: Grid<Terrain>,
                 private val effects: Grid<Effect>) {

    /**
     * Includes effects
     */
    fun moveTo(player: Player, position: Position, cost: Int): Detailed<Arena> {
        return findRobot(player)
            .moveTo(position, cost)
            .map { withRobots(it) }
            .flatMap { it.applyEffects(player) }
    }

    /**
     * Includes effects
     */
    fun resolveFiring(player: Player, dir: Direction, amount: Int): Detailed<Arena> =

        findRobot(player).fireCannon(dir, amount).flatMap { (robot, dmg) ->
            val shotTrajectory = generateSequence(robot.position.move(dir, bounds)) { it.move(dir, bounds) }
            when (val playerHit = findRobotHit(shotTrajectory)?.player) {
                null -> single(withRobots(robot)) { "$player doesn't hit anything" }
                else -> none(withRobots(robot)).flatMap {
                    it.takeSimpleDamage(playerHit, dmg).flatMap { directFireResolved: Arena ->
                        val robotHit = directFireResolved.findRobot(playerHit)
                        when (effects[robotHit.position]) {
                            is Effect.Burnable -> directFireResolved
                                .ignite(robotHit.position)
                                .flatMap { arena: Arena -> arena.takeSimpleDamage(robotHit.player, 1) }
                            else -> none(directFireResolved)
                        }
                    }
                }
            }
        }

    fun loadShield(player: Player, amount: Int): Detailed<Arena> {
        return findRobot(player).loadShield(amount).map { withRobots(it) }
    }

    fun resolveRamming(player: Player, dir: Direction): Detailed<Arena> {

        val robot = findRobot(player)
        return robot.ram(dir).map { withRobots(it) }.flatMap { arena ->
            val targetPos = robot.position.move(dir, bounds)
            val targetRobot = targetPos?.let { arena.findRobot(it) }
            when {
                targetPos == null -> single(arena) { "$player rams the wall" }
                targetRobot == null -> single(arena) { "$player doesn't hit anyone" }
                else -> {
                    val nextPos = targetPos.move(dir, bounds)
                    val nextTerrain = nextPos?.let { terrain[it] }
                    val nextRobot = nextPos?.let { arena.findRobot(it) }
                    val firstRamDamageDone = targetRobot.takeDamage(1).map { arena.withRobots(it) }

                    when {
                        nextPos == null -> firstRamDamageDone
                            .addDetail("${targetRobot.player} is rammed into the wall").flatMap {
                                targetRobot.takeDamage(1).map { rammed -> it.withRobots(rammed) }
                            }

                        nextTerrain == Terrain.ROCK -> firstRamDamageDone
                            .addDetail("${targetRobot.player} is rammed into a rock").flatMap {
                                targetRobot.takeDamage(1).map { rammed -> it.withRobots(rammed) }
                            }

                        nextRobot != null -> firstRamDamageDone
                            .addDetail("${targetRobot.player} is rammed into ${nextRobot.player}").flatMap {
                                targetRobot.takeDamage(1).flatMap { rammed ->
                                    nextRobot.takeDamage(1).map { secondRammed -> it.withRobots(rammed, secondRammed) }
                                }
                            }

                        else -> firstRamDamageDone.flatMap { it.moveTo(targetRobot.player, nextPos, 0) }
                    }
                }
            }
        }
    }

    private fun takeSimpleDamage(player: Player, amount: Int): Detailed<Arena> {
        return findRobot(player).takeDamage(amount).map { withRobots(it) }
    }

    private fun ignite(position: Position): Detailed<Arena> {
        return single(withEffects(effects.mapOne(position) { Effect.fire() })) {
            "Burnable at $position ignites."
        }
    }

    private fun findRobotHit(shotTrajectory: Sequence<Position>): Robot? {

        for (position in shotTrajectory) {
            val candidate = findRobot(position)
            if (candidate != null) return candidate
        }

        return null
    }

    fun applyEffects(player: Player): Detailed<Arena> {
        val robot = findRobot(player)
        return when (effects[robot.position]) {
            is Effect.Fire -> single(this) { "$player is in fire" }.flatMap {
                robot.takeDamage(1).map { withRobots(it) }
            }
            else -> none(this)
        }
    }

    fun findRobot(position: Position) = robots.find { it.position == position }

    fun findRobot(player: Player) =
        robots.find { it.player == player } ?: throw IllegalArgumentException("Robot $player not found")

    private fun withRobots(vararg robot: Robot) =
        copy(robots = robots.map { existing ->
            robot.find { it.player == existing.player } ?: existing
        })

    private fun withEffects(effects: Grid<Effect>) = copy(effects = effects)

    fun determineWinner(): Player? = TODO()
}

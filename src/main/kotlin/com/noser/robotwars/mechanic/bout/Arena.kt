package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.none
import com.noser.robotwars.mechanic.Detailed.Companion.single
import com.noser.robotwars.mechanic.tournament.Competitor

data class Arena(val competitors: List<Competitor>,
                 val robots: List<Robot>,
                 val bounds: Bounds,
                 val terrain: Grid<Terrain>,
                 val effects: Grid<Effect>) {

    /**
     * Includes effects
     */
    fun moveTo(competitor: Competitor, position: Position, cost: Int): Detailed<Arena> {
        return findRobot(competitor)
            .moveTo(position, cost)
            .map { withRobots(it) }
            .flatMap { it.applyEffects(competitor) }
    }

    /**
     * Includes effects
     */
    fun resolveFiring(competitor: Competitor, dir: Direction, amount: Int): Detailed<Arena> =

        findRobot(competitor).fireCannon(dir, amount).flatMap { (robot, dmg) ->
            val shotTrajectory = generateSequence(robot.position.move(dir, bounds)) { it.move(dir, bounds) }
            when (val competitorHit = findRobotHit(shotTrajectory)?.competitor) {
                null -> single(withRobots(robot)) { "$competitor doesn't hit anything" }
                else -> none(withRobots(robot)).flatMap {
                    it.takeSimpleDamage(competitorHit, dmg).flatMap { directFireResolved: Arena ->
                        val robotHit = directFireResolved.findRobot(competitorHit)
                        when (effects[robotHit.position]) {
                            is Effect.Burnable -> directFireResolved
                                .ignite(robotHit.position)
                                .flatMap { arena: Arena -> arena.takeSimpleDamage(robotHit.competitor, 1) }
                            else -> none(directFireResolved)
                        }
                    }
                }
            }
        }

    fun loadShield(competitor: Competitor, amount: Int): Detailed<Arena> {
        return findRobot(competitor).loadShield(amount).map { withRobots(it) }
    }

    fun resolveRamming(competitor: Competitor, dir: Direction): Detailed<Arena> {

        val robot = findRobot(competitor)
        return robot.ram(dir).map { withRobots(it) }.flatMap { arena ->
            val targetPos = robot.position.move(dir, bounds)
            val targetRobot = targetPos?.let { arena.findRobot(it) }
            when {
                targetPos == null -> single(arena) { "$competitor rams the wall" }
                targetRobot == null -> single(arena) { "$competitor doesn't hit anyone" }
                else -> {
                    val nextPos = targetPos.move(dir, bounds)
                    val nextTerrain = nextPos?.let { terrain[it] }
                    val nextRobot = nextPos?.let { arena.findRobot(it) }
                    val firstRamDamageDone = targetRobot.takeDamage(1).map { arena.withRobots(it) }

                    when {
                        nextPos == null -> firstRamDamageDone
                            .addDetail("${targetRobot.competitor.name} is rammed into the wall").flatMap {
                                targetRobot.takeDamage(1).map { rammed -> it.withRobots(rammed) }
                            }

                        nextTerrain == Terrain.ROCK -> firstRamDamageDone
                            .addDetail("${targetRobot.competitor.name} is rammed into a rock").flatMap {
                                targetRobot.takeDamage(1).map { rammed -> it.withRobots(rammed) }
                            }

                        nextRobot != null -> firstRamDamageDone
                            .addDetail("${targetRobot.competitor.name} is rammed into ${nextRobot.competitor.name}").flatMap {
                                targetRobot.takeDamage(1).flatMap { rammed ->
                                    nextRobot.takeDamage(1).map { secondRammed -> it.withRobots(rammed, secondRammed) }
                                }
                            }

                        else -> firstRamDamageDone.flatMap { it.moveTo(targetRobot.competitor, nextPos, 0) }
                    }
                }
            }
        }
    }

    private fun takeSimpleDamage(competitor: Competitor, amount: Int): Detailed<Arena> {
        return findRobot(competitor).takeDamage(amount).map { withRobots(it) }
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

    fun applyEffects(competitor: Competitor): Detailed<Arena> {
        val robot = findRobot(competitor)
        return when (effects[robot.position]) {
            is Effect.Fire -> single(this) { "$competitor is in fire" }.flatMap {
                robot.takeDamage(1).map { withRobots(it) }
            }
            else -> none(this)
        }
    }

    fun findRobot(position: Position) = robots.find { it.position == position }

    fun findRobot(competitor: Competitor) =
        robots.find { it.competitor == competitor } ?: throw IllegalArgumentException("Robot $competitor not found")

    private fun withRobots(vararg robot: Robot) =
        copy(robots = robots.map { existing ->
            robot.find { it.competitor == existing.competitor } ?: existing
        })

    private fun withEffects(effects: Grid<Effect>) = copy(effects = effects)

    private fun getHealthyRobots() = robots.filter { it.health > 0 }

    fun hasAWinner(): Boolean {
        return getHealthyRobots().size <= 1
    }

    fun getWinner(): Competitor? {
        return getHealthyRobots().let { if (it.size == 1) it[0].competitor else null }
    }

    fun harakiri(competitor: Competitor): Detailed<Arena> {
        return findRobot(competitor).run { takeDamage(health) }.map { withRobots(it) }
    }
}

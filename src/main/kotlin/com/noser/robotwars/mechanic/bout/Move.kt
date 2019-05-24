package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.lift
import com.noser.robotwars.mechanic.Detailed.Companion.none
import com.noser.robotwars.mechanic.Detailed.Companion.single
import com.noser.robotwars.mechanic.Extensions.before

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

    fun applyDirections(arena: Arena): Detailed<Arena> =

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
                                .flatMap { anArena -> anArena.moveTo(player, newPos, terrain.movementCost) }
                        }
                    }
                }
            }

    fun applyLoadShield(arena: Arena): Detailed<Arena> {

        if (loadShield <= 0) return none(arena)

        val robot = arena.findRobot(player)
        return when {

            robot.health <= 0 -> single(arena) { "$player has no health left." }

            robot.energy <= 0 -> single(arena) { "$player has no energy left." }

            else -> arena.loadShield(player, loadShield)
        }
    }

    fun applyFireCannon(arena: Arena): Detailed<Arena> {

        if (shootDirection == null || shootEnergy <= 0) return none(arena)

        val robot = arena.findRobot(player)
        return when {

            robot.health <= 0 -> single(arena) { "$player has no health left." }

            robot.energy <= 0 -> single(arena) { "$player has no energy left." }

            else -> arena.resolveFiring(player, shootDirection, shootEnergy)
        }
    }

    fun applyRamming(arena: Arena): Detailed<Arena> {

        if (ramDirection == null) return none(arena)

        val robot = arena.findRobot(player)

        return when {

            robot.health <= 0 -> single(arena) { "$player has no health left." }

            robot.energy <= 0 -> single(arena) { "$player has no energy left." }

            else -> arena.resolveRamming(player, ramDirection)
        }
    }
}

object Moves {

    val applyMove: (Move) -> (Arena) -> Detailed<Arena> = { move ->
        { arena ->
            when {
                arena.activePlayer == move.player ->
                    applyDirections(move) before
                    lift(applyShieldLoading(move)) before
                    lift(applyFiring(move)) before
                    lift(applyRamming(move))
                else -> {
                    { single(arena) { "It's not ${move.player}'s turn (but ${arena.activePlayer}'s)" } }
                }
            }(arena)
        }
    }

    fun applyDirections(move: Move): (Arena) -> Detailed<Arena> = { move.applyDirections(it) }

    fun applyShieldLoading(move: Move): (Arena) -> Detailed<Arena> = { move.applyDirections(it) }

    fun applyRamming(move: Move): (Arena) -> Detailed<Arena> = { move.applyDirections(it) }

    fun applyFiring(move: Move): (Arena) -> Detailed<Arena> = { move.applyDirections(it) }

}
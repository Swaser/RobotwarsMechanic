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
            return Detailed(arena, listOf("It's not $player's turn (but ${arena.activePlayer}'s)"))
        }

        return applyDirections(arena)
            .flatMap(this::applyLoadShield)
            .flatMap(this::applyFireCannon)
            .flatMap(this::applyRamming)
    }

    fun applyDirections(arena: Arena): Detailed<Arena> {


        TODO()
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

        return Pair(robot,others)
    }

}
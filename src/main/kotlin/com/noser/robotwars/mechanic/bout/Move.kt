package com.noser.robotwars.mechanic.bout

class Move(
    val player: Player,
    val directions: List<Direction>,
    val loadShield: Int,
    val shootDirection: Direction?,
    val shootEnergy: Int,
    val ramDirection: Direction?
)
package com.noser.robotwars.mechanic.bout

import kotlin.math.min

data class Robot(val player: Player,
                 val position: Position,
                 val energy: Int,
                 val maxEnergy: Int,
                 val health: Int,
                 val shield: Int,
                 val maxShield: Int) {

    init {
        check(maxEnergy > 0)
        check(energy >= 0)
        check(energy <= maxEnergy)
        check(maxShield > 0)
        check(shield >= 0)
        check(shield <= maxShield)
    }

    fun takeDamage(amount: Int): Robot {
        check(amount >= 0) { "damage cannot be negative" }
        val shieldDmg = min(shield,amount)
        val healthDmg = amount - shieldDmg
        return copy(health = health - healthDmg, shield = shield - shieldDmg)
    }

    fun addEnergy(amount: Int): Pair<Robot,Int> {
        check(amount >= 0) { "energy cannot be negative" }
        val actual = min(maxEnergy - energy, amount)
        return Pair(copy(energy = energy + actual), actual)
    }

    fun loadShield(amount: Int): Pair<Robot, Int> {
        check(amount >= 0) { "shield load amount cannot be negative" }
        val actualLoading = min(min(energy, maxShield - shield), amount)
        return Pair(copy(energy = energy - actualLoading, shield = shield + actualLoading), actualLoading)
    }

    /**
     * returns the robot and the actual energy in the shot
     */
    fun fireCannon(amount: Int): Pair<Robot, Int> {
        check(amount >= 0) { "shield load amount cannot be negative" }
        val actualEnergy = min(energy, amount)
        return Pair(copy(energy = energy - actualEnergy), actualEnergy)
    }

}

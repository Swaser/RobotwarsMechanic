package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.single
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

    fun moveTo(pos: Position, energyCost: Int): Robot {
        check(position.distanceTo(pos) == 1) { "Robot can only move 1 field at a time" }
        check(energyCost >= 0) { "Energy cost of move cannot be negative" }
        check(energy >= energyCost) { "Robot doesn't have enough energy to move" }
        return copy(position = pos, energy = energy - energyCost)
    }

    fun takeDamage(amount: Int): Detailed<Robot> {
        check(amount >= 0) { "damage cannot be negative" }
        val shieldDmg = min(shield, amount)
        val healthDmg = amount - shieldDmg
        return single(copy(health = health - healthDmg, shield = shield - shieldDmg)) {
            "$player takes $amount damage - $shieldDmg to the shield (${it.shield} shield left; ${it.health} left)"
        }
    }

    fun addEnergy(amount: Int): Pair<Robot, Int> {
        check(amount >= 0) { "energy cannot be negative" }
        val actual = min(maxEnergy - energy, amount)
        return Pair(copy(energy = energy + actual), actual)
    }

    fun loadShield(amount: Int): Detailed<Robot> {

        check(amount >= 0) { "shield load amount cannot be negative" }

        val actual = min(min(energy, maxShield - shield), amount)

        return single(copy(energy = energy - actual, shield = shield + actual)) {
            "$player loads shield for $actual (desired $amount - ${it.energy} energy left)"
        }
    }

    fun fireCannon(dir: Direction, amount: Int): Detailed<Pair<Robot, Int>> {

        check(amount >= 0) { "fire cannon amount cannot be negative" }

        val actual = min(energy, amount)

        return single(Pair(copy(energy = energy - actual), actual)) { (fired, _) ->
            "$player fires cannon $dir for $actual energy (desired $amount - ${fired.energy} energy left)"
        }
    }
}

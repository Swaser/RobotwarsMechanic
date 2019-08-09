package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.single
import kotlin.math.min

class Robot(val player: Int,
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

    private fun update(energy: Int? = null,
                       shield: Int? = null,
                       health: Int? = null,
                       position: Position? = null): Robot =
        Robot(player,
              position ?: this.position,
              energy ?: this.energy,
              maxEnergy,
              health ?: this.health,
              shield ?: this.shield,
              maxShield)

    fun moveTo(pos: Position, energyCost: Int): Detailed<Robot> {
        check(position.distanceTo(pos) == 1) { "Robot can only move 1 field at a time" }
        check(energyCost >= 0) { "Energy cost of move cannot be negative" }
        check(energy >= energyCost) { "Robot doesn't have enough energy to move" }
        return single(update(energy - energyCost, position = pos)) {
            val action = if (energyCost == 0) "is pushed" else "moves"
            "$player $action to $pos (E=${it.energy},S=${it.shield},H=${it.health})"
        }
    }

    fun takeDamage(amount: Int): Detailed<Robot> {
        check(amount >= 0) { "damage cannot be negative" }
        val shieldDmg = min(shield, amount)
        val healthDmg = amount - shieldDmg
        return single(update(shield = shield - shieldDmg, health = health - healthDmg)) {
            "$player takes $amount damage ($shieldDmg to shield) (E=${it.energy},S=${it.shield},H=${it.health})"
        }
    }

    fun addEnergy(amount: Int): Pair<Int, Detailed<Robot>> {
        check(amount >= 0) { "energy cannot be negative" }
        val actual = min(maxEnergy - energy, amount)
        return Pair(actual, single(update(energy + actual)) {
            "$player receives $actual ($amount) energy (E=${it.energy},S=${it.shield},H=${it.health})"
        })
    }

    fun ram(dir: Direction): Detailed<Robot> {
        check(energy >= 1) { "robot must have at least 1 energy to ram" }
        return single(update(energy - 1)) {
            "$player rams $dir  (E=${it.energy},S=${it.shield},H=${it.health})"
        }
    }

    fun loadShield(amount: Int): Detailed<Robot> {
        check(amount >= 0) { "shield load amount cannot be negative" }
        val actual = min(min(energy, maxShield - shield), amount)
        return single(update(energy - actual, shield = shield + actual)) {
            "$player loads shield by $actual ($amount) (E=${it.energy},S=${it.shield},H=${it.health})"
        }
    }

    fun fireCannon(dir: Direction, amount: Int): Detailed<Pair<Robot, Int>> {
        check(amount >= 0) { "fire cannon amount cannot be negative" }
        val actual = min(energy, amount)
        return single(Pair(update(energy - actual), actual)) { (it, _) ->
            "$player fires cannon $dir for $actual ($amount) (E=${it.energy},S=${it.shield},H=${it.health})"
        }
    }
}

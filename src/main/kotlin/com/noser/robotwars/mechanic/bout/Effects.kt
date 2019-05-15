package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed

class Effects(private val grid: Grid<Effect>) {

    fun applyTo(robot: Robot): Detailed<Pair<Robot, Effects>> =

        when (val effect = grid[robot.position]) {

            is Effect.Fire -> Detailed.single(Pair(robot.takeDamage(effect.amount), this),
                                              "${robot.player} took ${effect.amount} fire damage")

            is Effect.Energy -> {
                val (updated, amount) = robot.addEnergy(effect.amount)
                Detailed.single(Pair(updated, Effects(grid.mapOne(robot.position) { Effect.none() })),
                                "${robot.player} absorbed $amount energy")
            }

            else -> Detailed.none(Pair(robot, this))
        }

    fun robotHit(robot: Robot) : Detailed<Pair<Robot,Effects>> = TODO("burnable -> fire / robot takes damage if burnable -> fire")
}
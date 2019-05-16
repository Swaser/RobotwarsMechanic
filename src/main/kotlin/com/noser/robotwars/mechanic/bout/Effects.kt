package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.none

class Effects(private val grid: Grid<Effect>) {

    fun applyTo(robot: Robot): Detailed<Pair<Robot, Effects>> =

        when (val effect = grid[robot.position]) {

            is Effect.Fire   -> Detailed.single(Pair(robot.takeDamage(effect.amount), this),
                                                "${robot.player} took ${effect.amount} fire damage")

            is Effect.Energy -> {
                val (updated, amount) = robot.addEnergy(effect.amount)
                Detailed.single(Pair(updated, Effects(grid.mapOne(robot.position) { Effect.none() })),
                                "${robot.player} absorbed $amount energy")
            }

            else             -> Detailed.none(Pair(robot, this))
        }

    fun isFire(position: Position) = grid[position] is Effect.Fire

    fun robotHit(robot: Robot): Detailed<Pair<Robot, Effects>> {

        // TODO
        return none(Pair(robot, this))
    }
}
package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed
import com.noser.robotwars.mechanic.Detailed.Companion.none
import com.noser.robotwars.mechanic.Detailed.Companion.single

class Effects(private val grid: Grid<Effect>) {

    fun applyTo(robot: Robot): Detailed<Pair<Robot, Effects>> =

        when (val effect = grid[robot.position]) {

            is Effect.Fire -> robot.takeDamage(effect.amount).flatMap { burned ->
                none(Pair(burned, this))
            }

            is Effect.Energy -> {
                single(robot.addEnergy(effect.amount)) { (updated, amount) ->
                    "${updated.player} absorbed $amount energy to ${updated.energy}"
                }.flatMap { (updated, _) ->
                    none(Pair(updated, Effects(grid.mapOne(robot.position) { Effect.none() })))
                }
            }

            else -> none(Pair(robot, this))
        }

    fun robotHit(robot: Robot): Detailed<Pair<Robot, Effects>> {

        // TODO
        return none(Pair(robot, this))
    }
}
package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.Detailed

data class Arena(val activePlayer: Player,
                 val robots: List<Robot>,
                 val terrain: Grid<Terrain>,
                 val effects: Effects)

object Arenas {

    /**
     * Will never advance the active player
     */
    fun applyMove(arena: Arena, move: Move): Detailed<Arena> {

        TODO("Move shield loading, firing and ramming into Move")

//        var robot = arena.robots.find { it.player == move.player }
//                    ?: throw IllegalArgumentException("Robot of ${move.player} not found")
//
//        var otherRobots = arena.robots.filter { it.player != move.player }
//        val messages = mutableListOf<String>()
//        var effects = arena.effects
//
//
//
//        if (move.shootDirection != null && move.shootEnergy > 0) {
//
//            val (shot, amount) = robot.fireCannon(move.shootEnergy)
//            robot = shot
//            val (row, col) = robot.position
//            when {
//                move.shootDirection == Direction.N && robot.position.col > 0 -> {
//                    for (col in ((robot.position.col - 1) downTo 0)) {
//                        val hitPosition = Position(row, col)
//                        val robotHit = otherRobots.find { it.position == hitPosition }
//                        if (robotHit != null) {
//                            otherRobots = otherRobots.map {
//                                if (it.player == robotHit.player) {
//                                    messages.add("ApplyMove Fire Cannon : " +
//                                                 "${arena.activePlayer}'s Robot shot ${robotHit.player} for $amount damage")
//                                    it.takeDamage(amount)
//                                } else {
//                                    it
//                                }
//                            }
//                            if (effects[hitPosition] is Effect.Burnable) {
//                                messages.add("ApplyMove Fire Cannon : Burnable at $hitPosition turns into fire")
//                                effects = effects.mapOne(hitPosition) { Effect.fire() }
//                            }
//                            break
//                        }
//                    }
//                }
//            }
//        }
//
//        // TODO ramming
//
//        return Pair(Arena(arena.activePlayer.next(),
//                          otherRobots.toMutableList().apply { add(robot) },
//                          arena.terrain,
//                          effects),
//                    messages)
    }


    fun determineWinner(arena: Arena): Player? = TODO()
}

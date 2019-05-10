package com.noser.robotwars.mechanic.bout

data class Arena(val activePlayer: Player,
                 val robots: List<Robot>,
                 val terrain: Grid<Terrain>,
                 val effects: Grid<Effect>)

object Arenas {

    /**
     * if the move is not for the active player, the active player will NOT be advanced
     */
    fun applyMove(arena: Arena, move: Move): Pair<Arena, List<String>> {

        if (move.player != arena.activePlayer) {
            return Pair(arena, listOf("It's not ${move.player}'s turn (but $arena.activePlayer's)"))
        }

        var robot = arena.robots.find { it.player == move.player }
                    ?: throw IllegalArgumentException("Robot of ${move.player} not found")

        var otherRobots = arena.robots.filter { it.player != move.player }
        val messages = mutableListOf<String>()
        var effects = arena.effects

        for (i in (0..move.directions.size)) {

            val dir = move.directions[i]

            if (robot.health <= 0) {
                messages.add("ApplyMove Direction $i$dir : " +
                             "${arena.activePlayer}'s Robot has no health left. Terminating move.")
                return Pair(Arena(arena.activePlayer.next(),
                                  otherRobots.toMutableList().apply { add(robot) },
                                  arena.terrain,
                                  effects),
                            messages)
            } else if (robot.energy <= 0) {
                messages.add("ApplyMove Direction $i$dir : " +
                             "${arena.activePlayer}'s Robot has no energy left. Terminating move.")
                return Pair(Arena(arena.activePlayer.next(),
                                  otherRobots.toMutableList().apply { add(robot) },
                                  arena.terrain,
                                  effects),
                            messages)
            }

            val pos = robot.position.move(dir, arena.terrain.rows, arena.terrain.cols)

            if (pos == robot.position) {
                messages.add("ApplyMove Direction $i$dir : " +
                             "${arena.activePlayer}'s Robot bumped into terrain edge"
                )
                continue // ignore this dir
            }

            val occupyingRobot = otherRobots.find { it.position == pos }
            if (occupyingRobot != null) {
                messages.add("ApplyMove Direction $i$dir : " +
                             "${arena.activePlayer}'s Robot bumped into other robot (${occupyingRobot.player})")
                continue // ignore this dir
            }

            // now we can move
            messages.add("Apply Move Direction $i$dir : ${arena.activePlayer}'s Robot moves to $pos")
            robot = robot.copy(position = pos, energy = robot.energy - 1)
            when (val effect = effects[pos]) {
                is Effect.Fire -> {
                    messages.add("Apply Move Direction $i$dir : ${arena.activePlayer}'s Robot drove into a fire")
                    robot = robot.takeDamage(1)
                }
                is Effect.Energy -> {
                    val (added,amount) = robot.addEnergy(effect.amount)
                    robot = added
                    messages.add("Apply Move Direction $i$dir : " +
                                 "${arena.activePlayer}'s Robot picked up $amount energy (from ${effect.amount})")
                    effects = effects.mapOne(pos) { Effect.none() }
                }
            }
        }

        if (move.loadShield > 0) {
            val (loaded, amount) = robot.loadShield(move.loadShield)
            robot = loaded
            messages.add("ApplyMove Shield : ${arena.activePlayer}'s Robot loads shield by $amount to ${robot.shield}")
        }

        if (move.shootDirection != null && move.shootEnergy > 0) {

            val (shot, amount) = robot.fireCannon(move.shootEnergy)
            robot = shot
            val (row, col) = robot.position
            when {
                move.shootDirection == Direction.N && robot.position.col > 0 -> {
                    for (col in ((robot.position.col - 1) downTo 0)) {
                        val hitPosition = Position(row, col)
                        val robotHit = otherRobots.find { it.position == hitPosition }
                        if (robotHit != null) {
                            otherRobots = otherRobots.map {
                                if (it.player == robotHit.player) {
                                    messages.add("ApplyMove Fire Cannon : " +
                                                 "${arena.activePlayer}'s Robot shot ${robotHit.player} for $amount damage")
                                    it.takeDamage(amount)
                                } else {
                                    it
                                }
                            }
                            if (effects[hitPosition] is Effect.Burnable) {
                                messages.add("ApplyMove Fire Cannon : Burnable at $hitPosition turns into fire")
                                effects = effects.mapOne(hitPosition) { Effect.fire() }
                            }
                            break
                        }
                    }
                }
            }
        }

        // TODO ramming

        return Pair(Arena(arena.activePlayer.next(),
                          otherRobots.toMutableList().apply { add(robot) },
                          arena.terrain,
                          effects),
                    messages)
    }


    fun determineWinner(arena: Arena): Player? = TODO()
}

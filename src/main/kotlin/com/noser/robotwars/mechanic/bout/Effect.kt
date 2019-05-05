package com.noser.robotwars.mechanic.bout

sealed class Effect {

    abstract fun applyTo(robot : Robot) : Robot

    class Fire : Effect() {
        override fun applyTo(robot: Robot): Robot {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class Burnable : Effect() {
        override fun applyTo(robot: Robot) = robot
    }

    class Energy(val amount : Int) : Effect() {

        override fun applyTo(robot: Robot): Robot {
            TODO()
        }

        init {
            check(amount > 0) {
                "amount of energy must be > 0"
            }
        }
    }
}
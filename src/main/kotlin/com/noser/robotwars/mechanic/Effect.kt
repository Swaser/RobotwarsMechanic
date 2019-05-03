package com.noser.robotwars.mechanic

sealed class Effect {

    class Fire : Effect()

    class Burnable : Effect()

    class Energy(val amount : Int) : Effect() {

        init {
            check(amount > 0) {
                "amount of energy must be > 0"
            }
        }
    }
}
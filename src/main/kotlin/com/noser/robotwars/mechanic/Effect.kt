package com.noser.robotwars.mechanic

abstract class Effect {

    private class None : Effect()

    private class Fire : Effect()

    private class Energy(val amount : Int) : Effect() {

        init {
            check(amount > 0) {
                "amount of energy must be > 0"
            }
        }
    }
}
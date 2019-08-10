package com.noser.robotwars.mechanic.bout

sealed class Effect {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    class None : Effect()

    class Fire(val amount: Int) : Effect()

    class Burnable : Effect()

    class Energy(val amount: Int) : Effect() {
        init {
            check(amount > 0) {
                "amount of energy must be > 0"
            }
        }
    }

    companion object {
        private val NONE = None()
        private val FIRE = Fire(1)
        private val BURNABLE = Burnable()

        fun none(): Effect = NONE
        fun fire(): Effect = FIRE
        fun burnable(): Effect = BURNABLE
        fun energy(amount: Int): Effect = Energy(amount)
    }
}
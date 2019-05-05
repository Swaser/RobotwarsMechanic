package com.noser.robotwars.mechanic.bout

enum class BoutState {

    REGISTERED, STARTED, YELLOW_WINS, BLUE_WINS;

    fun winner(): Player? = when (this) {
        YELLOW_WINS -> Player.YELLOW
        BLUE_WINS -> Player.BLUE
        else -> null
    }

}
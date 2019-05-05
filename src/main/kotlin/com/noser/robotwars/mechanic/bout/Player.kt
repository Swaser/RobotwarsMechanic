package com.noser.robotwars.mechanic.bout

enum class Player {

    YELLOW, BLUE;

    fun next() = when (this) {
        YELLOW -> BLUE
        else -> YELLOW
    }
}
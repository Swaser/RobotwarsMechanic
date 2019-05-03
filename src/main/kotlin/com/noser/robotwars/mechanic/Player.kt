package com.noser.robotwars.mechanic

enum class Player {

    YELLOW, BLUE;

    fun next() = when (this) {
        YELLOW -> BLUE
        else -> YELLOW
    }
}
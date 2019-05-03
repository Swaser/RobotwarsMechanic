package com.noser.robotwars.mechanic

interface Decoupler {

    fun decouple(job: () -> Unit) : Decoupled
}

interface Decoupled {

    fun whenDone(job : () -> Unit)
}


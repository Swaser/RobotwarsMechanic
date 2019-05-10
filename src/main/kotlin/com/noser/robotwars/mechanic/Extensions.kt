package com.noser.robotwars.mechanic

object Extensions {

    fun <A, B, C, D> Pair<A, B>.flatMap(f: (A, B) -> Pair<C, D>): Pair<C, D> {
        return f(this.first, this.second)
    }

}
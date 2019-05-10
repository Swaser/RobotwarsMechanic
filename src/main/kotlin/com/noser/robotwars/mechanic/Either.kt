package com.noser.robotwars.mechanic

sealed class Either<L, R> {

    abstract fun isLeft(): Boolean

    abstract fun isRight(): Boolean

    abstract fun getLeft(): L

    abstract fun getRight(): R

    /**
     * Usually call this at the end.
     *
     * Do something with L and return an appropriate R so the calc can go on.
     */
    abstract fun handle(fl: (L) -> R): R

    companion object {
        fun <L, R> left(l: L): Either<L, R> = TODO()
        fun <L, R> right(r: R): Either<L, R> = TODO()
    }

    private data class Left<L, R>(private val l: L) : Either<L, R>() {

        override fun isLeft() = true

        override fun isRight() = false

        override fun getLeft() = l

        override fun getRight() = throw IllegalArgumentException("getRight() on left called")

        override fun handle(fl: (L) -> R) = fl(l)
    }

    private data class Right<L, R>(private val r: R) : Either<L, R>() {
        override fun isLeft() = false

        override fun isRight() = true

        override fun getLeft() = throw IllegalArgumentException("getLeft() on right called")

        override fun getRight() = r

        override fun handle(fl: (L) -> R): R = r
    }
}
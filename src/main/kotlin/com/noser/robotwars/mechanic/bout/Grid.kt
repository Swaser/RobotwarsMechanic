package com.noser.robotwars.mechanic.bout

class Grid<V>
private constructor(private val bounds: Bounds,
                    private val grid: Map<Position, V>) {

    constructor(bounds: Bounds, initializer: (Position) -> V)
            : this(bounds, bounds.positions.map { Pair(it, initializer(it)) }.toMap())

    init {
        check(bounds.nRows >= 2) { "Grid must have at least 2 rows" }
        check(bounds.nCols >= 2) { "Grid must have at least 2 cols" }
    }

    operator fun get(pos: Position): V = grid[pos] ?: throw IndexOutOfBoundsException("$pos is not within $bounds")

    fun mapOne(position: Position, f: (V) -> V): Grid<V> =
        mapAll { p, v -> if (p == position) f(v) else v }

    fun <T> mapAll(f: (Position, V) -> T): Grid<T> =
        Grid(bounds, grid.mapValues { (pos, value) -> f(pos, value) })
}
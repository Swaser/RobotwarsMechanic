package com.noser.robotwars.mechanic


/**
 * A grid is a sequence of rows where a row is a sequence of elements
 */
class Grid<V>
private constructor(
    val rows: Int,
    val cols: Int,
    private val grid: List<List<V>>
) {

    constructor(
        rows: Int,
        cols: Int,
        initializer: (Int, Int) -> V
    )
            : this(rows, cols, List(rows) { row -> List(cols) { col -> initializer(row, col) } })

    fun row(r: Int) = grid[r]

    operator fun get(r: Int) = row(r)

    fun col(c: Int) = grid.map { it[c] }

    fun mapOne(position : Position, f : (V) -> V) : Grid<V> =
            mapAll { r, c, v ->
                if (Position(r,c) == position) f(v) else v
            }

    fun <T> mapAll(f: (Int, Int, V) -> T): Grid<T> =
        Grid(rows,
            cols,
            (0 until rows).map { r ->
                (0 until cols).map { c ->
                    f(r, c, grid[r][c])
                }
            })
}
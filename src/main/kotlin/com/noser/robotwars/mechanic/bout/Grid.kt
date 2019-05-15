package com.noser.robotwars.mechanic.bout


/**
 * A grid is a sequence of rows where a row is a sequence of elements
 */
open class Grid<V>
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

    init {
        check(rows >= 2) { "Grid must have at least 2 rows" }
        check(cols >= 2) { "Grid must have at least 2 cols" }
    }

    operator fun get(row: Int): List<V> = grid[row]
    operator fun get(position: Position): V = grid[position.row][position.col]

    fun row(row: Int) = grid[row]

    fun col(c: Int) = grid.map { it[c] }

    fun mapOne(position: Position, f: (V) -> V): Grid<V> =
        mapAll { r, c, v ->
            if (Position(r, c) == position) f(v) else v
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
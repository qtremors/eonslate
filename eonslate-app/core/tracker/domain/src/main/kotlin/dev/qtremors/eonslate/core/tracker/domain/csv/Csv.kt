package dev.qtremors.eonslate.core.tracker.domain.csv

import java.io.Writer

enum class CsvProblem { FORMAT, COLUMNS, ENTRY, TOO_LARGE, TRACKER_CHANGED }

class CsvException(val problem: CsvProblem, val row: Int = 0) : IllegalArgumentException()

/** Comma-separated UTF-8 data, including doubled quotes and newlines inside cells. */
internal object Csv {
    // The marker prevents text being interpreted as a formula and survives a round trip.
    fun escapeText(value: String) = if (mustEscape(value)) "'" + value else value

    fun unescapeText(value: String) =
        if (value.startsWith('\'') && mustEscape(value.drop(1))) value.drop(1) else value

    private fun mustEscape(value: String) = value.startsWith('\'') || value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')

    fun writeRow(output: Writer, cells: List<String>) {
        cells.forEachIndexed { index, cell ->
            if (index > 0) output.write(",")
            if (cell.any { it == '"' || it == ',' || it == '\n' || it == '\r' }) {
                output.write("\"")
                output.write(cell.replace("\"", "\"\""))
                output.write("\"")
            } else output.write(cell)
        }
        output.write("\r\n")
    }

    fun rows(source: String): Sequence<List<String>> = sequence {
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var state = Cell.START
        var line = 1
        var index = if (source.startsWith('\uFEFF')) 1 else 0
        while (index < source.length) {
            val ch = source[index++]
            when {
                state == Cell.QUOTED -> {
                    if (ch == '"') state = Cell.CLOSED else cell.append(ch)
                }
                state == Cell.CLOSED && ch == '"' -> {
                    cell.append(ch)
                    state = Cell.QUOTED
                }
                ch == ',' || ch == '\r' || ch == '\n' -> {
                    row += cell.toString()
                    cell.clear()
                    state = Cell.START
                    if (ch != ',') {
                        if (ch == '\r' && source.getOrNull(index) == '\n') index++
                        yield(row.toList())
                        row.clear()
                        line++
                    }
                }
                state == Cell.START && ch == '"' -> state = Cell.QUOTED
                state == Cell.CLOSED || ch == '"' -> throw CsvException(CsvProblem.FORMAT, line)
                else -> {
                    cell.append(ch)
                    state = Cell.PLAIN
                }
            }
        }
        if (state == Cell.QUOTED) throw CsvException(CsvProblem.FORMAT, line)
        if (state != Cell.START || row.isNotEmpty()) {
            row += cell.toString()
            yield(row.toList())
        }
    }

    private enum class Cell { START, PLAIN, QUOTED, CLOSED }
}

package processor

import java.lang.RuntimeException
import java.util.*
import kotlin.collections.ArrayList

// Basically become redundant, because there is only one descendant
sealed class Matrix<T : Number>(val rows: Int, val columns: Int, filler: ((Int, Int) -> T)? = null) {

    protected abstract val matrix: Array<Array<T>>

    fun getColumn(index: Int): ArrayList<T> {
        val column = ArrayList<T>(rows)
        for (i in 0 until rows) {
            column.add(this[i][index])
        }
        return column
    }

    operator fun get(row: Int) = matrix[row]

    operator fun get(row: Int, column: Int) = matrix[row][column]

    operator fun set(row: Int, array: Array<T>) {
        matrix[row] = array
    }

    operator fun set(row: Int, column: Int, value: T) {
        matrix[row][column] = value
    }

    @Throws(WrongMatrixSize::class)
    abstract operator fun plus(matrix: Matrix<*>): Matrix<T>

    @Throws(WrongMatrixSize::class)
    abstract operator fun minus(matrix: Matrix<*>): Matrix<T>

    @Throws(WrongMatrixSize::class)
    abstract operator fun times(matrix: Matrix<*>): Matrix<T>

    abstract operator fun times(number: Int): Matrix<T>

    abstract operator fun times(number: Double): Matrix<T>

    abstract fun determinant(): Double

    abstract fun cofactorValue(columnExcluded: Int, rowExcluded: Int = 0): Double

    abstract fun cofactorMatrix(): Matrix<T>

    abstract fun inverseMatrix(): Matrix<T>

    // Used by super class `Matrix` to create
    // new instance of sub type of required size
    // without knowing the actual type
    protected abstract fun newInstance(rows: Int, columns: Int, filler: (Int, Int) -> T): Matrix<T>

    override fun toString(): String {
        return buildString {

            val arrayOfArgumentsLengths = Array(columns) { 0 }
            for (column in 0 until columns) {
                arrayOfArgumentsLengths[column] =
                        getColumn(column)
                                .map { String.format("%.2f", it.toDouble()) }
                                .maxBy { it.length }!!.length
            }

            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    append(String.format("%${arrayOfArgumentsLengths[column]}.2f", this@Matrix[row][column].toDouble()))
                    if (column != columns - 1) {
                        append(" ")
                    }
                }
                append("\n")
            }
        }
    }

    fun transpose(command: TransposeCommand = TransposeCommand.MainDiagonal) =
            when (command) {
                TransposeCommand.VerticalLine -> reverse(Direction.LeftToRight)
                TransposeCommand.HorizontalLine -> reverse(Direction.TopToBottom)
                TransposeCommand.MainDiagonal -> {
                    newInstance(columns, rows) { row, column -> this[column][row] }
                }
                TransposeCommand.SideDiagonal -> {
                    newInstance(columns, rows) { row, column -> this[rows - 1 - column][columns - 1 - row] }
                }
                else -> this
            }

    private fun reverse(direction: Direction): Matrix<T> {
        when (direction) {
            Direction.TopToBottom, Direction.BottomToTop -> matrix.reverse()
            Direction.LeftToRight, Direction.RightToLeft -> matrix.forEach { it.reverse() }
        }
        return this
    }
}

sealed class Direction {
    object TopToBottom : Direction()
    object BottomToTop : Direction()
    object LeftToRight : Direction()
    object RightToLeft : Direction()
}

class NumberMatrix(rows: Int, columns: Int, filler: ((Int, Int) -> Number)? = null) : Matrix<Number>(rows, columns, filler) {
    override val matrix: Array<Array<Number>> = Array(rows) { row ->
        Array(columns) { column ->
            filler?.invoke(row, column) ?: 0
        }
    }

    override fun plus(matrix: Matrix<*>) =
            when {
                this.rows == matrix.rows && this.columns == matrix.columns -> {
                    NumberMatrix(this.rows, this.columns) { row, column ->
                        this[row][column].toDouble() + matrix[row][column].toDouble()
                    }
                }
                else -> throw WrongMatrixSize(this, matrix)
            }

    override fun minus(matrix: Matrix<*>) =
            when {
                this.rows == matrix.rows && this.columns == matrix.columns -> {
                    NumberMatrix(this.rows, this.columns) { row, column ->
                        this[row][column].toDouble() - matrix[row][column].toDouble()
                    }
                }
                else -> throw WrongMatrixSize(this, matrix)
            }

    override fun times(matrix: Matrix<*>) =
            when (this.columns) {
                matrix.rows -> {
                    NumberMatrix(rows, matrix.columns) { row, column ->
                        var value = 0.0
                        for (i in 0 until this.columns) {
                            value += this[row][i].toDouble() * matrix[i][column].toDouble()
                        }
                        value
                    }
                }
                else -> throw WrongMatrixSize(this, matrix)
            }

    override fun times(number: Int) =
            NumberMatrix(this.rows, this.columns) { row, column ->
                this[row][column].toDouble() * number
            }

    override fun times(number: Double) =
            NumberMatrix(this.rows, this.columns) { row, column ->
                this[row][column].toDouble() * number
            }

    override fun newInstance(rows: Int, columns: Int, filler: (Int, Int) -> Number) = NumberMatrix(rows, columns, filler)

    override fun determinant(): Double =
            when {
                rows != columns -> throw RuntimeException("Cannot calculate determinant for a non-square matrix: ${toString()}")
                rows == 2 && columns == 2 -> this[0][0].toDouble() * this[1][1].toDouble() - this[0][1].toDouble() * this[1][0].toDouble()
                else -> {
                    var determinant = 0.0
                    for (column in 0 until columns) {
                        determinant += this[0][column].toDouble() * cofactorValue(columnExcluded = column)
                    }
                    determinant
                }
            }

    override fun cofactorValue(columnExcluded: Int, rowExcluded: Int): Double {
        val sign = if ((rowExcluded + columnExcluded + 2) % 2 == 0) 1.0 else -1.0

        return sign * NumberMatrix(rows - 1, columns - 1)
        { row, column ->
            val rowIndex = if (row >= rowExcluded) row + 1 else row
            val columnIndex = if (column >= columnExcluded) column + 1 else column
            this[rowIndex][columnIndex]
        }.determinant()
    }

    override fun cofactorMatrix() =
            NumberMatrix(rows, columns) { row, column ->
                cofactorValue(row, column)
            }

    override fun inverseMatrix(): Matrix<Number> {
        val determinant = determinant()
        if (determinant == 0.0) {
            throw RuntimeException("Determinant cannot be equal to 0 when calculating inverse matrix")
        }

        val cofactorMatrix = cofactorMatrix().transpose()

        return cofactorMatrix.transpose().times(1 / determinant)
    }
}


sealed class MatrixCommand(val rawValue: Int) {
    object Addition : MatrixCommand(1)
    object MultiplicationByNumber : MatrixCommand(2)
    object MultiplicationByMatrix : MatrixCommand(3)
    object TransposeMatrix : MatrixCommand(4)
    object CalculateDeterminant : MatrixCommand(5)
    object InverseMatrix : MatrixCommand(6)
    class Exit(value: Int) : MatrixCommand(value)

    companion object {
        fun forValue(value: Int) =
                when (value) {
                    Addition.rawValue -> Addition
                    MultiplicationByNumber.rawValue -> MultiplicationByNumber
                    MultiplicationByMatrix.rawValue -> MultiplicationByMatrix
                    TransposeMatrix.rawValue -> TransposeMatrix
                    CalculateDeterminant.rawValue -> CalculateDeterminant
                    InverseMatrix.rawValue -> InverseMatrix
                    else -> Exit(value)
                }
    }
}

sealed class TransposeCommand(val rawValue: Int) {
    object MainDiagonal : TransposeCommand(1)
    object SideDiagonal : TransposeCommand(2)
    object VerticalLine : TransposeCommand(3)
    object HorizontalLine : TransposeCommand(4)
    class Unknown(value: Int) : TransposeCommand(value)

    companion object {
        fun forValue(value: Int) =
                when (value) {
                    MainDiagonal.rawValue -> MainDiagonal
                    SideDiagonal.rawValue -> SideDiagonal
                    VerticalLine.rawValue -> VerticalLine
                    HorizontalLine.rawValue -> HorizontalLine
                    else -> Unknown(value)
                }
    }
}

class WrongMatrixSize(matrix1: Matrix<*>, matrix2: Matrix<*>) :
        RuntimeException("Matrices must be of equal size to be summed. Actual size for lhs (${matrix1.rows}, ${matrix1.columns}) and for rhs (${matrix2.rows}, ${matrix2.columns})")


fun main() {
    val scanner = Scanner(System.`in`)

    var isTerminated = false
    while (!isTerminated) {
        println("""
            1. Add matrices
            2. Multiply matrix to a constant
            3. Multiply matrices
            4. Transpose matrix
            5. Calculate a determinant
            6. Inverse matrix
            0. Exit
            Your choice:
            """.trimIndent())

        when (MatrixCommand.forValue(scanner.nextInt())) {
            MatrixCommand.Addition -> addition(scanner)
            MatrixCommand.MultiplicationByNumber -> multiplicationByNumber(scanner)
            MatrixCommand.MultiplicationByMatrix -> multiplicationByMatrix(scanner)
            MatrixCommand.TransposeMatrix -> transposeMatrix(scanner)
            MatrixCommand.CalculateDeterminant -> calculateDeterminant(scanner)
            MatrixCommand.InverseMatrix -> inverseMatrix(scanner)
            is MatrixCommand.Exit -> isTerminated = true
        }
    }
}

private fun readMatrix(firstMessage: String = "Enter size of matrix:", secondMessage: String = "Enter matrix:", scanner: Scanner): Matrix<*> {
    if (!firstMessage.isBlank()) {
        println(firstMessage)
    }
    val firstMatrixSize = Pair(scanner.nextInt(), scanner.nextInt())

    if (!secondMessage.isBlank()) {
        println(secondMessage)
    }
    return newMatrix(ofSize = firstMatrixSize, scanner = scanner)
}

private fun transposeMatrix(scanner: Scanner) {
    println("""
        1. Main diagonal
        2. Side diagonal
        3. Vertical line
        4. Horizontal line
        Your choice:
    """.trimIndent())

    val transposeCommand = TransposeCommand.forValue(scanner.nextInt())

    if (transposeCommand is TransposeCommand.Unknown) {
        println("Wrong transpose command")
        return
    }

    val matrix = readMatrix(scanner = scanner).transpose(transposeCommand)
    println("The result is:")
    println(matrix)
}

private fun addition(scanner: Scanner) {
    val firstMatrix = readMatrix("Enter size of first matrix:",
            "Enter first matrix:",
            scanner = scanner)

    val secondMatrix = readMatrix("Enter size of second matrix:",
            scanner = scanner)

    try {
        val resultMatrix = firstMatrix + secondMatrix
        println("The addition result is:\n$resultMatrix")
    } catch (error: WrongMatrixSize) {
        println("ERROR")
    }
}

private fun newMatrix(ofSize: Pair<Int, Int>, scanner: Scanner) =
        NumberMatrix(ofSize.first, ofSize.second) { _, _ ->
            when {
                scanner.hasNextInt() -> scanner.nextInt()
                scanner.hasNextDouble() -> scanner.nextDouble()
                else -> throw RuntimeException("Wrong input type of matrix data")
            }
        }

private fun multiplicationByNumber(scanner: Scanner) {
    val firstMatrix = readMatrix(scanner = scanner)
    println("Enter multiplier:")
    println("The multiplication result is:")
    println(firstMatrix.times(scanner.nextInt()))
}

private fun multiplicationByMatrix(scanner: Scanner) {
    val firstMatrix = readMatrix("Enter size of first matrix:",
            "Enter first matrix:",
            scanner = scanner)

    val secondMatrix = readMatrix("Enter size of second matrix:",
            scanner = scanner)

    println("The multiplication result is:")
    println(firstMatrix * secondMatrix)
}

private fun calculateDeterminant(scanner: Scanner) {
    val determinant = readMatrix(scanner = scanner).determinant()
    println("The result is:")
    println(determinant)
}

private fun inverseMatrix(scanner: Scanner) {
    val inverseMatrix = readMatrix(scanner = scanner).inverseMatrix()
    println("The result is:")
    println(inverseMatrix)
}

/*

3 3
3 4 55
4 1 1
9 0 0

3 3
4 9 77
13 22 44
56 57 78

4 4
2.65 3.54 3.88 8.99
3.12 5.45 7.77 5.56
5.31 2.23 2.33 9.81
1.67 1.67 1.01 9.99

4 4
 0.40 -0.21  0.28 -0.51
 5.20 -2.07 -0.39 -3.14
-3.38  1.50  0.16  2.05
-0.59  0.23  0.00  0.50

 */
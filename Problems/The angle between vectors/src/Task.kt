import java.util.Scanner
import kotlin.math.acos
import kotlin.math.sqrt

fun main() {
    val scanner = Scanner(System.`in`)
    val u1 = scanner.nextInt().toDouble()
    val u2 = scanner.nextInt().toDouble()
    val v1 = scanner.nextInt().toDouble()
    val v2 = scanner.nextInt().toDouble()
    val dotProduct = u1 * v1 + u2 * v2
    val firstVectorLength = sqrt(u1 * u1 + u2 * u2)
    val secondVectorLength = sqrt(v1 * v1 + v2 * v2)

    println(Math.toDegrees(acos(dotProduct / (firstVectorLength * secondVectorLength))))
}
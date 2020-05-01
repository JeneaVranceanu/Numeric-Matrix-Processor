import java.util.Scanner

fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)
    val a = scanner.nextDouble()
    val b = scanner.nextDouble()
    val c = scanner.nextDouble()
    val discriminant = Math.pow(b, 2.0) - 4 * a * c

    val sqrtOfDiscriminant = Math.sqrt(discriminant)
    val root1 = (-b + sqrtOfDiscriminant) / (2 * a)

    val root2 = (-b - sqrtOfDiscriminant) / (2 * a)

    println(Math.min(root1, root2))
    println(Math.max(root1, root2))
}

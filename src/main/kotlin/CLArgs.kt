import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo

class CLArgs : CliktCommand() {
    override fun run() = Unit

    val input: String by
        option("-i", "--input", help = "Input path").required()

    val output: String by
        option("-o", "--output", help = "Output path").required()

    val cache: String by
        option("-c", "--cache", help = "Cache path").default("./.java-keywords-cache")

    val threads: Int by
        option("-t", "--threads", help = "Thread count").int().restrictTo(1, 100).default(1)

    val force: Boolean by
        option("-f", "--force", help = "Force recalculation without cache").flag()
}
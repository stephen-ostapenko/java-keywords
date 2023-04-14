import com.github.ajalt.clikt.core.NoSuchParameter
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.UsageError
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val arguments = CLArgs()
    try {
        arguments.parse(args)
    } catch (e: PrintHelpMessage) {
        println(arguments.getFormattedHelp())
        return
    } catch (e: NoSuchParameter) {
        println(e.message)
        return
    } catch (e: UsageError) {
        println(e.message)
        return
    } catch (e: Exception) {
        println("Error!\n$e")
        return
    }

    fun getPathWrapper(path: String): Path {
        return if (path.startsWith("/")) {
            Path(path)
        } else {
            Path(path).toAbsolutePath()
        }
    }

    val projectPath = getPathWrapper(arguments.input)
    val outputPath = getPathWrapper(arguments.output)
    val cachePath = getPathWrapper(arguments.cache)

    try {
        val keywordCounter = KeywordCounter(projectPath, outputPath, cachePath, arguments.threads)
        val time = measureTimeMillis { keywordCounter.run(arguments.force) }
        println("\ndone in ${time}ms")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
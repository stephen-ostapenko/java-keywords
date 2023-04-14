import com.github.ajalt.clikt.core.NoSuchParameter
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.UsageError
import java.nio.file.Path
import kotlin.io.path.*
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

    if (!projectPath.exists() || !projectPath.isDirectory() || !projectPath.isReadable()) {
        println("Bad project path '$projectPath'")
        return
    }
    if (!outputPath.exists() || !outputPath.isRegularFile() || !outputPath.isWritable()) {
        println("Bad output path '$outputPath'")
        return
    }
    if (!cachePath.exists()) {
        try {
            cachePath.createFile()
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return
        }
    }
    if (!cachePath.exists() || !cachePath.isRegularFile() || !cachePath.isWritable()) {
        println("Bad cache path '$cachePath'")
        return
    }

    val keywordCounter = try {
        KeywordCounter(projectPath, outputPath, cachePath, arguments.threads)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        return
    }
    try {
        val time = measureTimeMillis { keywordCounter.run(arguments.force) }
        println("\ndone in ${time}ms")
    } catch (e: Exception) {
        keywordCounter.stop()
        println("Error: ${e.message}")
    }
}
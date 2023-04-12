import com.github.ajalt.clikt.core.NoSuchParameter
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.UsageError
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

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

    val keywordCounter = KeywordCounter(projectPath, outputPath, cachePath, arguments.threads)
    val time = measureTimeMillis { keywordCounter.run(arguments.force) }
    println("\ndone in ${time}ms")
}
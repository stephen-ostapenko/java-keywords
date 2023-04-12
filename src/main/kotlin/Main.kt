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
    println(
        measureTimeMillis {
            //val keywordCounter = KeywordCounter("/home/stephen/Desktop/intellij-community", "home/stephen/Desktop/cache", 1)
            val keywordCounter = KeywordCounter(Path(".").toAbsolutePath(), Path("/home/stephen/Desktop/cache"), 3)
            keywordCounter.run()
        }
    )
}
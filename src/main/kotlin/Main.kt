import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries

fun main(args: Array<String>) {
    val keywordCounter = KeywordCounter("/home/stephen/Desktop/intellij-community", "home/stephen/Desktop/cache", 4)
    keywordCounter.run()
    keywordCounter.finish()
}
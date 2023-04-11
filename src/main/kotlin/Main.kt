import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {
    val processor = SourceFileProcessor()
    processor.processSourceFile(FileInputStream("./samples/AirlineProblem.java"))
}
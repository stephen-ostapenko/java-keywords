import java.io.FileInputStream
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

class KeywordCounter(pathToProjectFolder: String, pathToCacheFolder: String, threadNumber: Int) {
    private val pathToProject = Path(pathToProjectFolder)
    private val pathToCache = Path(pathToCacheFolder)
    private val threadPool = Executors.newFixedThreadPool(threadNumber)

    fun run() {
        threadPool.submit {
            listDirectory(pathToProject)
        }
    }

    fun finish() {
        while (!threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
            // pass
        }
    }

    private fun listDirectory(dirPath: Path) {
        try {
            val entries = dirPath.listDirectoryEntries()
            entries.forEach {
                val curPath = dirPath / it

                if (curPath.isRegularFile() && curPath.toString().endsWith(".java")) {
                    threadPool.submit {
                        processSourceFile(curPath)
                    }
                }

                if (curPath.isDirectory()) {
                    threadPool.submit {
                        listDirectory(curPath)
                    }
                }
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    private fun processSourceFile(sourceFilePath: Path) {
        println(sourceFilePath)
        val cacheFileName = sourceFilePath.toString().replace("/", "+++")
        val cachedFilePath = pathToCache / Path(cacheFileName)

        if (cachedFilePath.exists()) {
            return
        }

        try {
            val sourceFileProcessor = SourceFileProcessor(pathToCache)
            sourceFileProcessor.processSourceFile(FileInputStream(sourceFilePath.toFile()))

            sourceFileProcessor.saveStatsToFile(cacheFileName)

            println("Processed file '$sourceFilePath'")
        } catch (e: Exception) {
            println(e)
        }
    }
}
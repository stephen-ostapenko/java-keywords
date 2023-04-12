import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.*

class KeywordCounter(
    private val pathToProject: Path, private val pathToOutput: Path, private val pathToCache: Path,
    threadNumber: Int
) {
    private class FixedThreadFactory : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val thread = Thread(r)
            thread.setUncaughtExceptionHandler { _, e ->
                e.printStackTrace(System.err)
                throw e
            }

            return thread
        }
    }

    private val threadPool = Executors.newFixedThreadPool(threadNumber, FixedThreadFactory())
    private val statsQueue = LinkedBlockingQueue<SourceFileStats>()

    private data class SourceFileStatsEntry(val isTestFile: Boolean, val counter: Map<String, Int>)
    private val fileStatsStorage = mutableMapOf<String, SourceFileStatsEntry>()

    private val filesFound = AtomicInteger(0)
    private val filesProcessed = AtomicInteger(0)
    private val dirsFound = AtomicInteger(0)
    private val dirsProcessed = AtomicInteger(0)

    fun run(force: Boolean = false) {
        if (!force && pathToCache.exists()) {
            loadCache()
            println("loaded ${fileStatsStorage.size} files from cache")
        }

        dirsFound.incrementAndGet()
        threadPool.execute {
            listDirectory(pathToProject)
        }

        while (
            filesProcessed.get() < filesFound.get() ||
            dirsProcessed.get() < dirsFound.get() ||
            statsQueue.isNotEmpty()
        ) {
            val sourceFileStat = statsQueue.poll() ?: continue

            fileStatsStorage[sourceFileStat.filePath] = SourceFileStatsEntry(
                sourceFileStat.isTestFile, sourceFileStat.counter
            )

            pathToCache.appendText(Json.encodeToString(sourceFileStat))
            pathToCache.appendText("\n")
        }

        println("${filesProcessed.get()} / ${filesFound.get()}")
        println("${dirsProcessed.get()} / ${dirsFound.get()}")
        threadPool.shutdown()
    }

    private fun loadCache() {
        val cachedFiles = pathToCache.bufferedReader().lines()
        cachedFiles.forEach {
            val stats = Json.decodeFromString<SourceFileStats>(it)
            fileStatsStorage[stats.filePath] = SourceFileStatsEntry(stats.isTestFile, stats.counter)
        }
    }

    private fun listDirectory(dirPath: Path) {
        val entries = dirPath.listDirectoryEntries()
        entries.forEach {
            val curPath = dirPath / it

            if (curPath.isRegularFile() && curPath.toString().endsWith(".java")) {
                filesFound.incrementAndGet()
                threadPool.execute {
                    processSourceFile(curPath)
                }
            }

            if (curPath.isDirectory()) {
                dirsFound.incrementAndGet()
                threadPool.execute {
                    listDirectory(curPath)
                }
            }
        }

        //println("Processed dir '$dirPath'")
        dirsProcessed.incrementAndGet()
    }

    private fun processSourceFile(sourceFilePath: Path) {
        if (fileStatsStorage[sourceFilePath.toString()] != null) {
            println(sourceFilePath)
            filesProcessed.incrementAndGet()
            return
        }

        val sourceFileProcessor = SourceFileProcessor()
        sourceFileProcessor.processSourceFile(FileInputStream(sourceFilePath.toFile()))
        statsQueue.add(sourceFileProcessor.getStats(sourceFilePath.toString()))

        //println("Processed file '$sourceFilePath'")
        filesProcessed.incrementAndGet()
    }
}
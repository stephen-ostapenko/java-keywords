import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.lang.Thread.UncaughtExceptionHandler
import java.nio.file.Path
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.*

class KeywordCounter(
    private val pathToProject: Path, private val pathToCache: Path,
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

    private val filesFound = AtomicInteger(0)
    private val filesProcessed = AtomicInteger(0)
    private val dirsFound = AtomicInteger(0)
    private val dirsProcessed = AtomicInteger(0)

    fun run() {
        dirsFound.incrementAndGet()
        threadPool.execute {
            listDirectory(pathToProject)
        }

        while (filesProcessed.get() < filesFound.get() || dirsProcessed.get() < dirsFound.get() || statsQueue.isNotEmpty()) {
            val fileStat = statsQueue.poll() ?: continue
            pathToCache.appendText(Json.encodeToString(fileStat))
            pathToCache.appendText("\n")
        }

        println("${filesProcessed.get()} / ${filesFound.get()}")
        println("${dirsProcessed.get()} / ${dirsFound.get()}")
        threadPool.shutdown()
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
        /*val cacheFileName = sourceFilePath.toString().replace("/", "+++")
        val cachedFilePath = pathToCache / Path(cacheFileName)

        if (cachedFilePath.exists()) {
            return
        }*/

        val sourceFileProcessor = SourceFileProcessor(pathToCache)
        sourceFileProcessor.processSourceFile(FileInputStream(sourceFilePath.toFile()))
        statsQueue.add(sourceFileProcessor.getStats(sourceFilePath.toString()))

        //println("Processed file '$sourceFilePath'")
        filesProcessed.incrementAndGet()
    }
}
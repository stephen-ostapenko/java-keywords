import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.io.path.*

class KeywordCounter(
    private val pathToProject: Path, private val pathToOutput: Path, private val pathToCache: Path,
    threadsCount: Int
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

    private val threadPool = Executors.newFixedThreadPool(threadsCount, FixedThreadFactory())
    private val statsQueue = LinkedBlockingQueue<SourceFileStats>()

    private data class SourceFileStatsEntry(val isTestFile: Boolean, val counter: HashMap<String, Int>)
    private val fileStatsStorage = HashMap<String, SourceFileStatsEntry>()
    private val overallStats = ConcurrentHashMap(javaKeywords.associateWith { 0 })

    private val filesFound = AtomicInteger(0)
    private val filesProcessed = AtomicInteger(0)
    private val dirsFound = AtomicInteger(0)
    private val dirsProcessed = AtomicInteger(0)

    private val prettyJson = Json { prettyPrint = true }

    fun run(force: Boolean = false) {
        if (!force && pathToCache.exists()) {
            loadCache()
            println("loaded ${fileStatsStorage.size} files from cache\n")
        }

        dirsFound.incrementAndGet()
        threadPool.execute {
            listDirectory(pathToProject)
        }

        thread(isDaemon = true) {
            while (true) {
                println("""
                    processed ${filesProcessed.get()} files from at least ${filesFound.get()}
                    processed ${dirsProcessed.get()} dirs from at least ${dirsFound.get()}
                    
                """.trimIndent())
                Thread.sleep(1000)
            }
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

        saveStats()
        pathToCache.writeText("")
        println("""
            processed ${filesProcessed.get()} / ${filesFound.get()} files
            processed ${dirsProcessed.get()} / ${dirsFound.get()} dirs
        """.trimIndent())

        threadPool.shutdown()
    }

    private fun loadCache() {
        val cachedFiles = pathToCache.bufferedReader().lines()
        cachedFiles.forEach {
            val stats = Json.decodeFromString<SourceFileStats>(it)
            fileStatsStorage[stats.filePath] = SourceFileStatsEntry(stats.isTestFile, stats.counter)
            updateOverallStats(stats.counter)
        }
    }

    private fun saveStats() {
        pathToOutput.writeText(prettyJson.encodeToString(overallStats.toSortedMap().toMap()))
    }

    private fun updateOverallStats(counter: HashMap<String, Int>) {
        counter.forEach { (keyword, cnt) ->
            overallStats[keyword] = overallStats.getOrDefault(keyword, 0) + cnt
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

        dirsProcessed.incrementAndGet()
    }

    private fun processSourceFile(sourceFilePath: Path) {
        if (fileStatsStorage[sourceFilePath.toString()] != null) {
            filesProcessed.incrementAndGet()
            return
        }

        val sourceFileProcessor = SourceFileProcessor()
        sourceFileProcessor.processSourceFile(FileInputStream(sourceFilePath.toFile()))
        val stats = sourceFileProcessor.getStats(sourceFilePath.toString())
        statsQueue.add(stats)
        updateOverallStats(stats.counter)

        filesProcessed.incrementAndGet()
    }
}
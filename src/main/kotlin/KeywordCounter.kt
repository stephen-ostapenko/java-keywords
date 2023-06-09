import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
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
    // producer-consumer queue for logging file stats to cache
    private val statsQueue = LinkedBlockingQueue<SourceFileStats>()

    private data class SourceFileStatsEntry(val isTestFile: Boolean, val keywordCounter: HashMap<String, Int>)
    // map to hold stats for each file
    private val fileStatsStorage = HashMap<String, SourceFileStatsEntry>()
    // map to hold counts for each keyword
    private val overallStats = ConcurrentHashMap(javaKeywords.associateWith { 0 })

    private val filesFound = AtomicInteger(0)
    private val filesProcessed = AtomicInteger(0)
    private val dirsFound = AtomicInteger(0)
    private val dirsProcessed = AtomicInteger(0)

    private val prettyJson = Json { prettyPrint = true }

    fun run(force: Boolean = false) {
        if (!force && pathToCache.exists()) {
            println("loading cache")
            loadCache()
            println("loaded ${fileStatsStorage.size} files from cache\n")
        }

        dirsFound.incrementAndGet()
        threadPool.execute {
            listDirectory(pathToProject)
        }

        val cacheWriter = pathToCache.bufferedWriter(options = arrayOf(StandardOpenOption.APPEND))
        var lastTimeStatusPrinted = LocalDateTime.now()
        // consumer thread loop for file stats
        while (
            filesProcessed.get() < filesFound.get() ||
            dirsProcessed.get() < dirsFound.get() ||
            statsQueue.isNotEmpty()
        ) {
            val sourceFileStat = statsQueue.poll() ?: continue

            fileStatsStorage[sourceFileStat.filePath] = SourceFileStatsEntry(
                sourceFileStat.isTestFile, sourceFileStat.keywordCounter
            )

            cacheWriter.appendLine(Json.encodeToString(sourceFileStat))

            val currentTime = LocalDateTime.now()
            if (Duration.between(lastTimeStatusPrinted, currentTime).seconds >= 1) {
                println("""
                    processed ${filesProcessed.get()} files from at least ${filesFound.get()}
                    processed ${dirsProcessed.get()} dirs from at least ${dirsFound.get()}
                """.trimIndent() + "\n")
                lastTimeStatusPrinted = currentTime
            }
        }

        saveStatsToOutput()
        cacheWriter.close()
        pathToCache.writeText("") // clearing cache
        println("""
            processed ${filesProcessed.get()} / ${filesFound.get()} files
            processed ${dirsProcessed.get()} / ${dirsFound.get()} dirs
        """.trimIndent())

        threadPool.shutdown()
    }

    fun stop() {
        threadPool.shutdownNow()
    }

    private fun loadCache() {
        val cachedFiles = pathToCache.bufferedReader().lines()
        cachedFiles.forEach loadCachedFileStats@{
            val stats = try {
                Json.decodeFromString<SourceFileStats>(it)
            } catch (e: Exception) {
                return@loadCachedFileStats
            }

            fileStatsStorage[stats.filePath] = SourceFileStatsEntry(stats.isTestFile, stats.keywordCounter)
            updateOverallStats(stats.keywordCounter)
        }
    }

    private fun saveStatsToOutput() {
        pathToOutput.writeText(
            prettyJson.encodeToString(
                overallStats.toSortedMap().toMap()
            )
        )
    }

    private fun updateOverallStats(counter: HashMap<String, Int>) {
        counter.forEach { (keyword, cnt) ->
            overallStats.computeIfPresent(keyword) { _, v -> v + cnt }
        }
    }

    private fun listDirectory(dirPath: Path) {
        if (Thread.interrupted()) {
            return
        }

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
        if (Thread.interrupted()) {
            return
        }

        if (fileStatsStorage[sourceFilePath.toString()] != null) {
            filesProcessed.incrementAndGet()
            return // file is already in cache
        }

        val sourceFileProcessor = SourceFileProcessor()
        sourceFileProcessor.processSourceFile(FileInputStream(sourceFilePath.toFile()))
        val stats = sourceFileProcessor.getStats(sourceFilePath.toString())
        statsQueue.add(stats)
        updateOverallStats(stats.keywordCounter)

        filesProcessed.incrementAndGet()
    }
}
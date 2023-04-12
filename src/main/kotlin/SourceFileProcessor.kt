import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.*

@Serializable
data class SourceFileStats(val isTestFile: Boolean, val counter: Map<String, Int>)

class SourceFileProcessor(private val cacheFolder: Path) {
    private val counter = javaKeywords.associateWith { 0 }.toMutableMap()
    private var commentDepth = 0 // depth of current comment section
                                 // needed to correctly process nested comments

    companion object {
        private const val delimiterRegexpString = """ !"#$%&'()*+,-./:;<=>?@`\[\]^{|}~\\"""
        private val delimiterRegex = Regex("(?<=[$delimiterRegexpString])|(?=[$delimiterRegexpString])")
    }

    /*init {
        if (!cacheFolder.exists() || !cacheFolder.isDirectory() || !cacheFolder.isWritable()) {
            throw IOException("Cache folder doesn't exist or is not writeable")
        }
    }*/

    /*fun run(sourceFilePath: String) {
        val cachedFile = cacheFolder / sourceFilePath.replace("/", "+++")
        if (cachedFile.exists()) {

        }
    }*/

    fun processSourceFile(sourceFile: InputStream) {
        val sourceStream = sourceFile.bufferedReader().lines()
        sourceStream.forEach { line ->
            processLine(line)
        }
    }

    fun saveStatsToFile(fileName: String, isTestFile: Boolean = false) {
        val filePath = cacheFolder / fileName
        if (isTestFile) {
            filePath.writeText(Json.encodeToString(SourceFileStats(true, mapOf())))
            return
        }

        filePath.writeText(Json.encodeToString(SourceFileStats(false, counter)))
    }

    private fun updateCommentDepth(token1: String, token2: String) {
        if (token1 == "/" && token2 == "*") {
            commentDepth++
        }
        if (token1 == "*" && token2 == "/") {
            commentDepth--
        }
    }

    private fun processLine(line: String) {
        val tokens = line.split(delimiterRegex).filter { it.isNotEmpty() }
        println(tokens)

        if (tokens.isEmpty()) {
            return
        }

        if (tokens[0] in listOf(JavaKeywordConstants.IMPORT, JavaKeywordConstants.PACKAGE)) {
            counter[tokens[0]] = counter.getOrDefault(tokens[0], 0) + 1
            return
        }

        for (tokenInd in tokens.indices) {
            val curToken = tokens[tokenInd]
            if (curToken.isBlank()) {
                continue
            }

            if (curToken == "/" && tokenInd > 0 && tokens[tokenInd - 1] == "/") {
                return // the rest of the string is commented
            }

            if (tokenInd > 0) {
                updateCommentDepth(tokens[tokenInd - 1], curToken)
            }

            if (commentDepth == 0 && curToken in javaKeywords) { // to count only uncommented keywords
                counter[tokens[tokenInd]] = counter.getOrDefault(tokens[tokenInd], 0) + 1
            }
        }
    }
}
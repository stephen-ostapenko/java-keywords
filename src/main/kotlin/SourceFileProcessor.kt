import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileWriter
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.*

// statistics for one source file
@Serializable
data class SourceFileStats(val filePath: String, val isTestFile: Boolean, val counter: HashMap<String, Int>)

class SourceFileProcessor {
    private val counter = HashMap(javaKeywords.associateWith { 0 })
    private var commentDepth = 0 // depth of current comment section
                                 // needed to correctly process nested comments
    private var isTestFile = false

    companion object { // to split line of code into tokens
        private const val delimiterRegexpString = """ !"#$%&'()*+,-./:;<=>?@`\[\]^{|}~\\"""
        private val delimiterRegex = Regex("(?<=[$delimiterRegexpString])|(?=[$delimiterRegexpString])")
    }

    fun processSourceFile(sourceFile: InputStream) {
        val sourceStream = sourceFile.bufferedReader().lines()
        for (line in sourceStream) {
            processLine(line)
            if (isTestFile) {
                break
            }
        }
    }

    fun getStats(filePath: String): SourceFileStats {
        return SourceFileStats(filePath, isTestFile, if (isTestFile) HashMap() else counter)
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

            if (tokenInd > 0 && tokens[tokenInd - 1] == "@" && curToken in testAnnotationKeywords) {
                isTestFile = true // file considered to be a test file
                return
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
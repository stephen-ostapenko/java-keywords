import kotlinx.serialization.Serializable
import java.io.InputStream

// statistics for one source file
@Serializable
data class SourceFileStats(val filePath: String, val isTestFile: Boolean, val keywordCounter: HashMap<String, Int>)

class SourceFileProcessor {
    private val keywordCounter = HashMap(javaKeywords.associateWith { 0 })

    // depth of current comment section
    // needed to correctly process nested comments
    private var commentDepth = 0
    // flag that this source file is test file
    private var isTestFile = false

    // to split line of code into tokens
    companion object {
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
        return SourceFileStats(filePath, isTestFile, if (isTestFile) HashMap() else keywordCounter)
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
            keywordCounter[tokens[0]] = keywordCounter.getOrDefault(tokens[0], 0) + 1
            return
        }

        for (tokenInd in tokens.indices) {
            val curToken = tokens[tokenInd]
            if (curToken.isBlank()) {
                continue
            }

            // check for @Test annotations etc
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
                keywordCounter[tokens[tokenInd]] = keywordCounter.getOrDefault(tokens[tokenInd], 0) + 1
            }
        }
    }
}
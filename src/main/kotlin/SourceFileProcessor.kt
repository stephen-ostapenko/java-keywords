import java.io.InputStream

object JavaKeywordConstants {
    const val ABSTRACT = "abstract"
    const val ASSERT = "assert"
    const val BOOLEAN = "boolean"
    const val BREAK = "break"
    const val BYTE = "byte"
    const val CASE = "case"
    const val CATCH = "catch"
    const val CHAR = "char"
    const val CLASS = "class"
    const val CONTINUE = "continue"
    const val DEFAULT = "default"
    const val DO = "do"
    const val DOUBLE = "double"
    const val ELSE = "else"
    const val ENUM = "enum"
    const val EXTENDS = "extends"
    const val FINAL = "final"
    const val FINALLY = "finally"
    const val FLOAT = "float"
    const val FOR = "for"
    const val IF = "if"
    const val IMPLEMENTS = "implements"
    const val IMPORT = "import"
    const val INSTANCEOF = "instanceof"
    const val INT = "int"
    const val INTERFACE = "interface"
    const val LONG = "long"
    const val NATIVE = "native"
    const val NEW = "new"
    const val PACKAGE = "package"
    const val PRIVATE = "private"
    const val PROTECTED = "protected"
    const val PUBLIC = "public"
    const val RETURN = "return"
    const val SHORT = "short"
    const val STATIC = "static"
    const val STRICTFP = "strictfp"
    const val SUPER = "super"
    const val SWITCH = "switch"
    const val SYNCHRONIZED = "synchronized"
    const val THIS = "this"
    const val THROW = "throw"
    const val THROWS = "throws"
    const val TRANSIENT = "transient"
    const val TRY = "try"
    const val VOID = "void"
    const val VOLATILE = "volatile"
    const val WHILE = "while"
}

val javaKeywords = listOf(
    JavaKeywordConstants.ABSTRACT, JavaKeywordConstants.ASSERT, JavaKeywordConstants.BOOLEAN,
    JavaKeywordConstants.BREAK, JavaKeywordConstants.BYTE, JavaKeywordConstants.CASE,
    JavaKeywordConstants.CATCH, JavaKeywordConstants.CHAR, JavaKeywordConstants.CLASS,
    JavaKeywordConstants.CONTINUE, JavaKeywordConstants.DEFAULT, JavaKeywordConstants.DO,
    JavaKeywordConstants.DOUBLE, JavaKeywordConstants.ELSE, JavaKeywordConstants.ENUM,
    JavaKeywordConstants.EXTENDS, JavaKeywordConstants.FINAL, JavaKeywordConstants.FINALLY,
    JavaKeywordConstants.FLOAT, JavaKeywordConstants.FOR, JavaKeywordConstants.IF,
    JavaKeywordConstants.IMPLEMENTS, JavaKeywordConstants.IMPORT, JavaKeywordConstants.INSTANCEOF,
    JavaKeywordConstants.INT, JavaKeywordConstants.INTERFACE, JavaKeywordConstants.LONG,
    JavaKeywordConstants.NATIVE, JavaKeywordConstants.NEW, JavaKeywordConstants.PACKAGE,
    JavaKeywordConstants.PRIVATE, JavaKeywordConstants.PROTECTED, JavaKeywordConstants.PUBLIC,
    JavaKeywordConstants.RETURN, JavaKeywordConstants.SHORT, JavaKeywordConstants.STATIC,
    JavaKeywordConstants.STRICTFP, JavaKeywordConstants.SUPER, JavaKeywordConstants.SWITCH,
    JavaKeywordConstants.SYNCHRONIZED, JavaKeywordConstants.THIS, JavaKeywordConstants.THROW,
    JavaKeywordConstants.THROWS, JavaKeywordConstants.TRANSIENT, JavaKeywordConstants.TRY,
    JavaKeywordConstants.VOID, JavaKeywordConstants.VOLATILE, JavaKeywordConstants.WHILE
)

val delimiters = listOf(
    "\b", "\t", "\n", "\r", " ",
    "!", "\"", "#", "$", "%",
    "&", "'", "(", ")", "*",
    "+", ",", "-", ".", "/",
    ":", ";", "<", "=", ">",
    "?", "@", "`", "[", "\\",
    "]", "^", "{", "|", "}",
    "~"
)

class SourceFileProcessor {
    private val counter = javaKeywords.associateWith { 0 }.toMutableMap()
    private val delimiterRegexpString = """ !"#$%&'()*+,-./:;<=>?@`\[\]^{|}~\\"""
    private val delimiterRegex = Regex("(?<=[$delimiterRegexpString])|(?=[$delimiterRegexpString])")
    private var commentDepth = 0 // depth of current comment section
                                 // needed to correctly process nested comments

    fun processSourceFile(sourceFile: InputStream) {
        val sourceStream = sourceFile.bufferedReader().lines()
        sourceStream.forEach { line ->
            processLine(line)
        }

        counter.forEach { (kw, cnt) ->
            println("'$kw': $cnt")
        }
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
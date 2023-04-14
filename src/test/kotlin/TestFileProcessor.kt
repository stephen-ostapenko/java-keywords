import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.io.FileInputStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals

internal class TestFileProcessor {
    companion object {
        private const val RESOURCES_ROOT = "src/test/resources/"
    }

    private fun readAns(path: Path): Map<String, Int> {
        return Json.decodeFromString(path.readText())
    }

    private fun <K: Comparable<K>, V>assertMapsEqual(a: Map<K, V>, b: Map<K, V>) {
        assertEquals(a.toSortedMap().toList(), b.toSortedMap().toList())
    }

    private fun runTest(tstName: String, ansName: String) {
        val sourceFileProcessor = SourceFileProcessor()
        sourceFileProcessor.processSourceFile(FileInputStream(RESOURCES_ROOT + tstName))

        assertMapsEqual(
            sourceFileProcessor.getStats("").keywordCounter,
            readAns(Path(RESOURCES_ROOT + ansName))
        )
    }

    @Test
    fun test1() {
        runTest("tst1.java", "ans1.txt")
    }

    @Test
    fun test2() {
        runTest("tst2.java", "ans2.txt")
    }

    @Test
    fun test3() {
        runTest("tst3.java", "ans3.txt")
    }

    @Test
    fun test4() {
        runTest("tst4.java", "ans4.txt")
    }

    @Test
    fun test5() {
        runTest("tst5.java", "ans5.txt")
    }
}
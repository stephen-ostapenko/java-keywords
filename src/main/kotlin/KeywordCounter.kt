import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.io.path.*

class KeywordCounter(pathToProject: String, pathToCacheFolder: String, threadNumber: Int) {
    private enum class ProcessTask {
        LIST_DIR, PROCESS_FILE
    }

    private val pathToProject = Path(pathToProject)
    private val threadPool = Executors.newFixedThreadPool(threadNumber)

    private fun runTask(type: ProcessTask, path: Path) {
        when (type) {
            ProcessTask.LIST_DIR -> listDirectory(path)
            ProcessTask.PROCESS_FILE -> processSourceFile(path)
        }
    }

    private fun listDirectory(dirPath: Path) {
        val entries = dirPath.listDirectoryEntries()
        entries.forEach {
            val curPath = dirPath / it

            if (curPath.isRegularFile() && curPath.endsWith(".java")) {
                threadPool.submit {
                    runTask(ProcessTask.PROCESS_FILE, curPath)
                }
            }

            if (curPath.isDirectory()) {
                threadPool.submit {
                    runTask(ProcessTask.LIST_DIR, curPath)
                }
            }
        }
    }

    private fun processSourceFile(sourceFilePath: Path) {

    }
}
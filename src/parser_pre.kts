import java.io.File
import java.nio.file.Files

class Structure {
    val chunks: HashMap<String, Chunk> = hashMapOf()
    fun addChunk(name: String, chunk: Chunk) {
        chunks[name] = chunk
    }
    fun addDependencies() {
        val names = chunks.keys
        for (name in names) {
            for (line in chunks[name]!!.lines) {
                for (otherName in names) {
                    if (otherName != name && line.contains(otherName)) {
                        chunks[name]!!.addDependency(otherName)
                    }
                }
            }
        }
    }
    fun getChunkPrompt(name: String): String {
        var res = "chunk is:\n ${chunks[name]!!.getText()}"
        for (dependency in chunks[name]!!.dependencies) {
            res += "Mentioned concept $dependency:\n" + chunks[dependency]!!.getText() + "\n"
        }
        return res
    }
}

class Chunk(val start: Int) {
    //    val firstStringIndex = 0
    var end = 0
    val lines: MutableList<String> = mutableListOf()
    val dependencies = HashSet<String>()
    fun addLine(value: String) {
        lines.add(value)
    }
    fun addDependency(value: String) {
        dependencies.add(value)
    }
    fun getText(): String {
        return lines.joinToString("\n")
    }
}

fun extractName(header: String): String? {
    val regex = "(fun|class|data class)\\s+([\\w\\d_]+)".toRegex()
    val matchResult = regex.find(header)

    // Return the captured group (the name) or null if no match is found
    return matchResult?.groups?.get(2)?.value
}

fun coolStart(line: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.startsWith("data") ||
        trimmed.startsWith("class") || trimmed.startsWith("fun")) {
        return true
    }
    return false
}

fun parse(lines: List<String>): Structure {
    var res = Structure()
//    val chunks = mutableListOf<Chunk>()
    var depth = 0
    var chunk_name: String = ""
    var inside_object = false

    lines.forEachIndexed { index, line ->
        if (depth == 0 && inside_object) {
            res.chunks[chunk_name]!!.end = index

            inside_object = false
        }
        if (depth == 0 && coolStart(line)) { // starting a new chunk
            chunk_name = extractName(line)!!
            println("new chunk: line ${index + 1}, name $chunk_name")
            res.addChunk(chunk_name, Chunk(index))
            inside_object = true
//            chunks.add(Chunk(index))
        }
        depth += line.count { it == '{' }
        depth -= line.count { it == '}' }
        if (inside_object) {
            res.chunks[chunk_name]!!.addLine(line)
        }
    }
    res.addDependencies()
    return res
}

//fun main() {
//    val lines = File("src/Main.kt").readLines()
//    val structure = parse(lines)
//    println(structure.getChunkPrompt("main"))
//}
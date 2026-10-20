package ro.klooseduard.streetsofsibiu.logic.parity

import java.io.File

object Original {
    val repoRoot = File(System.getProperty("sos.repoRoot") ?: error("sos.repoRoot not set"))
    val game = File(repoRoot, "2026-Game_of_the_year-main/newgame")
    val assets = File(repoRoot, "assets")

    fun file(resPath: String): File = File(game, resPath.removePrefix("res://"))
    fun text(resPath: String): String = file(resPath).readText()
}

class GodotText(text: String) {
    data class Section(val header: String, val attrs: Map<String, String>, val props: Map<String, String>)

    val sections: List<Section>

    init {
        val result = mutableListOf<Section>()
        var header: String? = null
        var attrs = emptyMap<String, String>()
        var props = LinkedHashMap<String, String>()
        val lines = text.lines()
        var i = 0
        fun flush() {
            header?.let { result += Section(it, attrs, props) }
        }
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("[") && !line.startsWith("[\"")) {
                flush()
                header = line.removePrefix("[").substringBefore(' ').removeSuffix("]")
                attrs = Regex("""(\w+)="([^"]*)"""").findAll(line).associate { it.groupValues[1] to it.groupValues[2] }
                props = LinkedHashMap()
            } else if (header != null && line.contains(" = ")) {
                val key = line.substringBefore(" = ")
                var value = line.substringAfter(" = ")
                while (!isComplete(value) && i + 1 < lines.size) {
                    i++
                    value += "\n" + lines[i]
                }
                props[key] = value
            }
            i++
        }
        flush()
        sections = result
    }

    private fun isComplete(value: String): Boolean {
        var quotes = 0
        var depth = 0
        var escaped = false
        for (c in value) {
            if (escaped) { escaped = false; continue }
            when (c) {
                '\\' -> escaped = true
                '"' -> quotes++
                '{', '[', '(' -> if (quotes % 2 == 0) depth++
                '}', ']', ')' -> if (quotes % 2 == 0) depth--
            }
        }
        return quotes % 2 == 0 && depth <= 0
    }

    val extResources: Map<String, String> by lazy {
        sections.filter { it.header == "ext_resource" }.associate { it.attrs.getValue("id") to it.attrs.getValue("path") }
    }

    val subResources: Map<String, Section> by lazy {
        sections.filter { it.header == "sub_resource" }.associateBy { it.attrs.getValue("id") }
    }

    val nodes: List<Section> by lazy { sections.filter { it.header == "node" } }

    val resource: Map<String, String> by lazy { sections.first { it.header == "resource" }.props }

    fun node(name: String): Section = nodes.first { it.attrs["name"] == name }

    fun extPath(value: String): String =
        extResources.getValue(Regex("""ExtResource\("([^"]*)"\)""").find(value)!!.groupValues[1]).removePrefix("res://")

    fun sub(value: String): Section =
        subResources.getValue(Regex("""SubResource\("([^"]*)"\)""").find(value)!!.groupValues[1])

    companion object {
        fun load(resPath: String) = GodotText(Original.text(resPath))

        fun vec2(value: String): Pair<Double, Double> {
            val m = Regex("""Vector2i?\(([^,]+),\s*([^)]+)\)""").matchEntire(value.trim()) ?: error("not a Vector2: $value")
            return m.groupValues[1].toDouble() to m.groupValues[2].toDouble()
        }

        fun string(value: String): String {
            val v = value.trim()
            require(v.startsWith("\"") && v.endsWith("\"")) { "not a string: $value" }
            return v.substring(1, v.length - 1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
        }

        fun numberDict(value: String): Map<String, Double> =
            Regex(""""([^"]+)"\s*:\s*(-?[0-9.]+)""").findAll(value).associate { it.groupValues[1] to it.groupValues[2].toDouble() }
    }
}

fun gdscriptDefaults(resPath: String): Map<String, String> =
    Regex("""^(?:@export(?:_\w+)?\s+)?var\s+(\w+)\s*(?::\s*\w+)?\s*:?=\s*(.+?)\s*(?:#.*)?$""", RegexOption.MULTILINE)
        .findAll(Original.text(resPath)).associate { it.groupValues[1] to it.groupValues[2] }

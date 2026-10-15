package ro.klooseduard.streetsofsibiu.logic.parity

/** Minimal JSON reader for the Godot reference results (objects, arrays, numbers, strings, booleans, null). */
class MiniJson private constructor(private val s: String) {
    private var i = 0

    private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }

    private fun value(): Any? {
        ws()
        return when (s[i]) {
            '{' -> obj()
            '[' -> arr()
            '"' -> str()
            't' -> { i += 4; true }
            'f' -> { i += 5; false }
            'n' -> { i += 4; null }
            else -> num()
        }
    }

    private fun obj(): Map<String, Any?> {
        val m = LinkedHashMap<String, Any?>()
        i++; ws()
        if (s[i] == '}') { i++; return m }
        while (true) {
            ws(); val k = str(); ws(); i++ // ':'
            m[k] = value(); ws()
            if (s[i] == ',') { i++; continue }
            i++; return m
        }
    }

    private fun arr(): List<Any?> {
        val l = ArrayList<Any?>()
        i++; ws()
        if (s[i] == ']') { i++; return l }
        while (true) {
            l += value(); ws()
            if (s[i] == ',') { i++; continue }
            i++; return l
        }
    }

    private fun str(): String {
        val sb = StringBuilder()
        i++
        while (s[i] != '"') {
            if (s[i] == '\\') { i++; sb.append(when (s[i]) { 'n' -> '\n'; 't' -> '\t'; else -> s[i] }) } else sb.append(s[i])
            i++
        }
        i++
        return sb.toString()
    }

    private fun num(): Double {
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
        return s.substring(start, i).toDouble()
    }

    companion object {
        fun parse(text: String): Any? = MiniJson(text).value()
    }
}

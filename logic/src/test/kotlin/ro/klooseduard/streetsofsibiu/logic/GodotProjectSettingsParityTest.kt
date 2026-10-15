package ro.klooseduard.streetsofsibiu.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/** Checks the transcribed settings against the original `project.godot` (read-only). */
class GodotProjectSettingsParityTest {
    private val projectGodot: Map<String, String> by lazy {
        val root = File(System.getProperty("sos.repoRoot") ?: error("sos.repoRoot not set"))
        val file = File(root, "2026-Game_of_the_year-main/newgame/project.godot")
        var section = ""
        val values = mutableMapOf<String, String>()
        file.readLines().forEach { line ->
            when {
                line.startsWith("[") -> section = line.trim('[', ']')
                '=' in line && !line.startsWith(";") && !line.startsWith("\"") ->
                    values["$section/${line.substringBefore('=')}"] = line.substringAfter('=')
            }
        }
        values
    }

    @Test
    fun viewportSizeMatchesOriginal() {
        assertEquals(GodotProjectSettings.VIEWPORT_WIDTH.toString(), projectGodot["display/window/size/viewport_width"])
        assertEquals(GodotProjectSettings.VIEWPORT_HEIGHT.toString(), projectGodot["display/window/size/viewport_height"])
    }

    @Test
    fun textureFilterIsNearest() {
        assertEquals("0", projectGodot["rendering/textures/canvas_textures/default_texture_filter"])
        assertEquals(true, GodotProjectSettings.NEAREST_TEXTURE_FILTER)
    }

    @Test
    fun physicsSettingsAreEngineDefaults() {
        // The original never overrides the physics tick rate, so Godot's defaults apply.
        assertFalse(projectGodot.keys.any { it.startsWith("physics/") })
    }
}

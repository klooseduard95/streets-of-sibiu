package ro.klooseduard.streetsofsibiu.logic

/**
 * Engine-level settings of the original Godot project (`newgame/project.godot`),
 * plus the Godot 4 defaults the project relies on without overriding them.
 */
object GodotProjectSettings {
    /** `display/window/size/viewport_width` */
    const val VIEWPORT_WIDTH = 1920

    /** `display/window/size/viewport_height` */
    const val VIEWPORT_HEIGHT = 1080

    /** `rendering/textures/canvas_textures/default_texture_filter=0` (Nearest). */
    const val NEAREST_TEXTURE_FILTER = true

    /** Godot default `physics/common/physics_ticks_per_second` (not overridden by the project). */
    const val PHYSICS_TICKS_PER_SECOND = 60

    /** Godot default `physics/common/max_physics_steps_per_frame` (not overridden by the project). */
    const val MAX_PHYSICS_STEPS_PER_FRAME = 8
}

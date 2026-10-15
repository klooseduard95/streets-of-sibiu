package ro.klooseduard.streetsofsibiu.logic.world

/**
 * Godot `AnimationPlayer` driving `Sprite2D.frame` with discrete keys (the player's walk cycles),
 * as observed in Godot 4.6.1 (V3, v9 traces):
 * - `play(name)` on a different (or stopped) animation restarts at 0 and shows key 0 immediately;
 *   on the animation already playing it continues.
 * - Playback time advances in idle frames ([process]); loops wrap at [length].
 * - `stop()` resets to 0 and shows key 0 of the current animation.
 * Key times are 32-bit floats in the scene (PackedFloat32Array), compared with the double time.
 */
class FrameAnimator(
    private val animations: Map<String, List<Int>>,
    private val keyStep: Double,
    private val length: Double,
    initialFrame: Int = 0,
) {
    var current: String? = null; private set
    var isPlaying = false; private set
    var frame = initialFrame; private set
    private var time = 0.0
    private val keyTimes = List(animations.values.maxOf { it.size }) { (it * keyStep).toFloat().toDouble() }

    fun play(name: String) {
        if (isPlaying && current == name) return
        current = name
        isPlaying = true
        time = 0.0
        frame = animations.getValue(name)[0]
    }

    fun stop() {
        isPlaying = false
        time = 0.0
        current?.let { frame = animations.getValue(it)[0] }
    }

    fun process(delta: Double) {
        if (!isPlaying) return
        time += delta
        while (time >= length) time -= length
        val keys = animations.getValue(current!!)
        var index = 0
        for (i in keys.indices) if (keyTimes[i] <= time) index = i
        frame = keys[index]
    }
}

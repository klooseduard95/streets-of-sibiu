package ro.klooseduard.streetsofsibiu.core.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import kotlin.math.log10
import kotlin.math.pow

/** Godot audio buses (default_bus_layout.tres): volumes in dB, all routed to Master. */
class AudioBuses {
    var masterDb = -5.9307528
    var musicDb = -5.9307528
    var sfxDb = -10.24935

    val masterLinear get() = dbToLinear(masterDb)
    val musicLinear get() = dbToLinear(musicDb) * masterLinear
    val sfxLinear get() = dbToLinear(sfxDb) * masterLinear

    companion object {
        /** Godot `db_to_linear`. */
        fun dbToLinear(db: Double): Double = 10.0.pow(db / 20.0)

        /** Godot `linear_to_db` (−inf for 0, like Godot). */
        fun linearToDb(linear: Double): Double = if (linear <= 0.0) Double.NEGATIVE_INFINITY else 20.0 * log10(linear)
    }
}

/**
 * The `AudioPlayer` autoload (audio_player.gd / AudioPlayer.tscn), alive for the whole app:
 * - background music `bucurest_hroma.mp3`, restarted when it finishes, playing while paused.
 *   **Centred** (decision O2 / D13): the original's AudioStreamPlayer2D at world origin panned it left.
 * - `HorseTimer` (1 s, pausable): each timeout plays Horse.mp3 with probability 1/250
 *   (`randi_range(1, 250) == 67`) on the SFX bus, also centred.
 */
class MusicDirector(private val buses: AudioBuses, private val random: () -> Int) {
    private val music: Music = Gdx.audio.newMusic(Gdx.files.internal("bucurest_hroma.mp3"))
    private val horse: Sound = Gdx.audio.newSound(Gdx.files.internal("Music/Horse.mp3"))
    private var horseTimer = 1.0

    /** Mirrors `get_tree().paused` for the pausable horse timer. */
    var treePaused = false

    fun start() {
        music.setOnCompletionListener { it.play() }
        applyVolumes()
        music.play()
    }

    fun applyVolumes() {
        music.volume = buses.musicLinear.toFloat()
        music.setPan(0f, buses.musicLinear.toFloat())
    }

    /** Call once per rendered frame with the frame delta. */
    fun update(delta: Float) {
        if (treePaused) return
        horseTimer -= delta
        if (horseTimer < 0) {
            horseTimer += 1.0
            if (random() == 67) horse.play(buses.sfxLinear.toFloat())
        }
    }

    fun pause() = music.pause()
    fun resume() = music.play()

    fun dispose() {
        music.dispose()
        horse.dispose()
    }
}

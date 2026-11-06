package ro.klooseduard.streetsofsibiu.core.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import kotlin.math.log10
import kotlin.math.pow

class AudioBuses {
    var masterDb = -5.9307528
    var musicDb = -5.9307528
    var sfxDb = -10.24935

    val masterLinear get() = dbToLinear(masterDb)
    val musicLinear get() = dbToLinear(musicDb) * masterLinear
    val sfxLinear get() = dbToLinear(sfxDb) * masterLinear

    companion object {
        fun dbToLinear(db: Double): Double = 10.0.pow(db / 20.0)

        fun linearToDb(linear: Double): Double = if (linear <= 0.0) Double.NEGATIVE_INFINITY else 20.0 * log10(linear)
    }
}

class MusicDirector(private val buses: AudioBuses, private val random: () -> Int) {
    private val music: Music = Gdx.audio.newMusic(Gdx.files.internal("bucurest_hroma.mp3"))
    private val horse: Sound = Gdx.audio.newSound(Gdx.files.internal("Music/Horse.mp3"))
    private var horseTimer = 1.0

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

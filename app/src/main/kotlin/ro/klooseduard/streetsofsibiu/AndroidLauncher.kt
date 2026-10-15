package ro.klooseduard.streetsofsibiu

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import ro.klooseduard.streetsofsibiu.core.StreetsOfSibiuGame

/** Android entry point: hosts the shared libGDX game in an immersive landscape activity. */
class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
        }
        initialize(StreetsOfSibiuGame(), config)
    }
}

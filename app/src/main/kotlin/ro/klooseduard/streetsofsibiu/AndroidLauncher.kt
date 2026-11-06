package ro.klooseduard.streetsofsibiu

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import ro.klooseduard.streetsofsibiu.core.StreetsOfSibiuGame

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
        }
        initialize(StreetsOfSibiuGame(intent.getStringExtra("screen") ?: "menu", intent.getStringExtra("room"), intent.getStringExtra("dash"), intent.getStringExtra("parry"), intent.getStringExtra("items"), intent.getStringExtra("ground")), config)
    }
}

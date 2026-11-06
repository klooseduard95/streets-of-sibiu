package ro.klooseduard.streetsofsibiu.logic.world

data class Rgba(val r: Double, val g: Double, val b: Double, val a: Double) {
    fun withAlpha(alpha: Double) = copy(a = alpha)

    companion object {
        val WHITE = Rgba(1.0, 1.0, 1.0, 1.0)
        val DASH_BLUE = Rgba(0.3, 0.5, 1.0, 0.6)
        val PARRY_GREEN = Rgba(0.0, 1.0, 0.0, 1.0)
        val PARRY_YELLOW = Rgba(1.0, 0.8, 0.0, 1.0)
        val CHARGE_RED = Rgba(2.0, 0.5, 0.5, 1.0)
        val GHOST_GREY = Rgba(0.31061712, 0.31061712, 0.3106171, 1.0)

        fun lerp(from: Rgba, to: Rgba, weight: Float): Rgba {
            val w = weight.toDouble()
            return Rgba(
                from.r + (to.r - from.r) * w, from.g + (to.g - from.g) * w,
                from.b + (to.b - from.b) * w, from.a + (to.a - from.a) * w,
            )
        }
    }
}

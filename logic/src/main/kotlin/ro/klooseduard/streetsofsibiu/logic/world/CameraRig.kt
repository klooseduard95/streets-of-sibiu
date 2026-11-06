package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.GodotProjectSettings
import ro.klooseduard.streetsofsibiu.logic.math.Rect
import ro.klooseduard.streetsofsibiu.logic.math.Vec2

class CameraRig(
    val screenWidth: Float = GodotProjectSettings.VIEWPORT_WIDTH.toFloat(),
    val screenHeight: Float = GodotProjectSettings.VIEWPORT_HEIGHT.toFloat(),
) {
    var smoothingEnabled = true
    var smoothingSpeed = 5f
    var limitSmoothed = true

    var limitLeft = -10_000_000
    var limitTop = -10_000_000
    var limitRight = 10_000_000
    var limitBottom = 10_000_000

    private var first = true
    private var cameraPos = Vec2.ZERO
    private var smoothedPos = Vec2.ZERO

    var screenCenter = Vec2.ZERO
        private set

    val screenTopLeft: Vec2 get() = Vec2(screenCenter.x - screenWidth * 0.5f, screenCenter.y - screenHeight * 0.5f)

    fun enterTree() {
        first = true
    }

    fun setLimitsFromModuleBox(box: Rect) {
        val c = box.center
        limitTop = (c.y - box.height / 2).toInt()
        limitBottom = (c.y + box.height / 2).toInt()
        limitRight = (c.x + box.width / 2).toInt()
        limitLeft = (c.x - box.width / 2).toInt()
    }

    fun resetSmoothing(target: Vec2, delta: Float) {
        update(target, delta)
        smoothedPos = cameraPos
    }

    fun update(target: Vec2, delta: Float) {
        val w = screenWidth
        val h = screenHeight
        val ret: Vec2
        if (!first) {
            var cx = target.x
            var cy = target.y
            if (limitSmoothed && smoothingEnabled) {
                val sx = cx - w * 0.5f
                val sy = cy - h * 0.5f
                if (limitLeft > limitRight - w) {
                    cx -= sx + (w - limitRight - limitLeft) / 2
                } else if (sx < limitLeft) {
                    cx -= sx - limitLeft
                } else if (sx + w > limitRight) {
                    cx -= sx + w - limitRight
                }
                if (limitTop > limitBottom - h) {
                    cy -= sy + (h - limitBottom - limitTop) / 2
                } else if (sy < limitTop) {
                    cy -= sy - limitTop
                } else if (sy + h > limitBottom) {
                    cy -= sy + h - limitBottom
                }
            }
            cameraPos = Vec2(cx, cy)
            if (smoothingEnabled) {
                val c = smoothingSpeed * delta
                smoothedPos = Vec2((cameraPos.x - smoothedPos.x) * c + smoothedPos.x, (cameraPos.y - smoothedPos.y) * c + smoothedPos.y)
                ret = smoothedPos
            } else {
                smoothedPos = cameraPos
                ret = cameraPos
            }
        } else {
            cameraPos = target
            smoothedPos = target
            ret = target
            first = false
        }

        var left = ret.x - w * 0.5f
        var top = ret.y - h * 0.5f
        if (!smoothingEnabled || !limitSmoothed) {
            if (limitLeft > limitRight - w) {
                left = (limitLeft + limitRight - w) / 2
            } else if (left < limitLeft) {
                left = limitLeft.toFloat()
            } else if (left + w > limitRight) {
                left = limitRight - w
            }
            if (limitTop > limitBottom - h) {
                top = (limitTop + limitBottom - h) / 2
            } else if (top < limitTop) {
                top = limitTop.toFloat()
            } else if (top + h > limitBottom) {
                top = limitBottom - h
            }
        }
        screenCenter = Vec2(left + w * 0.5f, top + h * 0.5f)
    }
}

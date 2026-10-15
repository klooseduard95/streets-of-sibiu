package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.GodotProjectSettings
import ro.klooseduard.streetsofsibiu.logic.math.Rect
import ro.klooseduard.streetsofsibiu.logic.math.Vec2

/**
 * Port of Godot 4.6.1 `Camera2D::get_camera_transform` for the configuration the game uses:
 * anchor DRAG_CENTER without drag margins, zoom 1, rotation ignored, no offset.
 * `CoolCamera` in MainBackground.tscn: smoothing on (speed 5), smoothed limits, physics callback,
 * process mode ALWAYS. The node's scale (3.92 in the scene) is ignored by Godot (V4).
 *
 * Call [update] once per physics tick after the target (the player) has moved; Godot runs it in
 * the camera's internal physics process, which comes after its parent's `_physics_process`.
 * All arithmetic is 32-bit float, like Godot's `real_t`.
 */
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

    /** Godot `get_screen_center_position()`: the world point at the centre of the screen. */
    var screenCenter = Vec2.ZERO
        private set

    /** World coordinate of the screen's top-left corner (the canvas transform origin). */
    val screenTopLeft: Vec2 get() = Vec2(screenCenter.x - screenWidth * 0.5f, screenCenter.y - screenHeight * 0.5f)

    /** Godot NOTIFICATION_ENTER_TREE: the next update snaps to the target. */
    fun enterTree() {
        first = true
    }

    /**
     * `MainBackground.updateCameraAnchor`: limits from the room's module box. The float
     * expressions are assigned to Godot's int limit properties, which truncates toward zero (V13).
     */
    fun setLimitsFromModuleBox(box: Rect) {
        val c = box.center
        limitTop = (c.y - box.height / 2).toInt()
        limitBottom = (c.y + box.height / 2).toInt()
        limitRight = (c.x + box.width / 2).toInt()
        limitLeft = (c.x - box.width / 2).toInt()
    }

    /** `Camera2D.reset_smoothing()`: one update, then the smoothed position snaps to the target. */
    fun resetSmoothing(target: Vec2, delta: Float) {
        update(target, delta)
        smoothedPos = cameraPos
    }

    /** One `get_camera_transform()` evaluation with [target] = the camera's global position. */
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
            // bottom_right_corner - position == screen size here (zoom 1, no rotation).
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

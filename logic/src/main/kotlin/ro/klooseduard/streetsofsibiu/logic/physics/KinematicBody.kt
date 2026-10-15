package ro.klooseduard.streetsofsibiu.logic.physics

import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import kotlin.math.PI
import kotlin.math.acos

/** Godot `CharacterBody2D.MotionMode`. */
enum class MotionMode { GROUNDED, FLOATING }

/**
 * Port of Godot 4.6.1 `CharacterBody2D.move_and_slide()` (character_body_2d.cpp) and
 * `PhysicsBody2D.move_and_collide()` (cancel-sliding), without moving platforms (the game has none).
 * Defaults are Godot's; the player uses GROUNDED with up = (0, -1), which in a top-down game makes
 * walls below the player "floor" and walls above it "ceiling" (V9 traces verify the effects).
 */
class KinematicBody(
    val shape: BodyShape,
    var position: Vec2,
    var motionMode: MotionMode = MotionMode.GROUNDED,
) {
    var velocity = DVec.ZERO
    var safeMargin = 0.08
    var maxSlides = 4
    var floorSnapLength = 1.0
    var floorMaxAngle = PI / 4
    var floorStopOnSlope = true
    var floorBlockOnWall = true
    var floorConstantSpeed = false
    var slideOnCeiling = true
    var wallMinSlideAngle = 15.0 * PI / 180
    private val up = DVec(0.0, -1.0)

    var isOnFloor = false; private set
    var isOnWall = false; private set
    var isOnCeiling = false; private set
    private var floorNormal = DVec.ZERO
    private val motionResults = mutableListOf<MotionResult>()

    private var pos: DVec
        get() = DVec.of(position)
        set(value) { position = value.toVec2() }

    private fun angle(n: DVec, ref: DVec): Double = acos(n.dot(ref).coerceIn(-1.0, 1.0))

    private fun moveAndCollide(motion: DVec, testOnly: Boolean, cancelSliding: Boolean, margin: Double = safeMargin): MotionResult {
        var r = Kinematics.testBodyMotion(world, shape, pos, motion, margin)
        if (cancelSliding) {
            val motionLength = motion.length()
            var precision = 0.001
            var cancel = true
            if (r.collided) {
                precision += motionLength * (r.unsafeFraction - r.safeFraction)
                if (r.depth > margin + precision) cancel = false
            }
            if (cancel) {
                val motionNormal = if (motionLength > CMP_EPSILON) motion * (1.0 / motionLength) else DVec.ZERO
                val projected = r.travel.dot(motionNormal)
                val recovery = r.travel - motionNormal * projected
                if (recovery.length() < margin + precision) {
                    val travel = motionNormal * projected
                    r = r.copy(travel = travel, remainder = motion - travel)
                }
            }
        }
        if (!testOnly) pos += r.travel
        return r
    }

    private fun setCollisionDirection(r: MotionResult) {
        if (motionMode == MotionMode.GROUNDED && angle(r.normal, up) <= floorMaxAngle + FLOOR_ANGLE_THRESHOLD) {
            isOnFloor = true
            floorNormal = r.normal
        } else if (motionMode == MotionMode.GROUNDED && angle(r.normal, up * -1.0) <= floorMaxAngle + FLOOR_ANGLE_THRESHOLD) {
            isOnCeiling = true
        } else {
            isOnWall = true
        }
    }

    private lateinit var world: StaticWorld

    /** Godot `move_and_slide()` with the current [velocity]; returns true if anything was hit. */
    fun moveAndSlide(world: StaticWorld, delta: Double): Boolean {
        this.world = world
        motionResults.clear()
        val wasOnFloor = isOnFloor
        isOnFloor = false
        isOnCeiling = false
        isOnWall = false
        if (motionMode == MotionMode.GROUNDED) slideGrounded(delta, wasOnFloor) else slideFloating(delta)
        return motionResults.isNotEmpty()
    }

    private fun slideGrounded(delta: Double, wasOnFloor: Boolean) {
        var motion = velocity * delta
        val motionSlideUp = motion.slide(up)
        floorNormal = DVec.ZERO
        var slidingEnabled = !floorStopOnSlope
        var canApplyConstantSpeed = slidingEnabled
        var firstSlide = true
        val velDirFacingUp = velocity.dot(up) > 0
        for (iteration in 0 until maxSlides) {
            val r = moveAndCollide(motion, testOnly = false, cancelSliding = !slidingEnabled)
            var collided = r.collided
            if (collided) {
                motionResults += r
                setCollisionDirection(r)
                if (isOnFloor && floorStopOnSlope && (velocity.normalized() + up).length() < 0.01) {
                    if (r.travel.length() <= safeMargin + CMP_EPSILON) pos -= r.travel
                    velocity = DVec.ZERO
                    motion = DVec.ZERO
                    break
                }
                if (r.remainder.isZeroApprox()) {
                    motion = DVec.ZERO
                    break
                }
                if (floorBlockOnWall && isOnWall && motionSlideUp.dot(r.normal) <= 0) {
                    if (wasOnFloor && !isOnFloor && !velDirFacingUp) {
                        if (r.travel.length() <= safeMargin + CMP_EPSILON) pos -= r.travel
                        snapOnFloor(true, false, wallAsFloor = true)
                        velocity = DVec.ZERO
                        motion = DVec.ZERO
                        break
                    } else if (!isOnFloor) {
                        motion = up * up.dot(r.remainder)
                        motion = motion.slide(r.normal)
                    } else {
                        motion = r.remainder
                    }
                } else if (floorConstantSpeed && isOnFloor && !isOnWall && !isOnCeiling && canApplyConstantSpeed && wasOnFloor && motion.dot(r.normal) < 0) {
                    canApplyConstantSpeed = false
                    val norm = r.remainder.slide(r.normal).normalized()
                    motion = norm * (motionSlideUp.length() - r.travel.slide(up).length())
                } else if ((slidingEnabled || !isOnFloor) && (!isOnCeiling || slideOnCeiling || !velDirFacingUp)) {
                    val slideMotion = r.remainder.slide(r.normal)
                    motion = if (slideMotion.dot(velocity) > 0.0) slideMotion else DVec.ZERO
                    if (slideOnCeiling && isOnCeiling) {
                        velocity = if (velDirFacingUp) velocity.slide(r.normal) else up * up.dot(velocity)
                    }
                } else {
                    motion = r.remainder
                    if (isOnCeiling && !slideOnCeiling && velDirFacingUp) {
                        velocity = velocity.slide(up)
                        motion = motion.slide(up)
                    }
                }
            }
            canApplyConstantSpeed = !canApplyConstantSpeed && !slidingEnabled
            slidingEnabled = true
            firstSlide = false
            if (!collided || motion.isZeroApprox()) break
        }
        snapOnFloor(wasOnFloor, velDirFacingUp)
        if (isOnWall && !isOnFloor && !isOnCeiling && motionSlideUp.dot(motionResults[0].normal) < 0) {
            val slideMotion = velocity.slide(motionResults[0].normal)
            velocity = if (motionSlideUp.dot(slideMotion) < 0) up * up.dot(velocity)
            else up * up.dot(velocity) + slideMotion.slide(up)
        }
        if (isOnFloor && !velDirFacingUp) velocity = velocity.slide(up)
    }

    private fun snapOnFloor(wasOnFloor: Boolean, velDirFacingUp: Boolean, wallAsFloor: Boolean = false) {
        if (isOnFloor || !wasOnFloor || velDirFacingUp) return
        val length = maxOf(floorSnapLength, safeMargin)
        val r = moveAndCollide(up * -length, testOnly = true, cancelSliding = false)
        if (r.collided) {
            if (angle(r.normal, up) <= floorMaxAngle + FLOOR_ANGLE_THRESHOLD ||
                (wallAsFloor && angle(r.normal, up * -1.0) > floorMaxAngle + FLOOR_ANGLE_THRESHOLD)
            ) {
                isOnFloor = true
                floorNormal = r.normal
                val travel = if (r.travel.length() > safeMargin) up * up.dot(r.travel) else DVec.ZERO
                pos += travel
            }
        }
    }

    private fun slideFloating(delta: Double) {
        var motion = velocity * delta
        var firstSlide = true
        for (iteration in 0 until maxSlides) {
            val r = moveAndCollide(motion, testOnly = false, cancelSliding = false)
            if (r.collided) {
                motionResults += r
                setCollisionDirection(r)
                if (r.remainder.isZeroApprox()) {
                    motion = DVec.ZERO
                    break
                }
                motion = if (wallMinSlideAngle != 0.0 && angle(r.normal, velocity.normalized() * -1.0) < wallMinSlideAngle + FLOOR_ANGLE_THRESHOLD) {
                    DVec.ZERO
                } else if (firstSlide) {
                    r.remainder.slide(r.normal).normalized() * (motion.length() - r.travel.length())
                } else {
                    r.remainder.slide(r.normal)
                }
                if (motion.dot(velocity) <= 0.0) motion = DVec.ZERO
            }
            if (!r.collided || motion.isZeroApprox()) break
            firstSlide = false
        }
    }

    companion object {
        private const val FLOOR_ANGLE_THRESHOLD = 0.01
    }
}

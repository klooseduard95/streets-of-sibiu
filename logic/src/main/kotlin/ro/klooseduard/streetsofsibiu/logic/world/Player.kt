package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.data.PlayerDefs
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.physics.BodyShape
import ro.klooseduard.streetsofsibiu.logic.physics.DVec
import ro.klooseduard.streetsofsibiu.logic.physics.KinematicBody
import ro.klooseduard.streetsofsibiu.logic.physics.StaticWorld
import ro.klooseduard.streetsofsibiu.logic.runtime.ActionInput
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The player (player.gd). D1 scope: walking (8-way digital input, normalised, BASE_SPEED × speed
 * stat), the walk animations and `move_and_slide` against the room. Dash, parry, weapons, damage
 * and inventory follow in Phases 6–7.
 */
class Player(start: Vec2) {
    val body = KinematicBody(BodyShape(PlayerDefs.CAPSULE_RADIUS, PlayerDefs.CAPSULE_HEIGHT), start)
    val animator = FrameAnimator(PlayerDefs.WALK_ANIMATIONS, PlayerDefs.WALK_ANIMATION_STEP, PlayerDefs.WALK_ANIMATION_LENGTH)

    /** `var speed` (the base value; dashes and parries modify it later). */
    var speed = PlayerDefs.BASE_SPEED

    /** `get_stat("speed", 1.0)`: base 1 plus modifiers (none yet in D1). */
    var speedStat = 1.0

    var hp = PlayerDefs.MAX_HP
    var maxHp = PlayerDefs.MAX_HP
    var shield = 0.0

    val position: Vec2 get() = body.position

    /** The walking part of `_physics_process` (not sliding). */
    fun physicsProcess(input: ActionInput<Action>, world: StaticWorld, delta: Double) {
        var dx = 0.0
        var dy = 0.0
        if (input.isPressed(Action.MOVE_FORWARD)) dy -= 1
        if (input.isPressed(Action.MOVE_BACKWARD)) dy += 1
        if (input.isPressed(Action.MOVE_LEFTWARD)) dx -= 1
        if (input.isPressed(Action.MOVE_RIGHTWARD)) dx += 1
        val len = sqrt(dx * dx + dy * dy)
        if (len > 0) {
            dx /= len
            dy /= len
            body.velocity = DVec(dx * speedStat * speed, dy * speedStat * speed)
            updateAnimation(dx, dy)
        } else {
            body.velocity = DVec.ZERO
            animator.stop()
        }
        body.moveAndSlide(world, delta)
    }

    /** `_process`-time work: the AnimationPlayer advances in idle frames. */
    fun process(delta: Double) = animator.process(delta)

    private fun updateAnimation(dx: Double, dy: Double) {
        if (abs(dx) > abs(dy)) {
            animator.play(if (dx > 0) "walk_right" else "walk_left")
        } else {
            animator.play(if (dy > 0) "walk_down" else "walk_up")
        }
    }
}

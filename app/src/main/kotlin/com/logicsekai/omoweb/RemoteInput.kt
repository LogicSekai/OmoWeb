// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

/**
 * What a remote button means in OmoWeb. Sources: OmoAuto's Controls screen (rotary previous/next,
 * left, select, right), the motorcycle's buttons, and media keys, all of which reach this
 * activity through Android Auto as key events or rotary scroll motion.
 */
enum class RemoteAction { UP, DOWN, LEFT, RIGHT, SELECT, PLAY_PAUSE, PLAY, PAUSE, REWIND, FAST_FORWARD }

object RemoteInput {
    // Values of android.view.KeyEvent.KEYCODE_* (kept here so this file stays plain Kotlin).
    const val DPAD_UP = 19
    const val DPAD_DOWN = 20
    const val DPAD_LEFT = 21
    const val DPAD_RIGHT = 22
    const val DPAD_CENTER = 23
    const val ENTER = 66
    const val NUMPAD_ENTER = 160
    const val BUTTON_A = 96
    const val HEADSETHOOK = 79
    const val MEDIA_PLAY_PAUSE = 85
    const val MEDIA_NEXT = 87
    const val MEDIA_PREVIOUS = 88
    const val MEDIA_REWIND = 89
    const val MEDIA_FAST_FORWARD = 90
    const val MEDIA_PLAY = 126
    const val MEDIA_PAUSE = 127
    const val NAVIGATE_PREVIOUS = 260
    const val NAVIGATE_NEXT = 261

    /** Maps a key code to an action; null = not ours (Back, Home, typing, ...). */
    fun fromKey(keyCode: Int): RemoteAction? = when (keyCode) {
        DPAD_UP, NAVIGATE_PREVIOUS -> RemoteAction.UP
        DPAD_DOWN, NAVIGATE_NEXT -> RemoteAction.DOWN
        DPAD_LEFT -> RemoteAction.LEFT
        DPAD_RIGHT -> RemoteAction.RIGHT
        DPAD_CENTER, ENTER, NUMPAD_ENTER, BUTTON_A -> RemoteAction.SELECT
        MEDIA_PLAY_PAUSE, HEADSETHOOK -> RemoteAction.PLAY_PAUSE
        MEDIA_PLAY -> RemoteAction.PLAY
        MEDIA_PAUSE -> RemoteAction.PAUSE
        MEDIA_PREVIOUS, MEDIA_REWIND -> RemoteAction.REWIND
        MEDIA_NEXT, MEDIA_FAST_FORWARD -> RemoteAction.FAST_FORWARD
        else -> null
    }

    /**
     * Rotary knob / scroll wheel. [scroll] is MotionEvent.AXIS_SCROLL (rotary encoder,
     * positive = clockwise = next) and [vscroll] AXIS_VSCROLL (wheel, positive = away = up).
     */
    fun fromScroll(scroll: Float, vscroll: Float): RemoteAction? = when {
        scroll > 0f -> RemoteAction.DOWN
        scroll < 0f -> RemoteAction.UP
        vscroll > 0f -> RemoteAction.UP
        vscroll < 0f -> RemoteAction.DOWN
        else -> null
    }
}

/**
 * Pointer moved by the remote. Repeated presses in the same direction speed it up so crossing
 * the screen takes a few clicks; it stops at the edges and reports how far it was pushed past
 * them so the page can scroll instead.
 */
class RemotePointer(private val baseStep: Float, private val maxBoost: Int = 4, private val repeatWindowMs: Long = 450) {

    data class Move(val overflowX: Float, val overflowY: Float)

    var x = 0f
        private set
    var y = 0f
        private set
    private var width = 0f
    private var height = 0f
    private var lastDirection: RemoteAction? = null
    private var lastAt = Long.MIN_VALUE / 2
    private var boost = 1

    fun setBounds(width: Float, height: Float) {
        val first = this.width == 0f && this.height == 0f
        this.width = width
        this.height = height
        if (first) center() else clamp()
    }

    fun center() {
        x = width / 2f
        y = height / 2f
    }

    fun moveTo(newX: Float, newY: Float) {
        x = newX
        y = newY
        clamp()
    }

    /** Moves by ([dx], [dy]) pixels (trackpad); returns the part that went past an edge. */
    fun moveBy(dx: Float, dy: Float): Move {
        val wantX = x + dx
        val wantY = y + dy
        moveTo(wantX, wantY)
        return Move(wantX - x, wantY - y)
    }

    /** Moves for a direction action at time [nowMs]; returns the part that went past an edge. */
    fun move(direction: RemoteAction, nowMs: Long): Move {
        boost = if (direction == lastDirection && nowMs - lastAt <= repeatWindowMs) (boost + 1).coerceAtMost(maxBoost) else 1
        lastDirection = direction
        lastAt = nowMs
        val step = baseStep * boost
        val (dx, dy) = when (direction) {
            RemoteAction.UP -> 0f to -step
            RemoteAction.DOWN -> 0f to step
            RemoteAction.LEFT -> -step to 0f
            RemoteAction.RIGHT -> step to 0f
            else -> return Move(0f, 0f)
        }
        val wantX = x + dx
        val wantY = y + dy
        moveTo(wantX, wantY)
        return Move(wantX - x, wantY - y)
    }

    private fun clamp() {
        x = x.coerceIn(0f, (width - 1f).coerceAtLeast(0f))
        y = y.coerceIn(0f, (height - 1f).coerceAtLeast(0f))
    }
}

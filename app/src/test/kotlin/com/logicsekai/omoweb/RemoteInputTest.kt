// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemoteInputTest {

    @Test
    fun `OmoAuto controls and media keys map to actions`() {
        assertEquals(RemoteAction.LEFT, RemoteInput.fromKey(RemoteInput.DPAD_LEFT))
        assertEquals(RemoteAction.RIGHT, RemoteInput.fromKey(RemoteInput.DPAD_RIGHT))
        assertEquals(RemoteAction.SELECT, RemoteInput.fromKey(RemoteInput.DPAD_CENTER))
        assertEquals(RemoteAction.UP, RemoteInput.fromKey(RemoteInput.NAVIGATE_PREVIOUS))
        assertEquals(RemoteAction.DOWN, RemoteInput.fromKey(RemoteInput.NAVIGATE_NEXT))
        assertEquals(RemoteAction.PLAY_PAUSE, RemoteInput.fromKey(RemoteInput.MEDIA_PLAY_PAUSE))
        assertNull(RemoteInput.fromKey(4)) // Back stays with the system
        assertNull(RemoteInput.fromKey(3)) // Home stays with Android Auto
    }

    @Test
    fun `rotary clockwise moves down and wheel away moves up`() {
        assertEquals(RemoteAction.DOWN, RemoteInput.fromScroll(1f, 0f))
        assertEquals(RemoteAction.UP, RemoteInput.fromScroll(-1f, 0f))
        assertEquals(RemoteAction.UP, RemoteInput.fromScroll(0f, 1f))
        assertNull(RemoteInput.fromScroll(0f, 0f))
    }

    @Test
    fun `pointer accelerates on repeats, stops at edges and reports overflow`() {
        val p = RemotePointer(baseStep = 10f)
        p.setBounds(200f, 100f)
        assertEquals(100f to 50f, p.x to p.y)

        p.move(RemoteAction.RIGHT, 0)      // +10
        p.move(RemoteAction.RIGHT, 100)    // +20
        p.move(RemoteAction.RIGHT, 200)    // +30
        assertEquals(160f, p.x)
        p.move(RemoteAction.RIGHT, 2_000)  // slow again: +10
        assertEquals(170f, p.x)

        val up = p.move(RemoteAction.UP, 3_000) // 50 -> 40
        assertEquals(0f, up.overflowY)
        repeat(3) { p.move(RemoteAction.UP, 3_100L + it * 100) } // -20, -30 -> clamps at 0
        assertEquals(0f, p.y)
        val push = p.move(RemoteAction.UP, 3_400) // boost 4 = -40, all overflow
        assertEquals(-40f, push.overflowY)
        assertEquals(0f, p.y)
    }

    @Test
    fun `trackpad moves by pixels and clamps at the edges`() {
        val p = RemotePointer(baseStep = 10f)
        p.setBounds(200f, 100f)
        p.moveBy(30f, -20f)
        assertEquals(130f to 30f, p.x to p.y)
        val push = p.moveBy(100f, 0f)
        assertEquals(199f, p.x)
        assertEquals(31f, push.overflowX)
    }
}

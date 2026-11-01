// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

import android.app.BroadcastOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper

/**
 * Direct link between OmoWeb and OmoAuto on the same phone.
 *
 * Android Auto keeps the head unit's buttons and rotary knob for its own UI, so key events from
 * OmoAuto's Controls never reach an app shown on the car screen. Instead:
 *  - while OmoWeb is on the car screen it tells OmoAuto so ([ACTION_WEB_STATE], repeated every
 *    few seconds as a heartbeat);
 *  - OmoAuto then sends its Controls buttons straight to OmoWeb ([ACTION_REMOTE]).
 *
 * Both directions are explicit broadcasts (setPackage) that carry the sender's identity; each side
 * only accepts the other's packages. Protocol: docs in OmoAuto `docs/OMOWEB-LINK.md`.
 */
/** [onCommand] gets the command and, for trackpad commands, dx / dy as fractions of the pad. */
class OmoAutoLink(private val context: Context, private val onCommand: (String, Float, Float) -> Unit) {

    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_REMOTE) return
            if (sentFromPackage !in OMOAUTO_PACKAGES) return
            val command = intent.getStringExtra(EXTRA_COMMAND) ?: return
            onCommand(command, intent.getFloatExtra(EXTRA_DX, 0f), intent.getFloatExtra(EXTRA_DY, 0f))
        }
    }

    private val heartbeat = object : Runnable {
        override fun run() {
            if (!active) return
            sendState(true)
            handler.postDelayed(this, HEARTBEAT_MS)
        }
    }

    /** Call from onStart/onStop; [onCarScreen] = the activity is on Android Auto's display. */
    fun setActive(value: Boolean) {
        if (value == active) return
        active = value
        if (value) {
            if (!registered) {
                context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE), Context.RECEIVER_EXPORTED)
                registered = true
            }
            handler.post(heartbeat)
        } else {
            handler.removeCallbacks(heartbeat)
            sendState(false)
            if (registered) {
                runCatching { context.unregisterReceiver(receiver) }
                registered = false
            }
        }
    }

    private fun sendState(isActive: Boolean) {
        val options = BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle()
        OMOAUTO_PACKAGES.forEach { pkg ->
            val intent = Intent(ACTION_WEB_STATE).setPackage(pkg).putExtra(EXTRA_ACTIVE, isActive)
            runCatching { context.sendBroadcast(intent, null, options) }
        }
    }

    companion object {
        const val ACTION_REMOTE = "com.logicsekai.omoweb.action.REMOTE"
        const val ACTION_WEB_STATE = "com.logicsekai.omoauto.action.WEB_STATE"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_ACTIVE = "active"
        const val EXTRA_DX = "dx"
        const val EXTRA_DY = "dy"
        const val HEARTBEAT_MS = 4_000L
        val OMOAUTO_PACKAGES = setOf("com.logicsekai.omoauto", "com.logicsekai.omoauto.debug")

        /** Commands OmoAuto sends, as [RemoteAction]s (back is handled separately). */
        fun actionFor(command: String): RemoteAction? = when (command) {
            "up" -> RemoteAction.UP
            "down" -> RemoteAction.DOWN
            "left" -> RemoteAction.LEFT
            "right" -> RemoteAction.RIGHT
            "select" -> RemoteAction.SELECT
            "play_pause" -> RemoteAction.PLAY_PAUSE
            else -> null
        }
    }
}

// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import org.json.JSONObject

/**
 * Makes the web video a regular Android media player: a media session (so media buttons from
 * OmoAuto, headsets and the system media controls reach it) and a media notification with the
 * video title. Playback commands come back as strings for [WebScripts.video] / navigation:
 * "play", "pause", "next", "prev", "rewind", "forward", "seekto:<seconds>".
 */
class WebMediaSession(private val activity: Activity, private val onCommand: (String) -> Unit) {

    private val notifications = activity.getSystemService(NotificationManager::class.java)
    private var lastTitle = ""

    private val session = MediaSession(activity, "OmoWeb").apply {
        setCallback(object : MediaSession.Callback() {
            override fun onPlay() = onCommand("play")
            override fun onPause() = onCommand("pause")
            override fun onStop() = onCommand("pause")
            override fun onSkipToNext() = onCommand("next")
            override fun onSkipToPrevious() = onCommand("prev")
            override fun onRewind() = onCommand("rewind")
            override fun onFastForward() = onCommand("forward")
            override fun onSeekTo(pos: Long) = onCommand("seekto:${pos / 1000}")
        })
    }

    /** State reported by [WebScripts.MEDIA_REPORT]: playing, title, artist, position, duration (s). */
    fun update(json: JSONObject) {
        val playing = json.optBoolean("playing")
        val title = json.optString("title").ifBlank { activity.getString(R.string.app_name) }
        val artist = json.optString("artist")
        val positionMs = (json.optDouble("position", 0.0) * 1000).toLong()
        val durationMs = (json.optDouble("duration", 0.0) * 1000).toLong()

        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)
                .build(),
        )
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_REWIND or PlaybackState.ACTION_FAST_FORWARD or PlaybackState.ACTION_SEEK_TO,
                )
                .setState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, positionMs, if (playing) 1f else 0f)
                .build(),
        )
        session.isActive = true
        if (playing || title != lastTitle) postNotification(title, artist, playing)
        lastTitle = title
    }

    private fun postNotification(title: String, artist: String, playing: Boolean) {
        val manager = notifications ?: return
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, activity.getString(R.string.media_channel), NotificationManager.IMPORTANCE_LOW),
            )
        }
        val open = PendingIntent.getActivity(
            activity, 0,
            Intent(activity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(activity, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(open)
            .setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken))
            .build()
        // Without the notification permission this is silently dropped; the session still works.
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    fun release() {
        runCatching { notifications?.cancel(NOTIFICATION_ID) }
        session.isActive = false
        session.release()
    }

    private companion object {
        const val CHANNEL = "playback"
        const val NOTIFICATION_ID = 1
    }
}

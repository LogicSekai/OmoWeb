// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

/**
 * Google sign-in inside the WebView.
 *
 * Google refuses sign-in from embedded browsers ("This browser or app may not be secure") when it
 * recognises a WebView: the "; wv" token and "Version/4.0" in the user agent, and the
 * X-Requested-With header with the app's package name. OmoWeb sends a plain Chrome user agent and
 * suppresses that header. If Google still rejects the sign-in, OmoWeb retries once with the
 * desktop Chrome user agent. The session (cookies) is kept by the WebView across launches and is
 * shared between the phone screen and the motorcycle screen.
 */
object GoogleLogin {

    /** Sign-in page that returns to YouTube. */
    const val SIGN_IN_URL = "https://accounts.google.com/ServiceLogin?service=youtube&passive=true&continue=https%3A%2F%2Fm.youtube.com%2F"

    /** WebView's default user agent made to look like mobile Chrome. */
    fun chromeUserAgent(webViewDefault: String): String = webViewDefault
        .replace("; wv)", ")")
        .replace(Regex("""Version/\d+(\.\d+)* """), "")

    /** Google's "couldn't sign you in / browser not secure" pages. */
    fun isRejected(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("accounts.google.com") && (u.contains("/signin/rejected") || u.contains("disallowed_useragent"))
    }

    /** Whether the YouTube cookies show a signed-in Google account. */
    fun isSignedIn(youtubeCookies: String?): Boolean {
        if (youtubeCookies.isNullOrBlank()) return false
        return youtubeCookies.split(';').map { it.trim().substringBefore('=') }
            .any { it == "SAPISID" || it == "__Secure-3PAPISID" || it == "LOGIN_INFO" }
    }
}

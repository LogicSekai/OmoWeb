// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

import java.net.URLEncoder

/** Turns what the rider typed into a URL: a web address as is, anything else as a search. */
object WebUrls {
    const val HOME_URL = "https://m.youtube.com/"

    /** YouTube home in the app's language ("id", "en"; null = YouTube decides). */
    fun homeUrl(language: String?): String = if (language.isNullOrBlank()) HOME_URL else "$HOME_URL?hl=$language"

    /** Pages worth reopening later (not sign-in pages or blank pages). */
    fun isResumable(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val u = url.lowercase()
        return (u.startsWith("https://") || u.startsWith("http://")) && !u.contains("accounts.google.com")
    }
    private const val SEARCH_URL = "https://www.google.com/search?q="

    fun resolve(raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val lower = text.lowercase()
        return when {
            lower.startsWith("http://") || lower.startsWith("https://") -> text
            ' ' !in text && looksLikeHost(lower) -> "https://$text"
            else -> SEARCH_URL + URLEncoder.encode(text, "UTF-8")
        }
    }

    private fun looksLikeHost(text: String): Boolean {
        val host = text.substringBefore('/').substringBefore(':')
        if (host == "localhost") return true
        val dot = host.lastIndexOf('.')
        return dot in 1 until host.length - 2 && host.all { it.isLetterOrDigit() || it == '.' || it == '-' }
    }
}

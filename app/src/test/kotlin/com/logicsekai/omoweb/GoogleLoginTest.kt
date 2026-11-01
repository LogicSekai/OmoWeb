// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoogleLoginTest {
    @Test
    fun `webview markers are removed from the user agent`() {
        val wv = "Mozilla/5.0 (Linux; Android 16; 2306EPN60G Build/BP2A; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/141.0.7390.122 Mobile Safari/537.36"
        assertEquals(
            "Mozilla/5.0 (Linux; Android 16; 2306EPN60G Build/BP2A) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.7390.122 Mobile Safari/537.36",
            GoogleLogin.chromeUserAgent(wv),
        )
    }

    @Test
    fun `rejected sign-in pages and signed-in cookies are recognised`() {
        assertTrue(GoogleLogin.isRejected("https://accounts.google.com/v3/signin/rejected?continue=x"))
        assertFalse(GoogleLogin.isRejected("https://accounts.google.com/v3/signin/identifier"))
        assertTrue(GoogleLogin.isSignedIn("YSC=abc; SAPISID=xyz/abc; PREF=f6=40000000"))
        assertFalse(GoogleLogin.isSignedIn("YSC=abc; VISITOR_INFO1_LIVE=q"))
        assertFalse(GoogleLogin.isSignedIn(null))
    }

    @Test
    fun `home url follows the app language and sign-in pages are not resumed`() {
        assertEquals("https://m.youtube.com/?hl=id", WebUrls.homeUrl("id"))
        assertEquals(WebUrls.HOME_URL, WebUrls.homeUrl(null))
        assertTrue(WebUrls.isResumable("https://m.youtube.com/watch?v=1"))
        assertFalse(WebUrls.isResumable("https://accounts.google.com/ServiceLogin"))
        assertFalse(WebUrls.isResumable("about:blank"))
    }
}

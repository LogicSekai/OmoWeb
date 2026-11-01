// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebUrlsTest {
    @Test
    fun `addresses open directly and other text becomes a search`() {
        assertEquals("https://youtube.com/watch?v=1", WebUrls.resolve("  https://youtube.com/watch?v=1 "))
        assertEquals("https://m.youtube.com", WebUrls.resolve("m.youtube.com"))
        assertEquals("https://example.co.id/path", WebUrls.resolve("example.co.id/path"))
        assertEquals("https://www.google.com/search?q=lagu+santai", WebUrls.resolve("lagu santai"))
        assertEquals("https://www.google.com/search?q=youtube", WebUrls.resolve("youtube"))
        assertNull(WebUrls.resolve("   "))
    }
}

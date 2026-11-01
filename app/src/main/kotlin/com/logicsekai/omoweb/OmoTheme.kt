// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat

/**
 * OmoWeb's look, shared with OmoAuto: the same orange / navy palette, light and dark variants,
 * and the Poppins font. The phone screen follows the chosen [Mode]; the motorcycle screen is
 * always dark.
 */
object OmoTheme {

    enum class Mode { SYSTEM, LIGHT, DARK }

    /** Colours for one variant (ARGB ints). Names follow OmoAuto's OmoPalette. */
    data class Colors(
        val dark: Boolean,
        val background: Int,
        val surface: Int,
        val card: Int,
        val line: Int,
        val ink: Int,
        val inkMuted: Int,
        val field: Int,
        val accent: Int,
        val accentDeep: Int,
        val accentSoft: Int,
        val onAccent: Int,
        val blue: Int,
        val pink: Int,
        val good: Int,
        val warn: Int,
        val ripple: Int,
    )

    val LIGHT = Colors(
        dark = false,
        background = 0xFFFFFFFF.toInt(),
        surface = 0xFFFFFFFF.toInt(),
        card = 0xFFF6F7FA.toInt(),
        line = 0xFFEEF0F4.toInt(),
        ink = 0xFF2E3560.toInt(),
        inkMuted = 0xFF8A90A6.toInt(),
        field = 0xFFF6F7FA.toInt(),
        accent = 0xFFFF7A4F.toInt(),
        accentDeep = 0xFFEC6436.toInt(),
        accentSoft = 0xFFFFE6DC.toInt(),
        onAccent = 0xFFFFFFFF.toInt(),
        blue = 0xFF255FA0.toInt(),
        pink = 0xFFF48DBE.toInt(),
        good = 0xFF2DBE60.toInt(),
        warn = 0xFFFFB020.toInt(),
        ripple = 0x1F2E3560,
    )

    val DARK = Colors(
        dark = true,
        background = 0xFF141832.toInt(),
        surface = 0xFF1D2245.toInt(),
        card = 0xFF262C55.toInt(),
        line = 0xFF323A6B.toInt(),
        ink = 0xFFF1F3FF.toInt(),
        inkMuted = 0xFFA3A9C9.toInt(),
        field = 0xFF323A6B.toInt(),
        accent = 0xFFFF7A4F.toInt(),
        accentDeep = 0xFFEC6436.toInt(),
        accentSoft = 0x33FF7A4F,
        onAccent = 0xFFFFFFFF.toInt(),
        blue = 0xFF6CCFF6.toInt(),
        pink = 0xFFF48DBE.toInt(),
        good = 0xFF2DBE60.toInt(),
        warn = 0xFFFFB020.toInt(),
        ripple = 0x33FFFFFF,
    )

    private const val PREFS = "omoweb"
    private const val KEY_MODE = "theme_mode"

    fun mode(context: Context): Mode =
        runCatching { Mode.valueOf(prefs(context).getString(KEY_MODE, null) ?: Mode.SYSTEM.name) }.getOrDefault(Mode.SYSTEM)

    fun setMode(context: Context, mode: Mode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }

    fun systemIsDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** Whether the phone screen is dark for the saved mode. The car screen passes [onCar] = true. */
    fun isDark(context: Context, onCar: Boolean = false): Boolean = onCar || when (mode(context)) {
        Mode.SYSTEM -> systemIsDark(context)
        Mode.LIGHT -> false
        Mode.DARK -> true
    }

    fun colors(context: Context, onCar: Boolean = false): Colors = if (isDark(context, onCar)) DARK else LIGHT

    // ---- Poppins (SIL OFL 1.1, same font as OmoAuto) ----

    fun regular(context: Context): Typeface = font(context, R.font.poppins_regular, Typeface.NORMAL)
    fun medium(context: Context): Typeface = font(context, R.font.poppins_medium, Typeface.NORMAL)
    fun semiBold(context: Context): Typeface = font(context, R.font.poppins_semibold, Typeface.BOLD)
    fun bold(context: Context): Typeface = font(context, R.font.poppins_bold, Typeface.BOLD)

    private fun font(context: Context, res: Int, fallbackStyle: Int): Typeface =
        runCatching { ResourcesCompat.getFont(context, res) }.getOrNull() ?: Typeface.create(Typeface.SANS_SERIF, fallbackStyle)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

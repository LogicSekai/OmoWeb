// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.logicsekai.omoweb.update.UpdateManager
import java.util.Locale

/**
 * What OmoWeb shows on the phone before YouTube, styled like OmoAuto: an orange hero card to open
 * YouTube or continue, how to put OmoWeb on the motorcycle, account, appearance (theme and
 * language), app updates, support links and the license notice. On the motorcycle screen OmoWeb
 * skips this and opens YouTube directly.
 */
class PhoneSetupView(
    context: Context,
    private val c: OmoTheme.Colors,
    private val links: Links,
    private val actions: Actions,
) : ScrollView(context) {

    interface Actions {
        fun openYouTube()
        fun resume(url: String)
        fun signIn()
        fun signOut()
        /** "id", "en" or null for the phone's language. */
        fun setLanguage(tag: String?)
        fun setThemeMode(mode: OmoTheme.Mode)
        fun openAndroidAuto()
        fun openUrl(url: String)
        fun checkUpdate()
        fun downloadUpdate()
        fun cancelUpdate()
        fun installUpdate()
        fun setAutoUpdate(enabled: Boolean)
    }

    /** Fixed facts from BuildConfig. A blank URL hides its row. */
    data class Links(
        val versionLabel: String,
        val sourceUrl: String,
        val communityUrl: String,
        val sponsorUrl: String,
        val kofiUrl: String,
        val trakteerUrl: String,
    )

    data class State(
        val signedIn: Boolean,
        val lastUrl: String?,
        val lastTitle: String?,
        val language: String?,
        val themeMode: OmoTheme.Mode,
    )

    /** Everything the update card shows. */
    data class UpdateUi(
        val enabled: Boolean,
        val canSelfInstall: Boolean,
        val state: UpdateManager.State,
        val autoCheck: Boolean,
        val lastCheckMillis: Long,
        val releasesUrl: String,
    )

    private val fontRegular = OmoTheme.regular(context)
    private val fontMedium = OmoTheme.medium(context)
    private val fontSemiBold = OmoTheme.semiBold(context)
    private val fontBold = OmoTheme.bold(context)

    private var lastUrl: String? = null

    // Hero
    private val resumeBlock: LinearLayout
    private val resumeTitle: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dpf(v: Float) = v * resources.displayMetrics.density

    // Account
    private val accountDot = View(context)
    private val accountStatus = label("", 14f, c.ink, fontMedium)
    private val signInButton = pill(context.getString(R.string.setup_sign_in), c.accent, c.onAccent, filled = true) { actions.signIn() }
    private val signOutButton = pill(context.getString(R.string.setup_sign_out), c.inkMuted, c.ink, filled = false) { actions.signOut() }

    // Appearance
    private val themeSegments = Segmented(
        listOf(
            OmoTheme.Mode.SYSTEM.name to context.getString(R.string.theme_system),
            OmoTheme.Mode.LIGHT.name to context.getString(R.string.theme_light),
            OmoTheme.Mode.DARK.name to context.getString(R.string.theme_dark),
        ),
    ) { key -> actions.setThemeMode(OmoTheme.Mode.valueOf(key)) }
    private val languageSegments = Segmented(
        listOf(
            "" to context.getString(R.string.setup_language_system_short),
            "id" to context.getString(R.string.setup_language_id_short),
            "en" to context.getString(R.string.setup_language_en_short),
        ),
    ) { key -> actions.setLanguage(key.ifBlank { null }) }

    // Update
    private val updateCard: LinearLayout
    private val updateStatus = label("", 15f, c.ink, fontSemiBold)
    private val updateDetail = label("", 12f, c.inkMuted, fontRegular).apply { setPadding(0, dp(4), 0, 0); maxLines = 12 }
    private val updateProgress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
    private val updatePrimary = pill("", c.accent, c.onAccent, filled = true) {}
    private val updateSecondary = pill("", c.inkMuted, c.ink, filled = false) {}
    private val updateAuto = Switch(context)
    private var updatingAuto = false
    private val updateAutoRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(14), 0, 0)
        addView(label(context.getString(R.string.update_auto), 13f, c.ink, fontMedium), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(updateAuto.apply {
            thumbTintList = switchThumb()
            trackTintList = switchTrack()
            setOnCheckedChangeListener { _, checked -> if (!updatingAuto) actions.setAutoUpdate(checked) }
        })
    }

    init {
        setBackgroundColor(c.background)
        isFillViewport = true
        overScrollMode = OVER_SCROLL_NEVER
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(32))
        }
        addView(column)

        // ---- Top bar: icon, name, version chip ----
        column.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(16))
            addView(ImageView(context).apply {
                setImageResource(R.mipmap.ic_launcher)
                clipToOutline = true
                outlineProvider = roundOutline(dpf(14f))
            }, LinearLayout.LayoutParams(dp(44), dp(44)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(label(context.getString(R.string.setup_title), 22f, c.ink, fontBold))
                addView(label(context.getString(R.string.setup_tagline), 12f, c.inkMuted, fontRegular))
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(chip(links.versionLabel.substringBefore(' '), c.accentSoft, c.accentDeep))
        })

        // ---- Hero card (orange) ----
        val hero = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(22))
            background = rounded(c.accent, dpf(30f))
        }
        val heroTop = FrameLayout(context)
        heroTop.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(context.getString(R.string.setup_hero_title), 22f, Color.WHITE, fontBold))
            addView(label(context.getString(R.string.setup_subtitle), 13f, 0xE6FFFFFF.toInt(), fontRegular).apply {
                setPadding(0, dp(6), dp(84), 0)
            })
        })
        heroTop.addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_launcher_foreground)
            alpha = 0.95f
        }, FrameLayout.LayoutParams(dp(96), dp(96), Gravity.END or Gravity.CENTER_VERTICAL).apply { marginEnd = -dp(16) })
        hero.addView(heroTop)
        hero.addView(pill(context.getString(R.string.setup_open_youtube), Color.WHITE, c.accentDeep, filled = true) {
            actions.openYouTube()
        }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(18) })
        resumeTitle = label("", 13f, Color.WHITE, fontMedium).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        resumeBlock = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(10), dp(10))
            background = ripple(rounded(0x26FFFFFF, dpf(18f)), dpf(18f))
            isClickable = true
            setOnClickListener { lastUrl?.let(actions::resume) }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(context.getString(R.string.setup_resume_title), 11f, 0xCCFFFFFF.toInt(), fontRegular))
                addView(resumeTitle)
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(label(context.getString(R.string.setup_resume), 13f, Color.WHITE, fontSemiBold).apply { setPadding(dp(8), 0, dp(4), 0) })
            addView(chevron(Color.WHITE))
        }
        hero.addView(resumeBlock, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        column.addView(hero)

        // ---- Show on the motorcycle ----
        sectionHeader(column, R.string.setup_aa_title)
        card(column).apply {
            listOf(R.string.setup_aa_step1, R.string.setup_aa_step2, R.string.setup_aa_step3, R.string.setup_aa_step4, R.string.setup_aa_step5)
                .forEachIndexed { i, step -> addView(stepRow(i + 1, context.getString(step))) }
            addView(pill(context.getString(R.string.setup_aa_open), c.accent, c.onAccent, filled = true) { actions.openAndroidAuto() },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(12) })
        }

        // ---- Google account ----
        sectionHeader(column, R.string.setup_account_title)
        card(column).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(accountDot, LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(10) })
                addView(accountStatus, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            })
            addView(label(context.getString(R.string.setup_account_note), 12f, c.inkMuted, fontRegular).apply { setPadding(0, dp(6), 0, 0) })
            addView(signInButton, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(12) })
            addView(signOutButton, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(12) })
        }

        // ---- Appearance: theme and language ----
        sectionHeader(column, R.string.setup_appearance_title)
        card(column).apply {
            addView(label(context.getString(R.string.setup_theme_title), 13f, c.inkMuted, fontMedium))
            addView(themeSegments, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(8) })
            addView(label(context.getString(R.string.setup_language_title), 13f, c.inkMuted, fontMedium).apply { setPadding(0, dp(16), 0, 0) })
            addView(languageSegments, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(8) })
        }

        // ---- App update ----
        sectionHeader(column, R.string.update_title)
        updateCard = card(column).apply {
            addView(updateStatus)
            addView(updateDetail)
            addView(updateProgress.apply {
                max = 1000
                progressTintList = ColorStateList.valueOf(c.accent)
                progressBackgroundTintList = ColorStateList.valueOf(c.line)
                indeterminateTintList = ColorStateList.valueOf(c.accent)
            }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
            addView(updatePrimary, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(12) })
            addView(updateSecondary, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(8) })
            addView(updateAutoRow)
        }

        // ---- Support ----
        sectionHeader(column, R.string.support_title)
        column.addView(label(context.getString(R.string.support_body), 13f, c.inkMuted, fontRegular).apply { setPadding(dp(2), 0, dp(2), dp(4)) })
        accentRow(column, R.string.support_sponsor, R.string.support_sponsor_caption, links.sponsorUrl, c.pink)
        accentRow(column, R.string.support_kofi, R.string.support_kofi_caption, links.kofiUrl, c.pink)
        accentRow(column, R.string.support_trakteer, R.string.support_trakteer_caption, links.trakteerUrl, c.pink)
        accentRow(column, R.string.support_code, R.string.support_code_caption, links.sourceUrl, c.blue)
        accentRow(column, R.string.support_issue, R.string.support_issue_caption, links.sourceUrl.ifNotBlank { "$it/issues/new/choose" }, c.blue)
        accentRow(column, R.string.support_community, R.string.support_community_caption, links.communityUrl, c.accent)

        // ---- About & license ----
        sectionHeader(column, R.string.about_title)
        card(column).apply {
            addView(label(context.getString(R.string.about_version, links.versionLabel), 15f, c.ink, fontSemiBold))
            listOf(R.string.about_license, R.string.about_credits, R.string.about_disclaimer).forEach { res ->
                addView(label(context.getString(res), 12f, c.inkMuted, fontRegular).apply { setPadding(0, dp(8), 0, 0) })
            }
        }
        accentRow(column, R.string.about_license_button, R.string.about_license_caption, links.sourceUrl.ifNotBlank { "$it/blob/main/LICENSE" }, c.blue)
        accentRow(column, R.string.about_notices_button, R.string.about_notices_caption, links.sourceUrl.ifNotBlank { "$it/blob/main/THIRD-PARTY-NOTICES.md" }, c.blue)

        // ---- Parked-only notice ----
        column.addView(label(context.getString(R.string.parked_only), 13f, c.accentDeep, fontSemiBold).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(c.accentSoft, dpf(16f))
        }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(24) })
    }

    fun update(state: State) {
        accountStatus.setText(if (state.signedIn) R.string.setup_account_signed_in else R.string.setup_account_signed_out)
        accountDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (state.signedIn) c.good else c.inkMuted)
        }
        signInButton.visibility = if (state.signedIn) View.GONE else View.VISIBLE
        signOutButton.visibility = if (state.signedIn) View.VISIBLE else View.GONE
        lastUrl = state.lastUrl
        resumeBlock.visibility = if (state.lastUrl != null) View.VISIBLE else View.GONE
        resumeTitle.text = state.lastTitle?.takeIf { it.isNotBlank() } ?: state.lastUrl
        languageSegments.select(state.language ?: "")
        themeSegments.select(state.themeMode.name)
    }

    /** Shows the update card for [ui]. */
    fun showUpdate(ui: UpdateUi, needsInstallPermission: Boolean = false) {
        updatingAuto = true
        updateAuto.isChecked = ui.autoCheck
        updatingAuto = false

        if (!ui.enabled) {
            updateStatus.setText(R.string.update_disabled)
            updateDetail.visibility = View.GONE
            updateProgress.visibility = View.GONE
            updatePrimary.visibility = View.GONE
            updateSecondary.visibility = View.GONE
            updateAutoRow.visibility = View.GONE
            return
        }
        updateAutoRow.visibility = View.VISIBLE

        fun detail(text: CharSequence?) {
            updateDetail.text = text
            updateDetail.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        fun progress(value: Float?, shown: Boolean) {
            updateProgress.visibility = if (shown) View.VISIBLE else View.GONE
            updateProgress.isIndeterminate = value == null
            if (value != null) updateProgress.progress = (value * 1000).toInt()
        }
        fun primary(label: Int?, onClick: () -> Unit = {}) {
            updatePrimary.visibility = if (label == null) View.GONE else View.VISIBLE
            if (label != null) { updatePrimary.setText(label); updatePrimary.setOnClickListener { onClick() } }
        }
        fun secondary(label: Int?, onClick: () -> Unit = {}) {
            updateSecondary.visibility = if (label == null) View.GONE else View.VISIBLE
            if (label != null) { updateSecondary.setText(label); updateSecondary.setOnClickListener { onClick() } }
        }
        val openPage = { url: String -> actions.openUrl(url.ifBlank { ui.releasesUrl }) }

        when (val s = ui.state) {
            UpdateManager.State.Idle, UpdateManager.State.Checking -> {
                updateStatus.setText(R.string.update_checking)
                detail(null); progress(null, true); primary(null); secondary(null)
            }
            UpdateManager.State.UpToDate -> {
                updateStatus.setText(R.string.update_up_to_date)
                detail(if (ui.lastCheckMillis > 0) context.getString(R.string.update_last_checked, relative(ui.lastCheckMillis)) else null)
                progress(null, false)
                primary(null)
                secondary(R.string.update_check) { actions.checkUpdate() }
            }
            is UpdateManager.State.Available -> {
                val r = s.release
                updateStatus.text = context.getString(R.string.update_available, r.version)
                val size = if (r.apkSize > 0) context.getString(R.string.update_size, megabytes(r.apkSize)) else null
                detail(listOfNotNull(size, r.notes.take(MAX_NOTES).takeIf { it.isNotBlank() }).joinToString("\n\n"))
                progress(null, false)
                if (ui.canSelfInstall && r.apkUrl != null) {
                    primary(R.string.update_download) { actions.downloadUpdate() }
                    secondary(R.string.update_open_page) { openPage(r.pageUrl) }
                } else {
                    primary(R.string.update_open_page) { openPage(r.pageUrl) }
                    secondary(null)
                }
            }
            is UpdateManager.State.Downloading -> {
                updateStatus.text = context.getString(R.string.update_downloading, s.release.version)
                detail(s.progress?.let { "${(it * 100).toInt()}%" })
                progress(s.progress, true)
                primary(null)
                secondary(R.string.update_cancel) { actions.cancelUpdate() }
            }
            is UpdateManager.State.ReadyToInstall -> {
                updateStatus.text = context.getString(R.string.update_ready, s.release.version)
                detail(context.getString(if (needsInstallPermission) R.string.update_needs_permission else R.string.update_ready_body))
                progress(null, false)
                primary(R.string.update_install) { actions.installUpdate() }
                secondary(null)
            }
            is UpdateManager.State.Failed -> {
                updateStatus.setText(R.string.update_failed)
                detail(context.getString(when (s.problem) {
                    UpdateManager.Problem.NETWORK -> R.string.update_failed_network
                    UpdateManager.Problem.CHECKSUM -> R.string.update_failed_checksum
                    UpdateManager.Problem.INVALID_APK -> R.string.update_failed_invalid
                    UpdateManager.Problem.STORAGE -> R.string.update_failed_storage
                }))
                progress(null, false)
                val release = s.release
                if (release != null && ui.canSelfInstall && release.apkUrl != null && s.problem == UpdateManager.Problem.NETWORK) {
                    primary(R.string.update_retry) { actions.downloadUpdate() }
                } else {
                    primary(R.string.update_retry) { actions.checkUpdate() }
                }
                secondary(R.string.update_open_page) { openPage(release?.pageUrl.orEmpty()) }
            }
        }
    }

    /** Scrolls the update card into view (opened from the update notification). */
    fun scrollToUpdate() {
        post { smoothScrollTo(0, (updateCard.top - dp(48)).coerceAtLeast(0)) }
    }

    // ---- building blocks (styled after OmoAuto's design system) ----

    private fun label(text: CharSequence, size: Float, color: Int, font: Typeface) = TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setTextColor(color)
        typeface = font
        includeFontPadding = false
        setLineSpacing(dpf(2f), 1f)
    }

    private fun rounded(color: Int, radius: Float, stroke: Int? = null) = GradientDrawable().apply {
        cornerRadius = radius
        setColor(color)
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun ripple(content: Drawable, radius: Float) =
        RippleDrawable(ColorStateList.valueOf(c.ripple), content, rounded(Color.BLACK, radius))

    private fun roundOutline(radius: Float) = object : android.view.ViewOutlineProvider() {
        override fun getOutline(view: View, outline: android.graphics.Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }

    /** "Show OmoWeb on the motorcycle"-style title above a group (SectionHeader in OmoAuto). */
    private fun sectionHeader(parent: LinearLayout, res: Int) {
        parent.addView(label(context.getString(res), 18f, c.ink, fontSemiBold).apply {
            setPadding(dp(2), dp(28), 0, dp(10))
        })
    }

    /** Soft rounded card (OmoCard in OmoAuto). */
    private fun card(parent: LinearLayout): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(c.card, dpf(24f))
        }
        parent.addView(card, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        return card
    }

    private fun chip(text: String, bg: Int, fg: Int) = label(text, 11f, fg, fontSemiBold).apply {
        setPadding(dp(10), dp(5), dp(10), dp(5))
        background = rounded(bg, dpf(20f))
    }

    /** Rounded pill button: filled (primary) or outlined (secondary). */
    private fun pill(text: String, color: Int, textColor: Int, filled: Boolean, onClick: () -> Unit) = label(text, 15f, textColor, fontSemiBold).apply {
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        val radius = dpf(20f)
        background = ripple(if (filled) rounded(color, radius) else rounded(Color.TRANSPARENT, radius, stroke = c.line), radius)
        setOnClickListener { onClick() }
    }

    /** Numbered step (StepCard in OmoAuto). */
    private fun stepRow(number: Int, text: String) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(6), 0, dp(6))
        addView(label(number.toString(), 13f, c.accentDeep, fontBold).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c.accentSoft) }
        }, LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(12) })
        addView(label(text, 14f, c.ink, fontRegular).apply { setPadding(0, dp(4), 0, 0) }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
    }

    /** Light row with a coloured accent bar, caption and chevron (AccentRow in OmoAuto). */
    private fun accentRow(parent: LinearLayout, title: Int, caption: Int, url: String, accent: Int) {
        if (url.isBlank()) return
        val radius = dpf(20f)
        parent.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(16), dp(14), dp(16))
            background = ripple(rounded(c.card, radius), radius)
            isClickable = true
            isFocusable = true
            setOnClickListener { actions.openUrl(url) }
            addView(View(context).apply { background = rounded(accent, dpf(2f)) }, LinearLayout.LayoutParams(dp(3), dp(28)).apply { marginEnd = dp(18) })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(context.getString(title), 14f, c.ink, fontSemiBold))
                addView(label(context.getString(caption), 12f, c.inkMuted, fontRegular).apply { setPadding(0, dp(3), 0, 0) })
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(chevron(c.inkMuted))
        }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
    }

    private fun chevron(color: Int) = label("›", 24f, color, fontRegular).apply { setPadding(dp(6), 0, 0, dp(2)) }

    private fun switchThumb() = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(c.accent, if (c.dark) 0xFFA3A9C9.toInt() else Color.WHITE),
    )

    private fun switchTrack() = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(0x80FF7A4F.toInt(), c.line),
    )

    /** Segmented choice (theme, language). */
    private inner class Segmented(
        private val options: List<Pair<String, String>>,
        private val onSelect: (String) -> Unit,
    ) : LinearLayout(context) {
        private val items = mutableListOf<TextView>()
        private var selected: String? = null

        init {
            orientation = HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = rounded(if (c.dark) c.surface else Color.WHITE, dpf(16f), stroke = c.line)
            options.forEach { (key, text) ->
                val item = label(text, 13f, c.ink, fontMedium).apply {
                    gravity = Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setOnClickListener {
                        if (selected == key) return@setOnClickListener
                        select(key)
                        onSelect(key)
                    }
                }
                items += item
                addView(item, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            }
        }

        fun select(key: String) {
            selected = key
            options.forEachIndexed { i, (k, _) ->
                val on = k == key
                items[i].setTextColor(if (on) c.onAccent else c.ink)
                items[i].typeface = if (on) fontSemiBold else fontMedium
                items[i].background = if (on) rounded(c.accent, dpf(12f)) else null
            }
        }
    }

    private fun relative(millis: Long): String =
        DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

    private fun megabytes(bytes: Long): String = String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576f)

    private inline fun String.ifNotBlank(block: (String) -> String): String = if (isBlank()) "" else block(this)

    private companion object {
        const val MAX_NOTES = 1_200
    }
}

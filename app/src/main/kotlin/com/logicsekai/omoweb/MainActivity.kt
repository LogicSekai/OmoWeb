// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.view.Display
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.app.AlertDialog
import android.app.LocaleManager
import android.os.LocaleList
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.logicsekai.omoweb.update.UpdateManager
import com.logicsekai.omoweb.update.UpdateNotifier
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * OmoWeb: a small browser that opens YouTube, made to run on the car screen through Android Auto
 * (and so on the OMOWAY display through OmoAuto).
 *
 * Remote control: OmoAuto's Controls arrive through [OmoAutoLink] (Android Auto does not pass
 * head-unit keys to apps on the car screen); key events and rotary scroll are handled too, for
 * other input sources. They drive an on-screen pointer (up/down, left/right, select = tap); in
 * full-screen video they become play/pause and seek. Touch works as usual and hides the pointer.
 *
 * On the car screen the toolbar floats over the page and hides itself. OmoAuto's video buttons
 * (Controls › Media) switch full screen, "fill width", next / previous video, play and seek.
 *
 * Sound: videos start unmuted; see [WebScripts.UNMUTE].
 */
class MainActivity : ComponentActivity() {

    private lateinit var root: FrameLayout
    private lateinit var content: ViewGroup
    private lateinit var bar: LinearLayout
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var backButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var modeButton: ImageButton
    private lateinit var cursor: ImageView
    private lateinit var osd: TextView

    private val pointer by lazy { RemotePointer(baseStep = dp(28).toFloat()) }
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var desktopMode = false
    private var injectingTouch = false
    private var remoteHintShown = false
    private var videoHintShown = false
    private var onCarScreen = false
    /** Ultrawide zoom is on (full screen, video zoomed to the full width). */
    private var ultrawide = false
    /** Ultrawide switched full screen on, so switching it off leaves full screen too. */
    private var ultrawideOpenedFullscreen = false
    private var media: WebMediaSession? = null
    /** Phone only: settings / guide shown before YouTube. */
    private var setupView: PhoneSetupView? = null
    /** When this screen last loaded a page (to pick up pages opened on the other screen). */
    private var loadedAtMs = 0L
    private var loginRetriedAsDesktop = false

    private val omoAuto by lazy { OmoAutoLink(this, ::onOmoAutoCommand) }
    private val updates: UpdateManager get() = (application as OmoWebApp).updateManager
    /** Light/dark palette shared with OmoAuto. The motorcycle screen is always dark. */
    private lateinit var colors: OmoTheme.Colors
    /** The last install attempt needs "Install unknown apps" first. */
    private var updateNeedsPermission = false

    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private val hideCursor = Runnable { cursor.visibility = View.GONE }
    private val hideOsd = Runnable { osd.animate().alpha(0f).setDuration(250).start() }
    private val hideBar = Runnable { setBarShown(false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        desktopMode = prefs.getBoolean(KEY_DESKTOP, false)
        onCarScreen = display?.displayId != Display.DEFAULT_DISPLAY
        colors = OmoTheme.colors(this, onCar = onCarScreen)
        buildUi()
        setContentView(root)
        applySystemBarAppearance()
        configureWebView()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    customView != null -> hideCustomView()
                    address.hasFocus() -> clearAddressFocus()
                    setupView?.visibility == View.VISIBLE && web.url != null -> hideSetup()
                    web.canGoBack() -> web.goBack()
                    // On the phone, leaving YouTube's first page goes back to the guide.
                    !onCarScreen && setupView?.visibility != View.VISIBLE -> showSetup()
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })

        val restored = savedInstanceState != null && web.restoreState(savedInstanceState) != null
        if (restored && savedInstanceState?.getBoolean(STATE_SETUP) == true) showSetup()
        val linkUrl = urlFrom(intent)
        when {
            savedInstanceState?.getBoolean(STATE_SETUP) == true && !restored -> showSetup()
            restored -> loadedAtMs = System.currentTimeMillis()
            linkUrl != null -> load(linkUrl)
            // The motorcycle screen goes straight to YouTube (last page, else home).
            onCarScreen -> load(lastUrl() ?: homeUrl())
            // The phone first shows the guide and settings.
            else -> showSetup()
        }
        val onPhone = !onCarScreen
        if (!onPhone) Toast.makeText(this, R.string.parked_only, Toast.LENGTH_LONG).show()
        if (onPhone) {
            updates.check()
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    combine(updates.state, updates.autoCheck) { _, _ -> Unit }.collect { pushUpdateState() }
                }
            }
            handleOpenRequest(intent)
        }
        // Same as other Android Auto apps: ask for notifications once, on the phone screen only.
        if (onPhone && savedInstanceState == null &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onStart() {
        super.onStart()
        omoAuto.setActive(onCarScreen)
    }

    override fun onStop() {
        omoAuto.setActive(false)
        super.onStop()
    }

    /** Status / navigation bar icons dark on the light theme, light on the dark one. */
    private fun applySystemBarAppearance() {
        val light = !onCarScreen && !colors.dark
        val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(if (light) mask else 0, mask)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // uiMode is handled here (no automatic restart): follow a system light/dark switch.
        if (!onCarScreen && OmoTheme.mode(this) == OmoTheme.Mode.SYSTEM && OmoTheme.colors(this) != colors) recreate()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        urlFrom(intent)?.let { web.loadUrl(it) }
        if (!onCarScreen) handleOpenRequest(intent)
    }

    /** The update notification opens the phone screen at the update card. */
    private fun handleOpenRequest(intent: Intent?) {
        if (intent?.getStringExtra(UpdateNotifier.EXTRA_OPEN) != UpdateNotifier.OPEN_UPDATE) return
        intent.removeExtra(UpdateNotifier.EXTRA_OPEN)
        showSetup()
        setupView?.scrollToUpdate()
    }

    private fun pushUpdateState() {
        val view = setupView ?: return
        if (updates.state.value !is UpdateManager.State.ReadyToInstall) updateNeedsPermission = false
        view.showUpdate(
            PhoneSetupView.UpdateUi(
                enabled = updates.isEnabled,
                canSelfInstall = updates.canSelfInstall,
                state = updates.state.value,
                autoCheck = updates.autoCheck.value,
                lastCheckMillis = updates.lastCheckMillis,
                releasesUrl = updates.releasesUrl,
            ),
            needsInstallPermission = updateNeedsPermission,
        )
    }

    private fun openExternal(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun urlFrom(intent: Intent?): String? = intent?.dataString?.takeIf { it.startsWith("http") }

    // ---- Shared state: last page, language, Google account ----------------------------------

    private fun load(url: String) {
        loadedAtMs = System.currentTimeMillis()
        web.loadUrl(url)
    }

    private fun lastUrl(): String? = prefs.getString(KEY_LAST_URL, null)?.takeIf(WebUrls::isResumable)

    /** Remembers the page for both screens (phone and motorcycle share one app and one store). */
    private fun rememberPage(url: String, title: String? = null) {
        if (!WebUrls.isResumable(url)) return
        val edit = prefs.edit().putString(KEY_LAST_URL, url).putLong(KEY_LAST_AT, System.currentTimeMillis())
        if (title != null) edit.putString(KEY_LAST_TITLE, title)
        edit.apply()
        loadedAtMs = System.currentTimeMillis()
    }

    private fun currentLanguage(): String? =
        getSystemService(LocaleManager::class.java)?.applicationLocales?.takeIf { !it.isEmpty }?.get(0)?.language
            ?.let { if (it == "in") "id" else it }

    private fun homeUrl(): String = WebUrls.homeUrl(currentLanguage())

    private fun isSignedIn(): Boolean =
        GoogleLogin.isSignedIn(CookieManager.getInstance().getCookie("https://www.youtube.com")) ||
            GoogleLogin.isSignedIn(CookieManager.getInstance().getCookie("https://m.youtube.com"))

    private fun showSetup() {
        val links = PhoneSetupView.Links(
            versionLabel = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            sourceUrl = BuildConfig.SOURCE_URL,
            communityUrl = BuildConfig.COMMUNITY_URL,
            sponsorUrl = BuildConfig.SPONSOR_URL,
            kofiUrl = BuildConfig.KOFI_URL,
            trakteerUrl = BuildConfig.TRAKTEER_URL,
        )
        val view = setupView ?: PhoneSetupView(this, colors, links, object : PhoneSetupView.Actions {
            override fun openYouTube() { hideSetup(); load(homeUrl()) }
            override fun resume(url: String) { hideSetup(); load(url) }
            override fun signIn() { hideSetup(); loginRetriedAsDesktop = false; load(GoogleLogin.SIGN_IN_URL) }
            override fun signOut() {
                CookieManager.getInstance().removeAllCookies {
                    CookieManager.getInstance().flush()
                    Toast.makeText(this@MainActivity, R.string.signed_out, Toast.LENGTH_SHORT).show()
                    refreshSetup()
                }
            }
            override fun setLanguage(tag: String?) {
                getSystemService(LocaleManager::class.java)?.applicationLocales =
                    if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
            }
            override fun openAndroidAuto() = openAndroidAutoSettings()
            override fun setThemeMode(mode: OmoTheme.Mode) {
                if (OmoTheme.mode(this@MainActivity) == mode) return
                OmoTheme.setMode(this@MainActivity, mode)
                // Rebuild the screen with the new palette (page and settings state are saved).
                if (OmoTheme.colors(this@MainActivity) != colors) recreate()
            }
            override fun openUrl(url: String) = openExternal(url)
            override fun checkUpdate() = updates.check(force = true)
            override fun downloadUpdate() = updates.download()
            override fun cancelUpdate() = updates.cancelDownload()
            override fun installUpdate() {
                updateNeedsPermission = updates.install(this@MainActivity) == UpdateManager.InstallResult.NEEDS_PERMISSION
                pushUpdateState()
            }
            override fun setAutoUpdate(enabled: Boolean) = updates.setAutoCheck(enabled)
        }).also {
            setupView = it
            root.addView(it, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            it.setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
        view.visibility = View.VISIBLE
        view.bringToFront()
        refreshSetup()
        pushUpdateState()
    }

    private fun hideSetup() {
        setupView?.visibility = View.GONE
    }

    private fun refreshSetup() {
        val view = setupView ?: return
        view.update(
            PhoneSetupView.State(
                signedIn = isSignedIn(),
                lastUrl = lastUrl(),
                lastTitle = prefs.getString(KEY_LAST_TITLE, null),
                language = currentLanguage(),
                themeMode = OmoTheme.mode(this),
            ),
        )
    }

    private fun openAndroidAutoSettings() {
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage(ANDROID_AUTO)
            ?: Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", ANDROID_AUTO, null))
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** Google refused the embedded browser: retry once as desktop Chrome, then explain. */
    private fun onLoginRejected() {
        if (!loginRetriedAsDesktop && !desktopMode) {
            loginRetriedAsDesktop = true
            Toast.makeText(this, R.string.login_rejected_retry, Toast.LENGTH_LONG).show()
            web.stopLoading()
            web.settings.userAgentString = DESKTOP_USER_AGENT
            web.postDelayed({ load(GoogleLogin.SIGN_IN_URL) }, 300)
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.setup_account_title)
                .setMessage(R.string.login_rejected_final)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    // ---- Remote control ---------------------------------------------------------------------

    /** A button or trackpad gesture from OmoAuto's Controls screen. */
    private fun onOmoAutoCommand(command: String, dx: Float, dy: Float) {
        when (command) {
            "back" -> onBackPressedDispatcher.onBackPressed()
            "fullscreen" -> if (customView != null) hideCustomView() else enterVideoFullscreen()
            "ultrawide", "fill" -> toggleUltrawide()
            "move" -> trackpadMove(dx, dy)
            "tap" -> trackpadTap()
            "scroll" -> trackpadScroll(dy)
            else -> OmoAutoLink.actionFor(command)?.let(::onRemote)
        }
    }

    // ---- Trackpad (OmoAuto Controls › Trackpad) -----------------------------------------------

    /** dx / dy are fractions of the trackpad's width; one full swipe crosses about the screen. */
    private fun trackpadMove(dx: Float, dy: Float) {
        pointer.setBounds(root.width.toFloat(), root.height.toFloat())
        showCursor()
        val scale = root.width * TRACKPAD_SPEED
        pointer.moveBy(dx * scale, dy * scale)
        placeCursor()
        if (pointer.y < dp(56)) showBarForAWhile()
    }

    private fun trackpadTap() {
        pointer.setBounds(root.width.toFloat(), root.height.toFloat())
        showCursor()
        tapAtPointer()
    }

    /** Two-finger scroll: the page follows the fingers. */
    private fun trackpadScroll(dy: Float) {
        val css = (-dy * root.width * TRACKPAD_SPEED / resources.displayMetrics.density).toInt()
        if (css != 0) web.evaluateJavascript(WebScripts.scroll(css), null)
    }

    // ---- Media session ----------------------------------------------------------------------

    /** Called by [WebScripts.MEDIA_REPORT] on a JavaBridge thread. */
    private inner class Bridge {
        @JavascriptInterface
        fun onMedia(json: String) {
            val state = runCatching { JSONObject(json) }.getOrNull() ?: return
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                val session = media ?: WebMediaSession(this@MainActivity, ::onMediaCommand).also { media = it }
                session.update(state)
            }
        }
    }

    /** Media buttons (OmoAuto, headset, system media controls) arriving through the session. */
    private fun onMediaCommand(command: String) {
        when {
            command == "play" -> videoCommand("play")
            command == "pause" -> videoCommand("pause")
            command == "next" -> navigateVideo(1)
            command == "prev" -> navigateVideo(-1)
            command == "rewind" -> videoCommand("seek", -SEEK_SECONDS)
            command == "forward" -> videoCommand("seek", SEEK_SECONDS)
            command.startsWith("seekto:") -> command.substringAfter(':').toIntOrNull()?.let { videoCommand("seekto", it) }
        }
    }

    private fun navigateVideo(direction: Int) {
        web.evaluateJavascript(WebScripts.navigateVideo(direction)) { result ->
            when {
                result?.contains("none") != true -> showOsd(if (direction > 0) "⏭" else "⏮")
                direction < 0 && web.canGoBack() -> {
                    hideCustomView()
                    web.goBack()
                    showOsd("⏮")
                }
                else -> showOsd(getString(R.string.no_video))
            }
        }
    }

    // ---- Ultrawide --------------------------------------------------------------------------

    private fun toggleUltrawide() {
        if (!ultrawide) {
            ultrawide = true
            if (customView == null) {
                // Full screen first; the zoom is applied once the full-screen view is up
                // (onShowCustomView), when the player has its final size.
                ultrawideOpenedFullscreen = true
                enterVideoFullscreen()
            } else {
                applyUltrawideZoom()
            }
            showOsd(getString(R.string.ultrawide_on))
        } else {
            setUltrawideOff()
            if (ultrawideOpenedFullscreen && customView != null) hideCustomView()
            ultrawideOpenedFullscreen = false
            showOsd(getString(R.string.ultrawide_off))
        }
    }

    private fun applyUltrawideZoom() {
        web.evaluateJavascript(WebScripts.zoom(true), null)
        // The player resizes in steps while entering full screen: re-apply once it settled.
        root.postDelayed({ if (ultrawide) web.evaluateJavascript(WebScripts.zoom(true), null) }, 500)
    }

    private fun setUltrawideOff() {
        if (!ultrawide) return
        ultrawide = false
        web.evaluateJavascript(WebScripts.zoom(false), null)
    }

    /** Full screen needs a user gesture: arm the page, then tap the video for real. */
    private fun enterVideoFullscreen() {
        if (customView != null || isFinishing) return
        web.evaluateJavascript(WebScripts.ARM_FULLSCREEN) { result ->
            val json = result?.takeIf { it.startsWith("{") }?.let { runCatching { JSONObject(it) }.getOrNull() }
            if (json == null) {
                setUltrawideOff()
                ultrawideOpenedFullscreen = false
                showOsd(getString(R.string.no_video))
                return@evaluateJavascript
            }
            val cssWidth = json.optDouble("w", 0.0)
            if (cssWidth <= 0.0 || web.width == 0) return@evaluateJavascript
            val scale = web.width / cssWidth
            tapView(web, (json.optDouble("x") * scale).toFloat(), (json.optDouble("y") * scale).toFloat())
        }
    }

    /** Injects a real tap at ([x], [y]) in [view]'s own coordinates. */
    private fun tapView(view: View, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        injectingTouch = true
        try {
            listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEachIndexed { i, act ->
                val ev = MotionEvent.obtain(downTime, downTime + i * 60L, act, x, y, 0)
                ev.source = InputDevice.SOURCE_TOUCHSCREEN
                view.dispatchTouchEvent(ev)
                ev.recycle()
            }
        } finally {
            injectingTouch = false
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // While typing in the address bar the keys belong to the text field.
        if (address.hasFocus() && event.keyCode != KeyEvent.KEYCODE_DPAD_UP && event.keyCode != KeyEvent.KEYCODE_DPAD_DOWN) {
            return super.dispatchKeyEvent(event)
        }
        val action = RemoteInput.fromKey(event.keyCode) ?: return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) onRemote(action)
        return true
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL &&
            (event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER) || event.isFromSource(InputDevice.SOURCE_CLASS_POINTER).not())
        ) {
            val action = RemoteInput.fromScroll(event.getAxisValue(MotionEvent.AXIS_SCROLL), event.getAxisValue(MotionEvent.AXIS_VSCROLL))
            if (action != null) {
                onRemote(action)
                return true
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!injectingTouch && ev.actionMasked == MotionEvent.ACTION_DOWN) {
            root.removeCallbacks(hideCursor)
            cursor.visibility = View.GONE
            if (ev.y < root.height * 0.2f) showBarForAWhile()
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun onRemote(action: RemoteAction) {
        if (customView != null) {
            onVideoRemote(action)
            return
        }
        when (action) {
            RemoteAction.PLAY_PAUSE -> videoCommand("toggle")
            RemoteAction.PLAY -> videoCommand("play")
            RemoteAction.PAUSE -> videoCommand("pause")
            RemoteAction.REWIND -> videoCommand("seek", -SEEK_SECONDS)
            RemoteAction.FAST_FORWARD -> videoCommand("seek", SEEK_SECONDS)
            else -> onPointerRemote(action)
        }
    }

    private fun onPointerRemote(action: RemoteAction) {
        if (!remoteHintShown) {
            remoteHintShown = true
            Toast.makeText(this, R.string.remote_hint, Toast.LENGTH_LONG).show()
        }
        pointer.setBounds(root.width.toFloat(), root.height.toFloat())
        val wasHidden = cursor.visibility != View.VISIBLE
        showCursor()
        if (wasHidden && action == RemoteAction.SELECT) return // first press only reveals the pointer
        if (action == RemoteAction.SELECT) {
            tapAtPointer()
            return
        }
        val move = pointer.move(action, SystemClock.uptimeMillis())
        placeCursor()
        if (pointer.y < dp(56) || move.overflowY < 0f) showBarForAWhile()
        if (move.overflowY != 0f) {
            val css = (move.overflowY * 4 / resources.displayMetrics.density).toInt()
            web.evaluateJavascript(WebScripts.scroll(css), null)
        }
    }

    private fun onVideoRemote(action: RemoteAction) {
        if (!videoHintShown) {
            videoHintShown = true
            Toast.makeText(this, R.string.video_hint, Toast.LENGTH_LONG).show()
        }
        when (action) {
            RemoteAction.SELECT, RemoteAction.PLAY_PAUSE -> videoCommand("toggle")
            RemoteAction.PLAY -> videoCommand("play")
            RemoteAction.PAUSE -> videoCommand("pause")
            RemoteAction.LEFT, RemoteAction.UP, RemoteAction.REWIND -> videoCommand("seek", -SEEK_SECONDS)
            RemoteAction.RIGHT, RemoteAction.DOWN, RemoteAction.FAST_FORWARD -> videoCommand("seek", SEEK_SECONDS)
        }
    }

    private fun videoCommand(command: String, seconds: Int = 0) {
        web.evaluateJavascript(WebScripts.video(command, seconds)) { result ->
            showOsd(
                when {
                    command == "seek" -> if (seconds < 0) "⏪ ${-seconds} s" else "⏩ $seconds s"
                    command == "seekto" -> return@evaluateJavascript
                    result?.contains("paused") == true -> "⏸"
                    result?.contains("playing") == true -> "▶"
                    else -> return@evaluateJavascript
                },
            )
        }
    }

    private fun showCursor() {
        if (cursor.visibility != View.VISIBLE) {
            cursor.visibility = View.VISIBLE
            placeCursor()
        }
        root.removeCallbacks(hideCursor)
        root.postDelayed(hideCursor, CURSOR_TIMEOUT_MS)
    }

    /** The arrow's tip is at (2, 2) of its 28 × 28 drawable. */
    private fun placeCursor() {
        val tip = dp(2)
        cursor.translationX = pointer.x - tip
        cursor.translationY = pointer.y - tip
    }

    private fun tapAtPointer() {
        val x = pointer.x
        val y = pointer.y
        if (address.hasFocus() && !hit(address, x, y)) clearAddressFocus()
        tapView(root, x, y)
        cursor.animate().scaleX(0.8f).scaleY(0.8f).setDuration(70).withEndAction {
            cursor.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
        }.start()
    }

    private fun hit(view: View, x: Float, y: Float): Boolean {
        val loc = IntArray(2).also { view.getLocationInWindow(it) }
        val rootLoc = IntArray(2).also { root.getLocationInWindow(it) }
        val left = loc[0] - rootLoc[0]
        val top = loc[1] - rootLoc[1]
        return x >= left && x < left + view.width && y >= top && y < top + view.height
    }

    private fun showOsd(text: String) {
        osd.text = text
        osd.animate().cancel()
        osd.alpha = 1f
        root.removeCallbacks(hideOsd)
        root.postDelayed(hideOsd, 900)
    }

    // ---- UI ---------------------------------------------------------------------------------

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buildUi() {
        // Phone: OmoTheme light/dark. Car screen: the dark navy bar over a black page, as before.
        val phone = !onCarScreen
        root = FrameLayout(this).apply { setBackgroundColor(if (phone) colors.background else Color.BLACK) }
        bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(if (phone) colors.surface else BAR_COLOR)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            if (phone) elevation = dp(2).toFloat()
        }
        backButton = toolButton(R.drawable.ic_back, R.string.back) { if (web.canGoBack()) web.goBack() }
        forwardButton = toolButton(R.drawable.ic_forward, R.string.forward) { if (web.canGoForward()) web.goForward() }
        val reload = toolButton(R.drawable.ic_reload, R.string.reload) { web.reload() }
        val home = toolButton(R.drawable.ic_home, R.string.home) { load(homeUrl()) }
        address = EditText(this).apply {
            setSingleLine()
            textSize = 15f
            typeface = OmoTheme.regular(this@MainActivity)
            setTextColor(if (phone) colors.ink else Color.WHITE)
            setHintTextColor(if (phone) colors.inkMuted else 0x99FFFFFF.toInt())
            setHint(R.string.address_hint)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
            setSelectAllOnFocus(true)
            setPadding(dp(14), 0, dp(14), 0)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(if (phone) this@MainActivity.colors.field else FIELD_COLOR)
                if (phone) setStroke(dp(1), this@MainActivity.colors.line)
            }
            setOnEditorActionListener { _, actionId, event ->
                val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
                if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                    openInput(text.toString())
                    true
                } else {
                    false
                }
            }
        }
        modeButton = toolButton(modeIcon(), modeLabel()) { toggleDesktopMode() }

        listOf(backButton, forwardButton, reload, home).forEach { bar.addView(it, LinearLayout.LayoutParams(dp(52), dp(48))) }
        bar.addView(address, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(6); marginEnd = dp(6) })
        bar.addView(modeButton, LinearLayout.LayoutParams(dp(52), dp(48)))
        if (!onCarScreen) {
            bar.addView(toolButton(R.drawable.ic_settings, R.string.settings) { showSetup() }, LinearLayout.LayoutParams(dp(52), dp(48)))
        }

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = ColorStateList.valueOf(ACCENT)
            visibility = View.GONE
        }
        web = WebView(this).apply {
            setBackgroundColor(if (phone) colors.background else Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
        }

        if (onCarScreen) {
            // Car screen: the page gets the whole height; the toolbar floats on top and hides.
            bar.setBackgroundColor(BAR_COLOR_FLOATING)
            content = FrameLayout(this).apply {
                addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                addView(progress, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP))
                addView(bar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))
            }
            address.setOnFocusChangeListener { _, focused -> if (focused) setBarShown(true) else showBarForAWhile() }
        } else {
            content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
                addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            }
        }
        root.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        // Apps targeting Android 15+ are drawn edge to edge: keep the toolbar and page clear of
        // the status / navigation bars and the keyboard.
        content.setOnApplyWindowInsetsListener { v, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        cursor = ImageView(this).apply {
            setImageResource(R.drawable.ic_cursor)
            visibility = View.GONE
            isClickable = false
            isFocusable = false
            elevation = dp(8).toFloat()
            pivotX = dp(2).toFloat()
            pivotY = dp(2).toFloat()
        }
        root.addView(cursor, FrameLayout.LayoutParams(dp(28), dp(28)))

        osd = TextView(this).apply {
            textSize = 28f
            setTextColor(Color.WHITE)
            setPadding(dp(18), dp(8), dp(18), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(0xAA000000.toInt())
            }
            alpha = 0f
            elevation = dp(8).toFloat()
        }
        root.addView(osd, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            pointer.setBounds(root.width.toFloat(), root.height.toFloat())
            if (cursor.visibility == View.VISIBLE) placeCursor()
        }
    }

    private fun toolButton(icon: Int, label: Int, onClick: () -> Unit) = ImageButton(this).apply {
        setImageResource(icon)
        contentDescription = getString(label)
        // Icons are white vectors: tint them with the ink colour on the phone's light theme.
        if (!onCarScreen) imageTintList = ColorStateList.valueOf(colors.ink)
        background = android.graphics.drawable.RippleDrawable(
            ColorStateList.valueOf(if (onCarScreen) 0x33FFFFFF else colors.ripple),
            null,
            GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(Color.BLACK) },
        )
        scaleType = ImageView.ScaleType.CENTER
        setOnClickListener { onClick() }
    }

    /** Car screen only: shows the floating toolbar, then hides it after [BAR_TIMEOUT_MS]. */
    private fun showBarForAWhile() {
        if (!onCarScreen) return
        setBarShown(true)
        root.removeCallbacks(hideBar)
        if (!address.hasFocus()) root.postDelayed(hideBar, BAR_TIMEOUT_MS)
    }

    private fun setBarShown(shown: Boolean) {
        if (!onCarScreen) return
        if (!shown && address.hasFocus()) return
        bar.animate().cancel()
        bar.animate().translationY(if (shown) 0f else -bar.height.toFloat() - dp(4)).setDuration(180).start()
    }

    private fun modeIcon() = if (desktopMode) R.drawable.ic_mobile else R.drawable.ic_desktop
    private fun modeLabel() = if (desktopMode) R.string.mobile else R.string.desktop

    private fun updateNavButtons() {
        backButton.alpha = if (web.canGoBack()) 1f else 0.4f
        forwardButton.alpha = if (web.canGoForward()) 1f else 0.4f
    }

    private fun clearAddressFocus() {
        address.clearFocus()
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(address.windowToken, 0)
        web.requestFocus()
    }

    // ---- WebView ----------------------------------------------------------------------------

    @SuppressLint("SetJavaScriptEnabled", "RequiresFeature")
    private fun configureWebView() {
        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Lets videos start (and stay) with sound without a tap on the page.
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = userAgentFor(desktopMode)
        }
        // Keep the Google / YouTube sign-in across launches (shared by phone and motorcycle).
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        // Do not announce the app as an embedded WebView (X-Requested-With): Google blocks those.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(web.settings, emptySet())
        }
        web.addJavascriptInterface(Bridge(), "OmoBridge")
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(web, WebScripts.UNMUTE, setOf("*"))
            WebViewCompat.addDocumentStartJavaScript(web, WebScripts.MEDIA_REPORT, setOf("*"))
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme?.lowercase()
                // Keep the web inside this screen; app links (intent://, vnd.youtube:, market://)
                // would open on the phone, which the rider cannot see.
                return scheme != "http" && scheme != "https"
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (GoogleLogin.isRejected(url)) onLoginRejected()
                showBarForAWhile()
                if (!address.hasFocus()) address.setText(url)
                updateNavButtons()
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (!address.hasFocus()) address.setText(url)
                // Fallback for WebView versions without document-start scripts (no-op otherwise).
                view.evaluateJavascript(WebScripts.UNMUTE, null)
                view.evaluateJavascript(WebScripts.MEDIA_REPORT, null)
                rememberPage(url, view.title)
                // Back from Google sign-in: return to the mobile user agent for YouTube.
                if (loginRetriedAsDesktop && !desktopMode && !url.contains("accounts.google.com")) {
                    loginRetriedAsDesktop = false
                    view.settings.userAgentString = userAgentFor(false)
                }
                CookieManager.getInstance().flush()
                updateNavButtons()
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                if (GoogleLogin.isRejected(url)) onLoginRejected()
                // YouTube changes pages in place (no page load): remember every video here.
                rememberPage(url, view.title)
                if (!address.hasFocus()) address.setText(url)
                updateNavButtons()
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                content.visibility = View.GONE
                cursor.visibility = View.GONE
                root.addView(view, 1, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                if (ultrawide) applyUltrawideZoom()
            }

            override fun onHideCustomView() = hideCustomView()
        }
    }

    private fun hideCustomView() {
        val view = customView ?: return
        setUltrawideOff()
        ultrawideOpenedFullscreen = false
        root.removeView(view)
        customView = null
        content.visibility = View.VISIBLE
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    /** Mobile = WebView's own user agent without the WebView markers (see [GoogleLogin]). */
    private fun userAgentFor(desktop: Boolean): String =
        if (desktop) DESKTOP_USER_AGENT else GoogleLogin.chromeUserAgent(WebSettings.getDefaultUserAgent(this))

    private fun toggleDesktopMode() {
        desktopMode = !desktopMode
        prefs.edit().putBoolean(KEY_DESKTOP, desktopMode).apply()
        web.settings.userAgentString = userAgentFor(desktopMode)
        modeButton.setImageResource(modeIcon())
        modeButton.contentDescription = getString(modeLabel())
        web.reload()
    }

    private fun openInput(raw: String) {
        val url = WebUrls.resolve(raw) ?: return
        clearAddressFocus()
        web.loadUrl(url)
    }

    // ---- Lifecycle --------------------------------------------------------------------------

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
        outState.putBoolean(STATE_SETUP, setupView?.visibility == View.VISIBLE)
    }

    override fun onPause() {
        CookieManager.getInstance().flush()
        web.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
        // A page opened on the other screen (phone ↔ motorcycle) since this one last loaded.
        val savedAt = prefs.getLong(KEY_LAST_AT, 0L)
        val saved = lastUrl()
        if (saved != null && savedAt > loadedAtMs && saved != web.url && setupView?.visibility != View.VISIBLE) load(saved)
        refreshSetup()
    }

    override fun onDestroy() {
        root.removeCallbacks(hideCursor)
        root.removeCallbacks(hideOsd)
        root.removeCallbacks(hideBar)
        media?.release()
        media = null
        hideCustomView()
        (web.parent as? ViewGroup)?.removeView(web)
        web.destroy()
        super.onDestroy()
    }

    private companion object {
        const val PREFS = "omoweb"
        const val KEY_LAST_URL = "last_url"
        const val KEY_LAST_AT = "last_url_at"
        const val STATE_SETUP = "setup_visible"
        const val KEY_LAST_TITLE = "last_title"
        const val ANDROID_AUTO = "com.google.android.projection.gearhead"
        const val KEY_DESKTOP = "desktop_mode"
        const val SEEK_SECONDS = 10
        /** Trackpad: one swipe across the pad moves the pointer about 1.2 screen widths. */
        const val TRACKPAD_SPEED = 1.2f
        const val CURSOR_TIMEOUT_MS = 8_000L
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        const val BAR_TIMEOUT_MS = 3_500L
        const val BAR_COLOR = 0xFF1B1F3B.toInt()
        const val BAR_COLOR_FLOATING = 0xE61B1F3B.toInt()
        const val FIELD_COLOR = 0xFF3C467D.toInt()
        const val ACCENT = 0xFFFF7A4F.toInt()
    }
}

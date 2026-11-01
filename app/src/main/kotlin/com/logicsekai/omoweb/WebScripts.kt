// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

/** JavaScript that OmoWeb injects into pages. */
object WebScripts {

    /**
     * YouTube (and many sites) start videos muted because of browser autoplay rules, showing a
     * "Tap to unmute" button. On a motorcycle there is no easy way to tap it, so OmoWeb turns the
     * sound on by itself:
     *  - YouTube's saved player volume is set to 100 % / not muted before the player loads;
     *  - every main video that starts, loads or gets muted without the rider touching anything is
     *    unmuted (YouTube's own player API first, the <video> element as a fallback);
     *  - if the rider mutes on purpose (a tap or key press just before), that choice is kept for
     *    that video.
     * Muted previews in the YouTube feed stay muted; only watch / Shorts / live / embed pages and
     * videos on other sites are unmuted.
     */
    val UNMUTE = """
        (function () {
          if (window.__omoUnmute) return;
          window.__omoUnmute = true;
          var lastGesture = 0;
          ['pointerdown', 'touchstart', 'keydown', 'mousedown'].forEach(function (t) {
            document.addEventListener(t, function () { lastGesture = Date.now(); }, true);
          });
          function isYouTube() { return /(^|\.)youtube\.com$/.test(location.hostname); }
          function mainPage() {
            return !isYouTube() || /^\/(watch|shorts|live|embed)/.test(location.pathname) || !!document.fullscreenElement;
          }
          function saveVolume() {
            if (!isYouTube()) return;
            try {
              var now = Date.now();
              var value = JSON.stringify({ data: JSON.stringify({ volume: 100, muted: false }), expiration: now + 30 * 864e5, creation: now });
              localStorage.setItem('yt-player-volume', value);
              sessionStorage.setItem('yt-player-volume', value);
            } catch (e) {}
          }
          function unmute(v) {
            if (!v || v.__omoUserMuted || !mainPage()) return;
            try {
              var p = document.getElementById('movie_player');
              if (p && p.isMuted && p.isMuted()) { p.unMute(); if (p.setVolume) p.setVolume(100); }
            } catch (e) {}
            try {
              if (v.muted) v.muted = false;
              if (v.volume < 1) v.volume = 1;
            } catch (e) {}
          }
          function onMedia(e) {
            var v = e.target;
            if (!v || v.tagName !== 'VIDEO') return;
            if (e.type === 'volumechange') {
              if (v.muted && Date.now() - lastGesture < 1000) { v.__omoUserMuted = true; return; }
              if (!v.muted) { v.__omoUserMuted = false; return; }
            }
            unmute(v);
          }
          ['loadeddata', 'play', 'playing', 'volumechange'].forEach(function (t) {
            document.addEventListener(t, onMedia, true);
          });
          saveVolume();
          setInterval(function () {
            var vs = document.querySelectorAll('video');
            for (var i = 0; i < vs.length; i++) if (!vs[i].paused) unmute(vs[i]);
          }, 1500);
        })();
    """.trimIndent()

    /**
     * Prepares a real full-screen switch of the main video. Browsers only allow full screen from
     * a user gesture, so this arms a one-shot click handler (earliest, capture phase) that calls
     * requestFullscreen on YouTube's player (or the video), and returns where OmoWeb should tap:
     * {"x","y","w","h"} in CSS pixels of the viewport, or null when there is no video. OmoWeb then
     * injects a real tap there; that tap is the user gesture. The click itself is swallowed.
     */
    val ARM_FULLSCREEN = """
        (function () {
          var vs = [].slice.call(document.querySelectorAll('video'));
          if (!vs.length || document.fullscreenElement) return null;
          var v = vs.filter(function (x) { return !x.paused; })[0] ||
                  vs.sort(function (a, b) { return b.clientWidth * b.clientHeight - a.clientWidth * a.clientHeight; })[0];
          var target = document.getElementById('movie_player') || (v.closest && v.closest('.html5-video-player')) || v;
          var r = v.getBoundingClientRect();
          var top = Math.max(0, r.top), bottom = Math.min(innerHeight, r.bottom);
          var left = Math.max(0, r.left), right = Math.min(innerWidth, r.right);
          if (bottom - top < 4 || right - left < 4) return null;
          var handler = function (e) {
            window.removeEventListener('click', handler, true);
            e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation();
            var fs = target.requestFullscreen || target.webkitRequestFullscreen;
            if (fs) { try { var p = fs.call(target); if (p && p.catch) p.catch(function () {}); } catch (x) {} }
          };
          window.addEventListener('click', handler, true);
          setTimeout(function () { window.removeEventListener('click', handler, true); }, 1500);
          return { x: (left + right) / 2, y: (top + bottom) / 2, w: innerWidth, h: innerHeight };
        })();
    """.trimIndent()

    /**
     * Reports the main video's state to OmoWeb's media session through `OmoBridge.onMedia(json)`:
     * {playing, title, artist, position, duration}. Sent on play/pause/seek/metadata changes and
     * every few seconds. Muted feed previews on YouTube's home page are ignored.
     */
    val MEDIA_REPORT = """
        (function () {
          if (window.__omoMedia) return;
          window.__omoMedia = true;
          var last = '';
          function mainPage() {
            return !/(^|\.)youtube\.com$/.test(location.hostname) || /^\/(watch|shorts|live|embed)/.test(location.pathname);
          }
          function report(force) {
            if (!mainPage()) return;
            var vs = [].slice.call(document.querySelectorAll('video'));
            var v = vs.filter(function (x) { return !x.paused; })[0] || vs.filter(function (x) { return x.currentTime > 0; })[0];
            if (!v) return;
            var md = navigator.mediaSession && navigator.mediaSession.metadata;
            var s = {
              playing: !v.paused && !v.ended,
              title: (md && md.title) || document.title,
              artist: (md && md.artist) || location.hostname,
              position: v.currentTime || 0,
              duration: isFinite(v.duration) ? v.duration : 0
            };
            var key = JSON.stringify([s.playing, s.title, s.artist, Math.round(s.duration)]);
            if (key === last && !force) return;
            last = key;
            try { OmoBridge.onMedia(JSON.stringify(s)); } catch (e) {}
          }
          ['play', 'playing', 'pause', 'ended', 'loadedmetadata', 'seeked'].forEach(function (t) {
            document.addEventListener(t, function (e) {
              if (e.target && e.target.tagName === 'VIDEO') report(true);
            }, true);
          });
          setInterval(function () { report(false); }, 3000);
        })();
    """.trimIndent()

    /** Controls the main video: "toggle", "play", "pause", "seek" by [seconds] or "seekto" [seconds]. */
    fun video(command: String, seconds: Int = 0): String = """
        (function (c, s) {
          var vs = [].slice.call(document.querySelectorAll('video'));
          if (!vs.length) return 'none';
          var v = vs.filter(function (x) { return !x.paused; })[0] ||
                  vs.sort(function (a, b) { return b.clientWidth * b.clientHeight - a.clientWidth * a.clientHeight; })[0];
          if (c === 'toggle') { if (v.paused) v.play(); else v.pause(); }
          else if (c === 'play') v.play();
          else if (c === 'pause') v.pause();
          else if (c === 'seek') v.currentTime = Math.max(0, v.currentTime + s);
          else if (c === 'seekto') v.currentTime = Math.max(0, s);
          return v.paused ? 'paused' : 'playing';
        })('$command', $seconds);
    """.trimIndent()

    /**
     * Next (+1) or previous (-1) video: YouTube's player API first, then the player's buttons.
     * Returns "player", "button" or "none".
     */
    fun navigateVideo(direction: Int): String = """
        (function (d) {
          var p = document.getElementById('movie_player');
          try {
            if (p && d > 0 && p.nextVideo) { p.nextVideo(); return 'player'; }
            if (p && d < 0 && p.previousVideo) { p.previousVideo(); return 'player'; }
          } catch (e) {}
          var b = document.querySelector(d > 0
            ? '.ytp-next-button, button.player-control-next-button, [aria-label*="next" i]'
            : '.ytp-prev-button, button.player-control-previous-button, [aria-label*="previous" i]');
          if (b) { b.click(); return 'button'; }
          return 'none';
        })($direction);
    """.trimIndent()

    /**
     * Ultrawide zoom on/off. The video keeps the size and position the player gave it; a CSS
     * transform scales it up until it covers the whole box (the screen in full screen), so the
     * left and right edges reach the screen edges and the top and bottom are cropped. Changing the
     * element's width/height instead collapsed YouTube's video to nothing (black).
     *
     * The transform lives in an `!important` stylesheet rule keyed on `html[data-omo-zoom]`, with
     * the scale in the `--omo-zoom` variable. It must NOT be an inline style on the video: YouTube
     * rewrites the video's `style` attribute whenever it lays the player out, which wiped an
     * inline transform and made the picture flicker between ultrawide and normal every second.
     * The scale is recomputed on resize and every second (new video, different aspect), and only
     * written when it actually changes.
     */
    fun zoom(on: Boolean): String = """
        (function (on) {
          var old = document.getElementById('omo-fill-style');
          if (old) old.remove();
          var root = document.documentElement;
          if (!document.getElementById('omo-zoom-style')) {
            var st = document.createElement('style');
            st.id = 'omo-zoom-style';
            st.textContent =
              'html[data-omo-zoom] video{' +
              'transform:scale(var(--omo-zoom,1)) !important;' +
              'transform-origin:center center !important;' +
              'transition:none !important;}';
            (document.head || root).appendChild(st);
          }
          if (!window.__omoZoomApply) {
            window.__omoZoomApply = function () {
              if (!window.__omoZoom) return;
              // The main video: the largest one on the page.
              var vs = document.querySelectorAll('video'), v = null, best = 0;
              for (var i = 0; i < vs.length; i++) {
                var r = vs[i].getBoundingClientRect(), a = r.width * r.height;
                if (a > best) { best = a; v = vs[i]; }
              }
              if (!v) return;
              var box = document.fullscreenElement || v.parentElement;
              var W = (box && box.clientWidth) || innerWidth;
              var H = (box && box.clientHeight) || innerHeight;
              if (!W || !H) return;
              var ar = (v.videoWidth && v.videoHeight) ? v.videoWidth / v.videoHeight : 16 / 9;
              var fitW, fitH;
              if (W / H > ar) { fitH = H; fitW = H * ar; } else { fitW = W; fitH = W / ar; }
              var s = Math.max(W / fitW, H / fitH).toFixed(3);
              if (root.style.getPropertyValue('--omo-zoom') !== s) root.style.setProperty('--omo-zoom', s);
              if (!root.hasAttribute('data-omo-zoom')) root.setAttribute('data-omo-zoom', '');
            };
          }
          window.__omoZoom = on;
          if (on && !window.__omoZoomTimer) {
            window.__omoZoomTimer = setInterval(window.__omoZoomApply, 1000);
            window.addEventListener('resize', window.__omoZoomApply);
            document.addEventListener('fullscreenchange', window.__omoZoomApply);
          }
          if (!on) {
            if (window.__omoZoomTimer) {
              clearInterval(window.__omoZoomTimer);
              window.__omoZoomTimer = 0;
              window.removeEventListener('resize', window.__omoZoomApply);
              document.removeEventListener('fullscreenchange', window.__omoZoomApply);
            }
            root.removeAttribute('data-omo-zoom');
            root.style.removeProperty('--omo-zoom');
            // Clean up inline transforms left by older OmoWeb versions.
            var vs = document.querySelectorAll('video');
            for (var j = 0; j < vs.length; j++) {
              vs[j].style.removeProperty('transform');
              vs[j].style.removeProperty('transform-origin');
            }
          }
          window.__omoZoomApply();
          return on;
        })($on);
    """.trimIndent()

    /** Scrolls the page by [cssPixels] (positive = down). */
    fun scroll(cssPixels: Int): String = "window.scrollBy({top: $cssPixels, behavior: 'smooth'});"
}

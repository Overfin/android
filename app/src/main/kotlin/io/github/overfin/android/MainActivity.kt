package io.github.overfin.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.autofill.AutofillManager
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.window.OnBackInvokedDispatcher

// The server's own site, in a view locked to that server. Everything the
// site does comes from the server, so it updates with it; this only adds
// what a page can't do by itself in a WebView.
class MainActivity : Activity() {
    private lateinit var origin: String
    private lateinit var web: WebView

    // A server was just switched to: back mustn't lead to the previous one.
    private var forgetHistory = false

    // The element shown fullscreen, and how to tell the page it has left.
    private var fullscreen: View? = null
    private var leaveFullscreen: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val list = Server.load(this)
        val named = intent.getStringExtra(EXTRA_SERVER)
        // Other apps can start this one too: only a server in the list opens.
        val target = if (named != null) named.takeIf { it in list } else list.last
        if (target == null) {
            startActivity(if (list.entries.isEmpty()) Intent(this, ConnectActivity::class.java) else Intent(this, ServersActivity::class.java))
            finish()
            return
        }
        origin = target
        Server.store(this, list.opened(target))
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) WebView.setWebContentsDebuggingEnabled(true)
        val root = FrameLayout(this)
        // On recent Android the app draws under the system bars: keep the site clear of them.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
        web = newWebView()
        root.addView(web)
        setContentView(root)
        if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) web.loadUrl(origin)
        if (savedInstanceState == null) offerIcon(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) { goBack() }
        }
    }

    // Opening a server while the app is on: another one replaces the page,
    // this one stays where it is.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val named = intent.getStringExtra(EXTRA_SERVER) ?: return
        val list = Server.load(this)
        if (named !in list) {
            startActivity(Intent(this, ServersActivity::class.java))
            return
        }
        Server.store(this, list.opened(named))
        if (named != origin) {
            closeFullscreen()
            keepScreenOn(false)
            origin = named
            forgetHistory = true
            web.loadUrl(origin)
        }
        offerIcon(intent)
    }

    private fun offerIcon(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_CONNECTED, false)) ServerIcon.offer(this, origin)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun newWebView() = WebView(this).apply {
        setBackgroundColor(getColor(R.color.page))
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // The next episode starts by itself.
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        // Password managers fill the site's sign-in form.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
        webViewClient = Client()
        webChromeClient = Chrome()
    }

    private inner class Client : WebViewClient() {
        // Whether a redirect took the view off the server: a sign-in at an
        // outside identity provider, which stays here until it comes back.
        private var away = false

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            if (Server.sameOrigin(url, origin)) {
                away = false
                return false
            }
            if (request.isRedirect) away = true
            val scheme = request.url.scheme
            if (away && scheme == "https") return false
            if (scheme == "https" || scheme == "http" || scheme == "mailto") openOutside(request.url)
            return true
        }

        override fun onPageFinished(view: WebView, url: String) {
            // Back on the server, however it came back (Back, a script):
            // the lock holds again.
            if (Server.sameOrigin(url, origin)) away = false
            if (forgetHistory) view.clearHistory()
            forgetHistory = false
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            keepScreenOn(Uri.parse(url).path?.startsWith("/play/") == true)
            // The site signs in with a script, not a form Android sees sent:
            // leaving the page is what tells password managers to offer to save.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getSystemService(AutofillManager::class.java)?.commit()
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (!request.isForMainFrame) return
            startActivity(Intent(this@MainActivity, ServersActivity::class.java).putExtra(ServersActivity.EXTRA_UNREACHABLE, true))
            finish()
        }

        // The page's process died (memory, usually): start over, not crash.
        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            recreate()
            return true
        }
    }

    private inner class Chrome : WebChromeClient() {
        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            if (fullscreen != null) {
                callback.onCustomViewHidden()
                return
            }
            fullscreen = view
            leaveFullscreen = callback
            (window.decorView as ViewGroup).addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            showSystemBars(false)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        override fun onHideCustomView() = closeFullscreen()

        // A video waiting to start shows the site's picture, not WebView's grey button.
        override fun getDefaultVideoPoster(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    private fun closeFullscreen() {
        val view = fullscreen ?: return
        (window.decorView as ViewGroup).removeView(view)
        fullscreen = null
        leaveFullscreen?.onCustomViewHidden()
        leaveFullscreen = null
        showSystemBars(true)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun keepScreenOn(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Suppress("DEPRECATION")
    private fun showSystemBars(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (show) window.insetsController?.show(WindowInsets.Type.systemBars())
            else window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            window.decorView.systemUiVisibility = if (show) View.SYSTEM_UI_FLAG_VISIBLE
            else View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    private fun openOutside(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            // Nothing on this device opens it.
        }
    }

    private fun goBack() {
        when {
            fullscreen != null -> closeFullscreen()
            web.canGoBack() -> web.goBack()
            else -> finish()
        }
    }

    @Deprecated("Android 12 and older still call it.")
    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() = goBack()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::web.isInitialized) web.saveState(outState)
    }

    override fun onPause() {
        super.onPause()
        if (!::web.isInitialized) return
        web.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume()
        if (!::web.isInitialized) return
        // It was removed from the list meanwhile.
        if (origin !in Server.load(this)) {
            startActivity(Intent(this, ServersActivity::class.java))
            finish()
            return
        }
        web.onResume()
    }

    override fun onDestroy() {
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }

    companion object {
        /** EXTRA_SERVER is the origin of the server to open. */
        const val EXTRA_SERVER = "server"

        /** EXTRA_CONNECTED says the person has just added the server. */
        const val EXTRA_CONNECTED = "connected"

        fun intent(context: Context, origin: String): Intent =
            Intent(context, MainActivity::class.java).putExtra(EXTRA_SERVER, origin).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

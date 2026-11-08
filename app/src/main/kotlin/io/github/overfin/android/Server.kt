package io.github.overfin.android

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import java.net.URI
import java.net.URISyntaxException

// The servers the app knows, kept on this device only, and what's said of
// their addresses.
object Server {
    private const val PREFS = "server"
    private const val LIST = "list"

    // Version 0.2 and older kept one server's origin.
    private const val OLD_ORIGIN = "origin"

    fun load(context: Context): ServerList {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = prefs.getString(LIST, null)
        if (list != null) return ServerList.decode(list)
        val old = prefs.getString(OLD_ORIGIN, null) ?: return ServerList()
        return ServerList().added(ServerEntry(old, old.removePrefix("https://")))
    }

    fun store(context: Context, list: ServerList) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(LIST, list.encode()).remove(OLD_ORIGIN).apply()
    }

    /** forgetData deletes what the server at origin left on this device: its sign-in and site data. */
    fun forgetData(origin: String) {
        val cookies = CookieManager.getInstance()
        cookies.getCookie(origin)?.split(";")?.forEach { cookie ->
            val name = cookie.substringBefore("=").trim()
            if (name.isNotEmpty()) cookies.setCookie(origin, "$name=; Max-Age=0; Path=/; Secure")
        }
        cookies.flush()
        WebStorage.getInstance().deleteOrigin(origin)
    }

    /** originOf turns what someone typed into an https origin, null when it can't be one. */
    fun originOf(typed: String): String? {
        val text = typed.trim()
        if (text.isEmpty()) return null
        val uri = try {
            URI(if ("://" in text) text else "https://$text")
        } catch (_: URISyntaxException) {
            return null
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        val host = uri.host?.lowercase() ?: return null
        return if (uri.port == -1) "https://$host" else "https://$host:${uri.port}"
    }

    /** logoAddress is the address of a logo the server names by its path, null for anything else. */
    fun logoAddress(origin: String, path: String): String? =
        if (path.startsWith("/") && !path.startsWith("//")) origin + path else null

    /** sameOrigin tells whether url is on the server at origin. */
    fun sameOrigin(url: String, origin: String): Boolean = try {
        val uri = URI(url)
        val port = if (uri.port == -1 || uri.port == 443) "" else ":${uri.port}"
        uri.scheme.equals("https", ignoreCase = true) && "https://${uri.host?.lowercase()}$port" == origin.removeSuffix(":443")
    } catch (_: URISyntaxException) {
        false
    }
}

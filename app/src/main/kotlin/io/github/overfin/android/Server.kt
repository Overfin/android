package io.github.overfin.android

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import java.net.Inet6Address
import java.net.InetAddress
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
            val secure = if (origin.startsWith("https://")) "; Secure" else ""
            if (name.isNotEmpty()) cookies.setCookie(origin, "$name=; Max-Age=0; Path=/$secure")
        }
        cookies.flush()
        WebStorage.getInstance().deleteOrigin(origin)
    }

    /**
     * originOf turns what someone typed into an origin, null when it can't be one: https, or plain
     * http for a private address written as such (a server's gateway on the LAN, which has no
     * HTTPS). Typed without a scheme, a private address is taken as http, anything else as https.
     */
    fun originOf(typed: String): String? {
        val text = typed.trim()
        if (text.isEmpty()) return null
        val uri = try {
            URI(if ("://" in text) text else "https://$text")
        } catch (_: URISyntaxException) {
            return null
        }
        val host = uri.host?.lowercase() ?: return null
        // A port that can't be one (URI takes any number) is no address.
        if (uri.port != -1 && uri.port !in 1..65535) return null
        val scheme = when {
            uri.scheme.equals("https", ignoreCase = true) && ("://" in text || !privateAddress(host)) -> "https"
            uri.scheme.equals("https", ignoreCase = true) -> "http"
            uri.scheme.equals("http", ignoreCase = true) && privateAddress(host) -> "http"
            else -> return null
        }
        return if (uri.port == -1) "$scheme://$host" else "$scheme://$host:${uri.port}"
    }

    private val ipv4 = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")

    /**
     * privateAddress tells whether host is an IP address of a private network, written as such: 10/8,
     * 172.16/12, 192.168/16, 100.64/10 (the shared range VPNs use), and unique-local IPv6 (fc00::/7).
     * A name is never one: what it stands for can't be checked here, nor stays the same.
     */
    fun privateAddress(host: String): Boolean {
        ipv4.matchEntire(host)?.let { m ->
            val b = m.groupValues.drop(1).map { it.toInt() }
            if (b.any { it > 255 }) return false
            return b[0] == 10 || (b[0] == 172 && b[1] in 16..31) || (b[0] == 192 && b[1] == 168) ||
                (b[0] == 100 && b[1] in 64..127)
        }
        val inner = host.removePrefix("[").removeSuffix("]")
        // Only a literal: a name would be looked up.
        if (':' !in inner || inner.any { !(it.isLetterOrDigit() || it == ':' || it == '.') }) return false
        val addr = try {
            InetAddress.getByName(inner)
        } catch (_: Exception) {
            return false
        }
        return addr is Inet6Address && (addr.address[0].toInt() and 0xfe) == 0xfc
    }

    /** logoAddress is the address of a logo the server names by its path, null for anything else. */
    fun logoAddress(origin: String, path: String): String? =
        if (path.startsWith("/") && !path.startsWith("//")) origin + path else null

    /** sameOrigin tells whether url is on the server at origin: the same scheme, host and port. */
    fun sameOrigin(url: String, origin: String): Boolean = try {
        val uri = URI(url)
        val scheme = uri.scheme?.lowercase()
        val default = when (scheme) {
            "https" -> 443
            "http" -> 80
            else -> return false
        }
        val port = if (uri.port == -1 || uri.port == default) "" else ":${uri.port}"
        "$scheme://${uri.host?.lowercase()}$port" == origin.removeSuffix(":$default")
    } catch (_: URISyntaxException) {
        false
    }
}

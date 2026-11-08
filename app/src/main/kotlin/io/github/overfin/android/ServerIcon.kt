package io.github.overfin.android

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

// A server's home screen icon, opening that server. One with its own branding
// offers it, with its name and logo, when it's added; Android always asks the
// person first.
object ServerIcon {
    private const val PREFS = "server_icon"

    // An adaptive icon's bitmap, and the part of it every launcher shows.
    private const val ICON_SIZE = 432
    private const val SAFE_SIZE = 264
    private const val MAX_LOGO_BYTES = 8 * 1024 * 1024

    /** available tells whether this device can add icons at all. */
    fun available(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.getSystemService(ShortcutManager::class.java).isRequestPinShortcutSupported

    /** offer proposes the icon of a server just added, if it has its own branding and wasn't offered yet. */
    fun offer(activity: Activity, origin: String) {
        if (!available(activity)) return
        if (activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(origin, false)) return
        pin(activity, origin, fallbackName = null)
    }

    /** add proposes the icon of a server someone asked for one: without branding, its name and Overfin's icon. */
    fun add(activity: Activity, entry: ServerEntry) {
        if (available(activity)) pin(activity, entry.origin, fallbackName = entry.name)
    }

    /** enable brings back the icons of a server added again. */
    fun enable(context: Context, origin: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.getSystemService(ShortcutManager::class.java).enableShortcuts(listOf(origin))
    }

    /** remove greys out the icons of a server removed, which Android lets only the person delete. */
    fun remove(context: Context, origin: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(origin).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(ShortcutManager::class.java).disableShortcuts(listOf(origin), context.getString(R.string.icon_removed))
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun pin(activity: Activity, origin: String, fallbackName: String?) {
        val shortcuts = activity.getSystemService(ShortcutManager::class.java)
        thread {
            val brand = try {
                JSONObject(String(fetch("$origin/api/v1/branding"))).takeIf { it.optBoolean("custom") }
            } catch (_: IOException) {
                null
            } catch (_: JSONException) {
                null
            }
            val name = brand?.optString("name").orEmpty().ifEmpty { fallbackName.orEmpty() }
            if (name.isEmpty()) return@thread
            val logo = Server.logoAddress(origin, brand?.optString("logo").orEmpty())?.let(::logoIcon)
            if (activity.isDestroyed) return@thread
            val shortcut = ShortcutInfo.Builder(activity, origin)
                .setShortLabel(name)
                .setLongLabel(name)
                .setIcon(logo ?: Icon.createWithResource(activity, R.drawable.ic_launcher))
                .setIntent(Intent(activity, OpenActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra(MainActivity.EXTRA_SERVER, origin))
                .build()
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(origin, true).apply()
            shortcuts.requestPinShortcut(shortcut, null)
        }
    }

    /** logoIcon is the logo centred on the page's color; null when it can't be read. */
    @TargetApi(Build.VERSION_CODES.O)
    private fun logoIcon(address: String): Icon? {
        val bytes = try {
            fetch(address)
        } catch (_: IOException) {
            return null
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SAFE_SIZE) sample *= 2
        val logo = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val scale = SAFE_SIZE.toFloat() / maxOf(logo.width, logo.height)
        val width = (logo.width * scale).toInt()
        val height = (logo.height * scale).toInt()
        val left = (ICON_SIZE - width) / 2
        val top = (ICON_SIZE - height) / 2
        val icon = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(icon).apply {
            drawColor(PAGE_COLOR)
            drawBitmap(logo, null, Rect(left, top, left + width, top + height), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        logo.recycle()
        return Icon.createWithAdaptiveBitmap(icon)
    }

    private fun fetch(address: String): ByteArray {
        val conn = URL(address).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != HttpURLConnection.HTTP_OK) throw IOException("status ${conn.responseCode}")
            return conn.inputStream.use(::readLimited)
        } finally {
            conn.disconnect()
        }
    }

    private fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) return out.toByteArray()
            out.write(buffer, 0, read)
            if (out.size() > MAX_LOGO_BYTES) throw IOException("too large")
        }
    }

    private const val PAGE_COLOR = 0xFF0C0D11.toInt()
}

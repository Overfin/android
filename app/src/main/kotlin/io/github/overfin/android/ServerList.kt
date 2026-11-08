package io.github.overfin.android

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class ServerEntry(val origin: String, val name: String)

// The servers someone added, and the one they last used.
data class ServerList(val entries: List<ServerEntry> = emptyList(), val last: String? = null) {
    operator fun contains(origin: String): Boolean = entries.any { it.origin == origin }

    /** added puts a server in the list, or updates its name, and makes it the last used. */
    fun added(entry: ServerEntry): ServerList {
        val updated = if (entry.origin in this) entries.map { if (it.origin == entry.origin) entry else it } else entries + entry
        return ServerList(updated, entry.origin)
    }

    fun removed(origin: String): ServerList =
        ServerList(entries.filter { it.origin != origin }, last.takeIf { it != origin })

    fun opened(origin: String): ServerList = if (origin in this) copy(last = origin) else this

    fun encode(): String {
        val servers = JSONArray()
        entries.forEach { servers.put(JSONObject().put("origin", it.origin).put("name", it.name)) }
        return JSONObject().put("servers", servers).put("last", last ?: JSONObject.NULL).toString()
    }

    companion object {
        /** decode reads what encode wrote; anything else is an empty list. */
        fun decode(text: String?): ServerList {
            if (text == null) return ServerList()
            return try {
                val json = JSONObject(text)
                val servers = json.getJSONArray("servers")
                val entries = (0 until servers.length()).map {
                    val server = servers.getJSONObject(it)
                    ServerEntry(server.getString("origin"), server.getString("name"))
                }
                val last = if (json.isNull("last")) null else json.getString("last")
                ServerList(entries, last.takeIf { origin -> entries.any { it.origin == origin } })
            } catch (_: JSONException) {
                ServerList()
            }
        }
    }
}

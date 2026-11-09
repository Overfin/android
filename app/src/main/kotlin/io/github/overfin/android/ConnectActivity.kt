package io.github.overfin.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

// Asks which server to add, and checks that it's an Overfin one.
class ConnectActivity : Activity() {
    private lateinit var address: EditText
    private lateinit var error: TextView
    private lateinit var connect: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        address = findViewById(R.id.address)
        error = findViewById(R.id.error)
        connect = findViewById(R.id.connect)
        connect.setOnClickListener { check() }
        address.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_GO) check()
            action == EditorInfo.IME_ACTION_GO
        }
    }

    private fun check() {
        val origin = Server.originOf(address.text.toString())
        if (origin == null) {
            show(R.string.error_address)
            return
        }
        connect.isEnabled = false
        error.visibility = View.GONE
        thread {
            val answer = askInfo(origin)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                connect.isEnabled = true
                if (answer.name != null) open(ServerEntry(origin, answer.name)) else show(answer.problem)
            }
        }
    }

    private fun show(message: Int) {
        error.setText(message)
        error.visibility = View.VISIBLE
    }

    private fun open(entry: ServerEntry) {
        Server.store(this, Server.load(this).added(entry))
        ServerIcon.enable(this, entry.origin)
        startActivity(MainActivity.intent(this, entry.origin).putExtra(MainActivity.EXTRA_CONNECTED, true))
        finish()
    }

    private class Answer(val name: String? = null, val problem: Int = 0)

    companion object {
        /** An Overfin server's info is small: what's beyond this isn't read. */
        private const val INFO_MOST = 64 * 1024

        private fun firstBytes(conn: HttpURLConnection): String = conn.inputStream.use { input ->
            val buffer = ByteArray(INFO_MOST)
            var read = 0
            while (read < INFO_MOST) {
                val n = input.read(buffer, read, INFO_MOST - read)
                if (n < 0) break
                read += n
            }
            String(buffer, 0, read)
        }

        /** askInfo tells the name of the Overfin server at origin, or what's wrong with it. */
        private fun askInfo(origin: String): Answer {
            val conn = URL("$origin/api/v1/info").openConnection() as HttpURLConnection
            return try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return Answer(problem = R.string.error_not_overfin)
                val info = JSONObject(firstBytes(conn))
                info.getString("apiVersion")
                info.getJSONArray("features")
                Answer(name = info.getString("name"))
            } catch (_: IOException) {
                Answer(problem = R.string.error_unreachable)
            } catch (_: JSONException) {
                Answer(problem = R.string.error_not_overfin)
            } finally {
                conn.disconnect()
            }
        }
    }
}

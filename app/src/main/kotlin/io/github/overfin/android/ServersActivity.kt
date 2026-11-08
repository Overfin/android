package io.github.overfin.android

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

// The servers added to this app: open one, add one, remove one.
class ServersActivity : Activity() {
    private lateinit var rows: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_servers)
        rows = findViewById(R.id.rows)
        findViewById<Button>(R.id.add).setOnClickListener { startActivity(Intent(this, ConnectActivity::class.java)) }
        if (intent.getBooleanExtra(EXTRA_UNREACHABLE, false)) {
            findViewById<TextView>(R.id.error).apply {
                setText(R.string.error_unreachable)
                visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showList()
    }

    private fun showList() {
        rows.removeAllViews()
        for (entry in Server.load(this).entries) {
            val row = LayoutInflater.from(this).inflate(R.layout.row_server, rows, false)
            row.findViewById<TextView>(R.id.name).text = entry.name
            row.findViewById<TextView>(R.id.address).text = entry.origin.removePrefix("https://")
            row.findViewById<View>(R.id.open).setOnClickListener {
                startActivity(MainActivity.intent(this, entry.origin))
                finish()
            }
            row.findViewById<Button>(R.id.icon).apply {
                visibility = if (ServerIcon.available(this@ServersActivity)) View.VISIBLE else View.GONE
                setOnClickListener { ServerIcon.add(this@ServersActivity, entry) }
            }
            row.findViewById<Button>(R.id.remove).setOnClickListener { confirmRemove(entry) }
            rows.addView(row)
        }
    }

    private fun confirmRemove(entry: ServerEntry) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.remove_question, entry.name))
            .setMessage(R.string.remove_details)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.remove) { _, _ ->
                Server.store(this, Server.load(this).removed(entry.origin))
                Server.forgetData(entry.origin)
                ServerIcon.remove(this, entry.origin)
                showList()
            }
            .show()
    }

    companion object {
        /** EXTRA_UNREACHABLE says the server just tried couldn't be reached. */
        const val EXTRA_UNREACHABLE = "unreachable"
    }
}

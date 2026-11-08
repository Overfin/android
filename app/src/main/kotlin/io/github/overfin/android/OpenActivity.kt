package io.github.overfin.android

import android.app.Activity
import android.os.Bundle

// What a server's home screen icon starts: it only passes the server on to
// the app, which checks it against its list.
class OpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(MainActivity.intent(this, intent.getStringExtra(MainActivity.EXTRA_SERVER).orEmpty()))
        finish()
    }
}

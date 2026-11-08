# Overfin for Android

The Overfin app for Android phones. It asks for a server's address,
checks that it's an Overfin server, then shows that server's own site, locked
to that server. The site comes from the server, so it updates with it: an app
release is only needed when the app's own part changes.

The app adds what a page can't do by itself in a WebView: fullscreen video,
keeping the screen on while playing, the back button. Addresses must use
https.

It can hold several servers, each with its own account: nothing has to be set
up between them. Long-press the app's icon and choose "Servers" to add, open or
remove one. Removing a server erases its sign-in and site data from the device.
A home screen icon opens its own server. A server with its own branding offers
one, with its name and logo, when it's added, and any server can get one from
the list. Android asks before adding it.

## Install

Download the APK from the [latest release](https://github.com/Overfin/android/releases/latest)
and open it on the phone (Android 7 or later); Android asks to allow
installing from the browser or file manager the first time. Releases are
signed with the same key, so each one installs over the last; the
certificate's SHA-256 fingerprint is
`bf8a87e53ef1b27c13cad9eae8f9b1f1edb98cb85ba9401060c391567fb269c8`.

Password managers fill the sign-in when they're Android's autofill service
(Settings, Passwords and autofill): a browser's own saved passwords don't
reach apps.

## Build

With JDK 21 and the Android SDK (`ANDROID_HOME`, or `sdk.dir` in
`local.properties`):

```
./gradlew test lint assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. `assembleRelease`
signs with the key named by the `overfin.signing` property (a properties
file with `storeFile`, `password` and `alias`), else
`~/.config/overfin-android/release.properties`; without one, the release APK
is unsigned.

## How it's made

Overfin for Android is designed by its maintainer, who makes the architecture, security and product decisions, and written by Claude (Anthropic's AI); Claude's commits carry a `Co-Authored-By: Claude` trailer. The code is checked by its tests and Android's lint; it hasn't had an independent security review yet.

## License

MIT

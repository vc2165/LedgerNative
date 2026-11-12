# Ledger (native)

Personal expense tracker, rebuilt as a native Android app in Kotlin + Jetpack Compose (Material 3).
Same data format as the original Ledger: move your data with *Save a backup* in the old app and *Restore a backup* here.

Every push to `main` builds a signed APK with GitHub Actions and publishes it under **Releases** (needs the `KEYSTORE_BASE64` secret).

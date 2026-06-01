## 2024-06-01 - Android Backup PII Exposure
**Vulnerability:** Android application backup was enabled (`android:allowBackup="true"`), exposing Personally Identifiable Information (PII) such as latitude, longitude, and location name stored in `widget_prefs.xml` via cloud backups or ADB extraction.
**Learning:** Default Android backup behavior includes all SharedPreferences. Storing PII for widgets in SharedPreferences requires disabling backups entirely or explicitly maintaining exclusion rules.
**Prevention:** Always set `android:allowBackup="false"` for apps handling PII in SharedPreferences, and remove redundant backup rule XML files to avoid configuration ambiguity.

# Tourlocate App (Capacitor / Android)

Native Android-Hülle um die Tourlocate-Web-App, gebaut mit [Capacitor](https://capacitorjs.com/).
Die App lädt die **live gehostete Seite** (`https://tourlocate.arcoworld.de`) in einer WebView
und ergänzt – schrittweise – native Funktionen, die als reine Web-App nicht möglich sind
(z. B. „Foto ins Album speichern", später Hintergrund-Standort-Teilen).

Die Web-App selbst liegt im Hauptrepo [`Tourlocate/tourlocate`](https://github.com/Tourlocate/tourlocate);
dieses Repo enthält **nur** die native Hülle.

## Voraussetzungen (auf deinem Rechner)

- **Node.js 18+** (bringt `npm`/`npx` mit)
- **Android Studio** inkl. Android-SDK **und JDK 17**
- Zum Testen ein **Android-Gerät** (USB-Debugging aktiviert) oder ein Emulator

## Einrichten & bauen

```bash
# 1. Abhängigkeiten installieren
npm install

# 2. Android-Projekt erzeugen (legt den Ordner android/ an)
npx cap add android

# 3. Web-Konfiguration ins native Projekt übernehmen
npx cap sync

# 4. In Android Studio öffnen und starten
npx cap open android
```

In Android Studio dann auf **Run** (Gerät/Emulator wählen). Alternativ ein **Debug-APK** auf der Kommandozeile:

```bash
cd android
./gradlew assembleDebug
# Ergebnis: android/app/build/outputs/apk/debug/app-debug.apk
```

Die Debug-APK lässt sich direkt aufs Gerät ziehen (Sideload). Für eine **Release-APK/AAB**
brauchst du einen Signatur-Schlüssel: Android Studio → **Build → Generate Signed Bundle / APK**.
Den Keystore **niemals** einchecken (siehe `.gitignore`).

## Konfiguration (`capacitor.config.json`)

- `server.url` → zeigt auf die live gehostete Seite. Änderst du sie, danach `npx cap sync`.
- `appId` = `de.arcoworld.tourlocate` — die eindeutige App-Kennung (identifiziert die App auf
  dem Gerät / im Store; nicht leichtfertig ändern).
- `appName` = `Tourlocate` — der angezeigte App-Name.

Weil die Hülle die Live-Seite lädt, erscheinen **Änderungen an der Web-App automatisch** –
ohne die App neu zu bauen. Nur **native** Teile (Plugins, Icon, Splashscreen) erfordern einen Neubau.

## Nächste Schritte

- **„Foto ins Album speichern"** als erstes natives Feature (Medien-Plugin + abgesicherter
  Aufruf aus der Web-App, nur wenn sie in der nativen Hülle läuft).
- Später optional: Hintergrund-Standort-Teilen (Android-Vordergrunddienst mit Benachrichtigung),
  eigenes App-Icon / Splashscreen, ggf. Play-Store-Veröffentlichung.

## Lizenz

GPL-3.0-or-later (wie das Hauptprojekt).

# Tourlocate App (Capacitor / Android)

Native Android-Hülle um die Tourlocate-Web-App, gebaut mit [Capacitor](https://capacitorjs.com/).
Die App lädt eine **selbst gehostete Tourlocate-Instanz** (deren URL du selbst konfigurierst,
siehe [Einrichten & bauen](#einrichten--bauen)) in einer WebView und ergänzt – schrittweise –
native Funktionen, die als reine Web-App nicht möglich sind (z. B. „Foto ins Album speichern",
später Hintergrund-Standort-Teilen).

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

Die Server-Adresse muss **nicht** einkompiliert werden: Beim **ersten Start** fragt die App,
mit welcher Tourlocate-Instanz sie sich verbinden soll (Server-Auswahl, wie bei der
Nextcloud-App). Die Wahl wird gespeichert; wechseln geht später über **Abmelden →
„Server wechseln"** (oder notfalls Android-Einstellungen → App-Daten löschen).

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

Die Konfiguration ist **universell** — sie enthält bewusst **keine** Server-URL. Welche
Instanz die App lädt, wählt man **beim ersten Start in der App selbst** (Server-Auswahl);
die Wahl landet in den App-Einstellungen auf dem Gerät, nicht im Code. Dieselbe APK
funktioniert dadurch für **jede** Tourlocate-Instanz.

- `appId` = `org.tourlocate.app` — die eindeutige App-Kennung (identifiziert die App auf
  dem Gerät / im Store; nicht leichtfertig ändern). Für eine **eigene Veröffentlichung** eine
  eigene Kennung wählen.
- `appName` = `Tourlocate` — der angezeigte App-Name.
- Optional: Wer doch eine **fest verdrahtete** Instanz will (ohne Server-Auswahl), kann wie
  gehabt einen `server`-Block mit `url` (inkl. `/app/`) eintragen — dann startet die App
  direkt dort. Nach Änderungen `npx cap sync`.

Weil die Hülle die Live-Seite lädt, erscheinen **Änderungen an der Web-App automatisch** –
ohne die App neu zu bauen. Nur **native** Teile (Plugins, Icon, Splashscreen) erfordern einen Neubau.

## Nächste Schritte

- **„Foto ins Album speichern"** als erstes natives Feature (Medien-Plugin + abgesicherter
  Aufruf aus der Web-App, nur wenn sie in der nativen Hülle läuft).
- Später optional: Hintergrund-Standort-Teilen (Android-Vordergrunddienst mit Benachrichtigung),
  eigenes App-Icon / Splashscreen, ggf. Play-Store-Veröffentlichung.

## Lizenz

GPL-3.0-or-later (wie das Hauptprojekt).

package de.arcoworld.tourlocate;

import android.content.Context;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Server-Picker: merkt sich die vom Nutzer gewählte Tourlocate-Instanz
 * (SharedPreferences). MainActivity liest den Wert beim Start und setzt ihn
 * als Capacitor-server.url — die gewählte Instanz bekommt dadurch die volle
 * native Brücke (Gallery/Geolocation/LiveShare), anders als bei einer bloßen
 * Navigation per allowNavigation.
 *
 * Ohne gespeicherte URL (und ohne server.url in capacitor.config.json) lädt
 * Capacitor die lokale www/-Seite = die Server-Auswahl.
 *
 * set()/clear() starten die Activity neu, damit die Änderung sofort greift.
 * Notausgang bei vertipptem, aber erreichbarem Server: Android-Einstellungen
 * → Apps → Tourlocate → Speicher → „Daten löschen" (setzt die Wahl zurück).
 */
@CapacitorPlugin(name = "ServerConfig")
public class ServerConfigPlugin extends Plugin {

    static final String PREFS = "tl_server_config";
    static final String KEY_URL = "server_url";

    /** Von MainActivity beim Start genutzt (ohne laufende Bridge). */
    static String load(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                  .getString(KEY_URL, null);
    }

    @PluginMethod
    public void get(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("url", load(getContext()));
        call.resolve(ret);
    }

    @PluginMethod
    public void set(PluginCall call) {
        String url = call.getString("url");
        if (url == null || !url.startsWith("https://")) {
            call.reject("bad-url"); // https ist Pflicht
            return;
        }
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY_URL, url).apply();
        call.resolve();
        restart();
    }

    @PluginMethod
    public void clear(PluginCall call) {
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().remove(KEY_URL).apply();
        call.resolve();
        restart();
    }

    // Activity neu aufbauen: MainActivity.onCreate läuft erneut und lädt je
    // nach gespeicherter URL die Instanz bzw. wieder die Server-Auswahl.
    private void restart() {
        getActivity().runOnUiThread(() -> getActivity().recreate());
    }
}

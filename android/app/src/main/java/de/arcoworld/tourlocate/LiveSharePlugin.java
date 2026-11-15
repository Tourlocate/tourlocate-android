package de.arcoworld.tourlocate;

import android.content.Intent;
import android.os.Build;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Brücke zwischen Web-App und LiveShareService. Die Web-App ruft beim Start des
 * Teilens startSharing({url, token, secret}) auf; der native Dienst übernimmt
 * dann den Standort-Upload (auch bei gesperrtem Bildschirm). isRunning() lässt
 * die Web-App einen schon laufenden Dienst erkennen (z. B. nach Seitenwechsel).
 */
@CapacitorPlugin(name = "LiveShare")
public class LiveSharePlugin extends Plugin {

    @PluginMethod
    public void startSharing(PluginCall call) {
        String url = call.getString("url");
        String token = call.getString("token");
        String secret = call.getString("secret");
        if (url == null || token == null || secret == null) {
            call.reject("missing-args");
            return;
        }
        Intent i = new Intent(getContext(), LiveShareService.class);
        i.putExtra("url", url);
        i.putExtra("token", token);
        i.putExtra("secret", secret);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(i);
        } else {
            getContext().startService(i);
        }
        call.resolve();
    }

    @PluginMethod
    public void stopSharing(PluginCall call) {
        getContext().stopService(new Intent(getContext(), LiveShareService.class));
        call.resolve();
    }

    @PluginMethod
    public void isRunning(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("running", LiveShareService.RUNNING);
        call.resolve(ret);
    }

    // Link im SYSTEM-Browser öffnen (statt in der App-WebView). Für den
    // Eigencheck „Ansicht öffnen": So bleibt die App auf der Teilen-Seite,
    // während der Betrachter parallel im Browser läuft.
    @PluginMethod
    public void openExternal(PluginCall call) {
        String url = call.getString("url");
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            call.reject("bad-url");
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject("open-failed", e);
        }
    }
}

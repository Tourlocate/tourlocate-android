package org.tourlocate.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Vordergrund-Dienst, der den Standort NATIV holt und per HTTP an den
 * token-basierten push-Endpunkt schickt — unabhängig von der WebView (die im
 * Hintergrund eingefroren wird). So läuft das Live-Standort-Teilen auch bei
 * gesperrtem Bildschirm weiter.
 */
public class LiveShareService extends Service {

    public static final String CHANNEL_ID = "tl_live_share";
    public static final int NOTIF_ID = 4711;
    public static volatile boolean RUNNING = false;

    private LocationManager lm;
    private LocationListener listener;
    private String url, token, secret;
    private volatile long lastPost = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        url = intent.getStringExtra("url");
        token = intent.getStringExtra("token");
        secret = intent.getStringExtra("secret");
        if (url == null || token == null || secret == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIF_ID, n);
        }
        RUNNING = true;
        startLocation();
        return START_STICKY;
    }

    private void startLocation() {
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override public void onLocationChanged(Location loc) { post(loc); }
            @Override public void onProviderEnabled(String p) {}
            @Override public void onProviderDisabled(String p) {}
            // Für ältere API-Level (deprecated, aber Pflicht zum Überschreiben):
            @Override public void onStatusChanged(String p, int s, Bundle b) {}
        };
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, listener, Looper.getMainLooper());
        } catch (SecurityException | IllegalArgumentException e) { /* keine Freigabe / kein Provider */ }
        try {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 0f, listener, Looper.getMainLooper());
        } catch (SecurityException | IllegalArgumentException e) { /* ok */ }
    }

    // Position an den Server schicken (gedrosselt auf ~3 s), im Hintergrund-Thread.
    private void post(final Location loc) {
        long now = System.currentTimeMillis();
        if (now - lastPost < 3000) return;
        lastPost = now;
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                JSONObject body = new JSONObject();
                body.put("token", token);
                body.put("secret", secret);
                body.put("lat", loc.getLatitude());
                body.put("lng", loc.getLongitude());
                if (loc.hasAccuracy()) body.put("acc", loc.getAccuracy());
                byte[] data = body.toString().getBytes("UTF-8");

                c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = c.getOutputStream()) { os.write(data); }
                int status = c.getResponseCode();
                // Antwort auswerten: Meldet der Server die Sitzung als beendet/
                // abgelaufen (active:false), den Dienst selbst stoppen — sonst
                // liefe er mit Benachrichtigung + GPS endlos weiter.
                if (status == 200) {
                    try (InputStream is = c.getInputStream()) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[1024];
                        int n;
                        while ((n = is.read(buf)) > 0 && bos.size() < 4096) bos.write(buf, 0, n);
                        JSONObject resp = new JSONObject(bos.toString("UTF-8"));
                        if (!resp.optBoolean("active", true)) stopSelf();
                    }
                } else if (status == 403) {
                    // Secret ungültig (z. B. neue Sitzung gestartet) → aufhören.
                    stopSelf();
                }
            } catch (Exception e) {
                // Netzfehler ignorieren — der nächste Standort-Fix versucht es erneut.
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }

    private Notification buildNotification() {
        Intent open = getPackageManager().getLaunchIntentForPackage(getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
            | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = (open != null)
            ? PendingIntent.getActivity(this, 0, open, flags) : null;
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tourlocate")
            .setContentText("Dein Standort wird gerade geteilt")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            // Großes Farb-Logo in der aufgeklappten Benachrichtigung /
            // auf dem Sperrbildschirm; kleines Status-Icon bleibt die
            // (tönbare) Standort-Nadel.
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_logo))
            .setOngoing(true)
            .setContentIntent(pi)
            .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Live-Standort", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Zeigt an, dass dein Standort geteilt wird.");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        RUNNING = false;
        if (lm != null && listener != null) lm.removeUpdates(listener);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}

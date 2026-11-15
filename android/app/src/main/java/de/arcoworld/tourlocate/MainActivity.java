package de.arcoworld.tourlocate;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.CapConfig;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Eigene lokale Plugins registrieren (vor super.onCreate).
        registerPlugin(GalleryPlugin.class);
        registerPlugin(LiveSharePlugin.class);
        registerPlugin(ServerConfigPlugin.class);

        // Server-Picker: Hat der Nutzer eine Instanz gewählt, wird sie hier —
        // vor super.onCreate — als server.url gesetzt. BridgeActivity nutzt
        // ein gesetztes this.config statt der capacitor.config.json; die
        // gewählte Instanz bekommt so die volle native Brücke. Ohne
        // gespeicherte URL bleibt config null → capacitor.config.json (ohne
        // server.url) → lokale www/-Seite = Server-Auswahl.
        String url = ServerConfigPlugin.load(this);
        if (url != null) {
            this.config = new CapConfig.Builder(this).setServerUrl(url).create();
        }

        super.onCreate(savedInstanceState);
    }
}

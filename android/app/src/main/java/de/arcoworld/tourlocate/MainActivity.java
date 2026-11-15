package de.arcoworld.tourlocate;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Eigene lokale Plugins registrieren (vor super.onCreate).
        registerPlugin(GalleryPlugin.class);
        registerPlugin(LiveSharePlugin.class);
        super.onCreate(savedInstanceState);
    }
}

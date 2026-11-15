package org.tourlocate.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Kleines, projekteigenes Plugin: sichert ein Base64-Foto in die ÖFFENTLICHE
 * Galerie unter DCIM/<album> (z. B. DCIM/Tourlocate) — sichtbar für alle
 * Galerie-Apps und unabhängig von der App-Installation.
 *
 * Android 10+ (API 29): via MediaStore mit RELATIVE_PATH, OHNE jede
 * Berechtigung (die App schreibt nur ihren eigenen Eintrag). Android ≤ 9:
 * direkt in den öffentlichen DCIM-Ordner (braucht WRITE_EXTERNAL_STORAGE,
 * im Manifest auf maxSdkVersion=28 begrenzt) + MediaScanner.
 */
@CapacitorPlugin(name = "Gallery")
public class GalleryPlugin extends Plugin {

    @PluginMethod
    public void savePhoto(PluginCall call) {
        String data = call.getString("data");
        String album = call.getString("album", "Tourlocate");
        String fileName = call.getString("fileName", "tourlocate-" + System.currentTimeMillis());
        if (data == null) {
            call.reject("no-data");
            return;
        }

        // Optionales „data:...;base64,"-Präfix abschneiden.
        int comma = data.indexOf(',');
        if (data.startsWith("data:") && comma >= 0) {
            data = data.substring(comma + 1);
        }

        byte[] bytes;
        try {
            bytes = Base64.decode(data, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            call.reject("bad-base64", e);
            return;
        }

        String lower = fileName.toLowerCase();
        if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg")) {
            fileName = fileName + ".jpg";
        }

        try {
            Uri uri = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ? saveViaMediaStore(bytes, album, fileName)
                : saveLegacy(bytes, album, fileName);
            JSObject ret = new JSObject();
            ret.put("uri", uri != null ? uri.toString() : null);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("save-failed", e);
        }
    }

    // Android 10+ : öffentlicher Eintrag über MediaStore, keine Berechtigung nötig.
    private Uri saveViaMediaStore(byte[] bytes, String album, String fileName) throws Exception {
        ContentResolver resolver = getContext().getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/" + album);
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri item = resolver.insert(collection, values);
        if (item == null) throw new Exception("insert-returned-null");

        try (OutputStream os = resolver.openOutputStream(item)) {
            if (os == null) throw new Exception("openOutputStream-null");
            os.write(bytes);
        }

        // Eintrag freigeben (IS_PENDING zurücksetzen), sonst bleibt er unsichtbar.
        values.clear();
        values.put(MediaStore.Images.Media.IS_PENDING, 0);
        resolver.update(item, values, null, null);
        return item;
    }

    // Android ≤ 9 : direkt in DCIM/<album> schreiben und Galerie-Index anstoßen.
    private Uri saveLegacy(byte[] bytes, String album, String fileName) throws Exception {
        File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File dir = new File(dcim, album);
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("mkdirs-failed");
        File out = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(bytes);
        }
        MediaScannerConnection.scanFile(getContext(),
            new String[]{ out.getAbsolutePath() }, new String[]{ "image/jpeg" }, null);
        return Uri.fromFile(out);
    }
}

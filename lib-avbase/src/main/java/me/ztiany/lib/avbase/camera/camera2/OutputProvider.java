package me.ztiany.lib.avbase.camera.camera2;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

public interface OutputProvider {

    String ORIENTATION = "orientation";
    String WORKER = "worker";
    String PREVIEW_SIZE = "preview_size";
    String STREAM_CONFIGURATION = "stream_configuration";

    class Components extends HashMap<String, Object> {

        public <T> T require(String key) {
            //noinspection unchecked
            T t = (T) get(key);
            if (t == null) {
                throw new NullPointerException("no value associated with " + key);
            }
            return t;
        }
    }

    void onAttach(@NonNull Camera2Handle camera2Handle, @NonNull Components components);

    @Nullable
    default Surface provideSurface() {
        return null;
    }

    void onDetach();

}

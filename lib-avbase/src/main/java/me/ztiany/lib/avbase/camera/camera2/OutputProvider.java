package me.ztiany.lib.avbase.camera.camera2;

import androidx.annotation.NonNull;

import java.util.HashMap;

public interface OutputProvider {

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

    String ORIENTATION = "ORIENTATION";
    String WORKER = "WORKER";
    String PREVIEW = "PREVIEW";
    String STREAM_CONFIGURATION = "StreamConfiguration";

    void onAttach(@NonNull Camera2Handle camera2Handle, @NonNull Components components);

    void onDetach();

}

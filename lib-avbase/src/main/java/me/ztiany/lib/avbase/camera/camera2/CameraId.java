package me.ztiany.lib.avbase.camera.camera2;

import androidx.annotation.StringDef;

@StringDef({
        CameraId.BACK,
        CameraId.FRONT
})
public @interface CameraId {

    String FRONT = "1";

    String BACK = "0";

}

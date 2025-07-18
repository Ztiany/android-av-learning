package me.ztiany.lib.avbase.camera.camera2;

import androidx.annotation.NonNull;

import java.util.List;

public interface CameraSelector {

    String selectCamera(@NonNull List<String> cameraIdList);

    CameraSelector Front = new Selector(CameraId.FRONT);

    CameraSelector Back = new Selector(CameraId.BACK);

}

final class Selector implements CameraSelector {

    private final String targetId;

    public Selector(String target) {
        targetId = target;
    }

    @Override
    public String selectCamera(@NonNull List<String> cameraIdList) {
        for (String cameraId : cameraIdList) {
            if (targetId.equals(cameraId)) {
                return cameraId;
            }
        }
        return cameraIdList.isEmpty() ? "" : cameraIdList.get(0);
    }
}
package me.ztiany.androidav.opengl.oglcamera;


import android.util.Size;

public interface CameraListener {

    /**
     * 当打开时执行
     *
     * @param camera             相机实例
     * @param cameraId           相机ID
     * @param displayOrientation 相机预览旋转角度
     */
    void onCameraOpened(ICamera camera, String cameraId, Size previewSize, int displayOrientation);

    /**
     * 当相机关闭时执行
     */
    default void onCameraClosed() {

    }

    /**
     * 当出现异常时执行
     *
     * @param e 相机相关异常
     */
    default void onCameraError(Exception e) {
    }

}
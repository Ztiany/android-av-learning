package me.ztiany.androidav.opengl.oglcamera;

import android.content.Context;
import android.graphics.Point;

import timber.log.Timber;

public final class CameraBuilder {

    /**
     * 是否镜像显示，只支持textureView
     */
    boolean isMirror;

    /**
     * 指定的相机ID
     */
    String specificCameraId;

    /**
     * 事件回调
     */
    CameraListener mCameraListener;

    /**
     * 屏幕的长宽，在选择最佳相机比例时用到
     */
    Point previewViewSize;

    /**
     * 传入getWindowManager().getDefaultDisplay().getRotation()的值即可
     */
    int rotation;

    /**
     * 指定的预览宽高，若系统支持则会以这个预览宽高进行预览
     */
    Point targetPreviewSize;

    /**
     * 最大分辨率
     */
    Point maxPreviewSize;

    /**
     * 最小分辨率
     */
    Point minPreviewSize;

    /**
     * 上下文，用于获取CameraManager
     */
    Context context;

    public CameraBuilder() {
    }

    public CameraBuilder isMirror(boolean val) {
        isMirror = val;
        return this;
    }

    public CameraBuilder targetPreviewSize(Point val) {
        targetPreviewSize = val;
        return this;
    }

    public CameraBuilder maxPreviewSize(Point val) {
        maxPreviewSize = val;
        return this;
    }

    public CameraBuilder minPreviewSize(Point val) {
        minPreviewSize = val;
        return this;
    }

    public CameraBuilder previewViewSize(Point val) {
        previewViewSize = val;
        return this;
    }

    public CameraBuilder rotation(int val) {
        rotation = val;
        return this;
    }

    public CameraBuilder specificCameraId(String val) {
        specificCameraId = val;
        return this;
    }

    public CameraBuilder cameraListener(CameraListener val) {
        mCameraListener = val;
        return this;
    }

    public CameraBuilder context(Context val) {
        context = val;
        return this;
    }

    public CameraOperator build(String version) {
        if (previewViewSize == null) {
            Timber.e("previewViewSize is null, now use default previewSize");
        }
        if (mCameraListener == null) {
            Timber.e("camera2Listener is null, callback will not be called");
        }

        if (maxPreviewSize != null && minPreviewSize != null) {
            if (maxPreviewSize.x < minPreviewSize.x || maxPreviewSize.y < minPreviewSize.y) {
                throw new IllegalArgumentException("maxPreviewSize must greater than minPreviewSize");
            }
        }

        switch (version) {
            case "1":
                return new Camera1Operator(this);
            case "2":
                return new Camera2Operator(this);
            case "x":
                return new CameraXOperator(this);
        }

        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        return "CameraBuilder{" +
                "isMirror=" + isMirror +
                ", specificCameraId='" + specificCameraId + '\'' +
                ", camera2Listener=" + mCameraListener +
                ", previewViewSize=" + previewViewSize +
                ", rotation=" + rotation +
                ", previewSize=" + targetPreviewSize +
                ", maxPreviewSize=" + maxPreviewSize +
                ", minPreviewSize=" + minPreviewSize +
                ", context=" + context +
                '}';
    }

}

package me.ztiany.androidav.stream.livecamera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.IOException;

import timber.log.Timber;

public class LocalSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private Camera.Size size;
    private Camera mCamera;
    private boolean mStopped;

    private EncodePushLiveH265 mEncodePushLiveH265;

    public LocalSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        startPreview();
    }

    private void startPreview() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(Constants.HEIGHT, Constants.WIDTH);
        parameters.setPreviewFormat(ImageFormat.NV21);
        printAllSupportedPreviewSize(parameters);
        try {
            mCamera.setDisplayOrientation(90);
            mCamera.setParameters(parameters);
            size = parameters.getPreviewSize();
            byte[] buffer = new byte[size.width * size.height * 3 / 2];
            Timber.d("size: width = %d, height = %d", size.width, size.height);
            mCamera.addCallbackBuffer(buffer);

            mCamera.setPreviewDisplay(getHolder());
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printAllSupportedPreviewSize(Camera.Parameters parameters) {
        Timber.d("supportedPreviewSize:");
        for (Camera.Size supportedPreviewSize : parameters.getSupportedPreviewSizes()) {
            Timber.d(supportedPreviewSize.width + "x" + supportedPreviewSize.height);
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Timber.d("surfaceChanged() called with: holder = [" + holder + "], format = [" + format + "], width = [" + width + "], height = [" + height + "]");
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
    }

    public Camera.Size getSize() {
        return size;
    }

    public void setEncodePushLiveH265(EncodePushLiveH265 encodePushLiveH265) {
        this.mEncodePushLiveH265 = encodePushLiveH265;
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (mEncodePushLiveH265 != null && !mStopped) {
            mEncodePushLiveH265.encodeFrame(bytes);
        }
        mCamera.addCallbackBuffer(bytes);
    }

    public void stop() {
        mStopped = true;
    }

}
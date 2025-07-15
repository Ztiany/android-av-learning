package me.ztiany.androidav.opengl.oglcamera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class Camera2Operator implements CameraOperator {

    private final Point maxPreviewSize;
    private final Point minPreviewSize;

    private final int rotation;
    private final Point previewViewSize;
    private final Point specificPreviewSize;

    private String mCameraId;

    private String specificCameraId;
    private CameraListener mCameraListener;

    private Context context;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    private Size mPreviewSize;

    Camera2Operator(CameraBuilder builder) {
        specificCameraId = builder.specificCameraId;
        mCameraListener = builder.mCameraListener;
        rotation = builder.rotation;
        previewViewSize = builder.previewViewSize;
        specificPreviewSize = builder.targetPreviewSize;
        maxPreviewSize = builder.maxPreviewSize;
        minPreviewSize = builder.minPreviewSize;
        context = builder.context;

        Timber.d("camera builder %s", builder.toString());
    }

    @Override
    public void switchCamera() {
        if (CAMERA_ID_BACK.equals(mCameraId)) {
            specificCameraId = CAMERA_ID_FRONT;
        } else if (CAMERA_ID_FRONT.equals(mCameraId)) {
            specificCameraId = CAMERA_ID_BACK;
        }
        stop();
        start();
    }

    private int getCameraOrientation(int rotation, String cameraId) {
        int degrees = rotation * 90;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }
        int result;

        if (CAMERA_ID_FRONT.equals(cameraId)) {
            result = (mSensorOrientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (mSensorOrientation - degrees + 360) % 360;
        }

        Timber.i("getCameraOri: rotation = " + rotation + " result = " + result + " mSensorOrientation = " + mSensorOrientation);
        return result;
    }

    private final CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Timber.i("onOpened: ");
            // This method is called when the camera is opened. We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;

            if (mCameraListener != null) {
                ContextCompat.getMainExecutor(context).execute(() ->
                        mCameraListener.onCameraOpened(new Camera2(mCameraDevice), mCameraId, mPreviewSize, getCameraOrientation(rotation, mCameraId))
                );
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Timber.i("onDisconnected: ");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            if (mCameraListener != null) {
                mCameraListener.onCameraClosed();
            }
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Timber.i("onError: ");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;

            if (mCameraListener != null) {
                mCameraListener.onCameraError(new Exception("error occurred, code is " + error));
            }
        }

    };

    public void startPreview(@NonNull SurfaceTexture surfaceTexture) {
        if (mCameraDevice == null) {
            return;
        }
        createCameraPreviewSession(surfaceTexture);
    }

    private final CameraCaptureSession.StateCallback mCaptureStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            Timber.i("onConfigured: ");
            // The camera is already closed
            if (null == mCameraDevice) {
                return;
            }

            // When the session is ready, we start displaying the preview.
            mCaptureSession = cameraCaptureSession;
            try {
                mCaptureSession.setRepeatingRequest(
                        mPreviewRequestBuilder.build(),
                        new CameraCaptureSession.CaptureCallback() {
                        },
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                Timber.e(e, "setRepeatingRequest");
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Timber.i("onConfigureFailed: ");
            if (mCameraListener != null) {
                mCameraListener.onCameraError(new Exception("configureFailed"));
            }
        }
    };

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private int mSensorOrientation;

    private Size getBestSupportedSize(List<Size> sizes) {

        Size defaultSize = sizes.get(0);
        Size[] tempSizes = sizes.toArray(new Size[0]);

        // 降序
        Arrays.sort(tempSizes, (o1, o2) -> {
            if (o1.getWidth() > o2.getWidth()) {
                return -1;
            } else if (o1.getWidth() == o2.getWidth()) {
                return o1.getHeight() > o2.getHeight() ? -1 : 1;
            } else {
                return 1;
            }
        });
        sizes = new ArrayList<>(Arrays.asList(tempSizes));

        Timber.d("getBestSupportedSize: all sizes camera supports");
        for (Size size : sizes) {
            Timber.d("%dx%d", size.getWidth(), size.getHeight());
        }

        Timber.d("getBestSupportedSize: filter undesired");
        for (int i = sizes.size() - 1; i >= 0; i--) {
            if (maxPreviewSize != null) {
                if (sizes.get(i).getWidth() > maxPreviewSize.x || sizes.get(i).getHeight() > maxPreviewSize.y) {
                    Size remove = sizes.remove(i);
                    Timber.d("remove %dx%d", remove.getWidth(), remove.getHeight());
                    continue;
                }
            }
            if (minPreviewSize != null) {
                if (sizes.get(i).getWidth() < minPreviewSize.x || sizes.get(i).getHeight() < minPreviewSize.y) {
                    Size remove = sizes.remove(i);
                    Timber.d("remove %dx%d", remove.getWidth(), remove.getHeight());
                }
            }
        }

        if (sizes.isEmpty()) {
            String msg = "can not find suitable previewSize, now using default";
            if (mCameraListener != null) {
                Timber.e(msg);
                mCameraListener.onCameraError(new Exception(msg));
            }
            return defaultSize;
        }

        Size bestSize = sizes.get(0);
        float previewViewRatio;
        if (previewViewSize != null) {//如果设置了预览 View 的宽高，就用它来做对比
            previewViewRatio = (float) previewViewSize.x / (float) previewViewSize.y;
        } else {//否则就用最佳预览来做对比
            previewViewRatio = (float) bestSize.getWidth() / (float) bestSize.getHeight();
        }
        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio;
        }
        Timber.d("getBestSupportedSize: previewViewRatio = 1 / previewViewRatio = %f", previewViewRatio);

        for (Size s : sizes) {
            //如果有符合目标尺寸的，就用目标尺寸。
            if (specificPreviewSize != null && specificPreviewSize.x == s.getWidth() && specificPreviewSize.y == s.getHeight()) {
                Timber.d("getBestSupportedSize: returning %dx%d", s.getWidth(), s.getHeight());
                return s;
            }
            /*
            否则就找到最小比例误差的那个尺寸。
            get the minimal deviation size.
                best:
                   2160 / 1080 = 1.996...
                    1 / 1.996 = 0.502...

                  option:
                    1920x1080
                        1080 / 1920 = 0.563...
                        0.563 - 0.502 =
             */
            if (Math.abs((s.getHeight() / (float) s.getWidth()) - previewViewRatio) < Math.abs(bestSize.getHeight() / (float) bestSize.getWidth() - previewViewRatio)) {
                bestSize = s;
            }
        }

        Timber.d("getBestSupportedSize: returning %dx%d", bestSize.getWidth(), bestSize.getHeight());
        return bestSize;
    }

    @Override
    public synchronized void start() {
        if (mCameraDevice != null) {
            return;
        }
        if (mBackgroundHandler != null) {
            return;
        }

        startBackgroundThread();
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        openCamera();
    }

    @Override
    public synchronized void stop() {
        if (mCameraDevice == null) {
            return;
        }
        closeCamera();
        stopBackgroundThread();
    }

    @Override
    public void release() {
        stop();
        mCameraListener = null;
        context = null;
    }

    private void setUpCameraOutputs(CameraManager cameraManager) {
        try {
            if (configCameraParams(cameraManager, specificCameraId)) {
                return;
            }
            for (String cameraId : cameraManager.getCameraIdList()) {
                if (configCameraParams(cameraManager, cameraId)) {
                    return;
                }
            }
        } catch (CameraAccessException e) {
            Timber.e(e, "setUpCameraOutputs");
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            if (mCameraListener != null) {
                mCameraListener.onCameraError(e);
            }
        }
    }

    private boolean configCameraParams(CameraManager manager, String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            return false;
        }
        mPreviewSize = getBestSupportedSize(new ArrayList<>(Arrays.asList(map.getOutputSizes(SurfaceTexture.class))));
        Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (sensorOrientation != null) {
            mSensorOrientation = sensorOrientation;
        }
        mCameraId = cameraId;
        return true;
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        setUpCameraOutputs(cameraManager);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            cameraManager.openCamera(mCameraId, mDeviceStateCallback, mBackgroundHandler);
        } catch (CameraAccessException | InterruptedException e) {
            if (mCameraListener != null) {
                mCameraListener.onCameraError(e);
            }
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mCameraListener != null) {
                mCameraListener.onCameraClosed();
            }
        } catch (InterruptedException e) {
            if (mCameraListener != null) {
                mCameraListener.onCameraError(e);
            }
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Timber.e(e, "stopBackgroundThread");
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession(SurfaceTexture surfaceTexture) {
        try {
            // We configure the size of default buffer to be the size of camera preview we want.
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(surfaceTexture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mPreviewRequestBuilder.addTarget(surface);
            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(
                    Collections.singletonList(surface),
                    mCaptureStateCallback,
                    mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            Timber.e(e, "createCameraPreviewSession");
        }
    }

}
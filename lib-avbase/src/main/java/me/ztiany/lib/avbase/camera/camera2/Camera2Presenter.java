package me.ztiany.lib.avbase.camera.camera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class Camera2Presenter {

    private final int mRotation;

    private final boolean mIsMirror;

    private Size mPreviewSize;

    @NonNull
    private final SizeSelector mSizeSelector;

    @Nullable
    private OutputProvider mOutputProvider;

    @CameraId
    private String mCameraId;

    @CameraId
    private String mSpecifiedCameraId;

    private final CameraSelector mCameraSelector;

    private Camera2Listener mCamera2Listener;

    private TextureView mTextureView;

    private final boolean mFitPreview;

    private Context mContext;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    private Camera2Presenter(Builder builder) {
        mTextureView = builder.previewView;
        mFitPreview = builder.fitPreview;

        mSpecifiedCameraId = builder.specifiedCameraId;
        mCameraSelector = builder.cameraSelector;

        mRotation = builder.rotation;
        mIsMirror = builder.isMirror;

        mOutputProvider = builder.outputProvider;
        mCamera2Listener = builder.mCamera2Listener;
        mSizeSelector = builder.sizeSelector;

        mContext = builder.context;

        if (mIsMirror) {
            mTextureView.setScaleX(-1);
        }

        Timber.d("camera builder %s", builder.toString());
    }

    public void switchCamera() {
        if (CameraId.BACK.equals(mCameraId)) {
            mSpecifiedCameraId = CameraId.FRONT;
        } else if (CameraId.FRONT.equals(mCameraId)) {
            mSpecifiedCameraId = CameraId.BACK;
        }
        stop();
        start();
    }

    private int getCameraOrientation(int rotation, String cameraId) {
        Timber.d("getCameraOrientation() called with: rotation = [" + rotation + "], cameraId = [" + cameraId + "]");
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

        if (CameraId.FRONT.equals(cameraId)) {
            result = (mSensorOrientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (mSensorOrientation - degrees + 360) % 360;
        }

        Timber.d("getCameraOrientation: rotation = " + rotation + " result = " + result + " sensorOrientation = " + mSensorOrientation);
        return result;
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture texture, int width, int height) {
            Timber.d("onSurfaceTextureAvailable: %d, %d", width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture texture, int width, int height) {
            Timber.d("onSurfaceTextureSizeChanged: %d, %d", width, height);
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture texture) {
            Timber.d("onSurfaceTextureDestroyed: ");
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture texture) {
        }

    };

    private final CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Timber.d("StateCallback.onOpened()");

            mCameraOpenCloseLock.release();

            // This method is called when the camera is opened. We start camera preview here.
            mCameraDevice = cameraDevice;
            createPreviewSession(null, null);
            if (mCamera2Listener != null) {
                mCamera2Listener.onCameraOpened(
                        cameraDevice,
                        mCameraId,
                        mPreviewSize,
                        getCameraOrientation(mRotation, mCameraId),
                        mIsMirror
                );
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Timber.d("StateCallback.onDisconnected()");

            mCameraOpenCloseLock.release();

            closeCameraSession();
            closeCameraDevice();

            if (mCamera2Listener != null) {
                mCamera2Listener.onCameraClosed();
            }
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Timber.d("StateCallback.onError(): error = %d", error);

            mCameraOpenCloseLock.release();

            closeCameraSession();
            closeCameraDevice();

            if (mCamera2Listener != null) {
                mCamera2Listener.onCameraError(new Exception("error occurred, code is " + error));
            }
        }

    };

    private final Camera2Handle mCamera2Handle = new Camera2Handle() {

        @Override
        public void startCapturingCameraSession(
                @NonNull Surface surface,
                CameraCaptureSession.StateCallback stateCallback
        ) {
            createPreviewSession(surface, stateCallback);
        }

        @Override
        public void stopCapturingCameraSession() {
            createPreviewSession(null, null);
        }
    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    public synchronized void start() {
        if (mCameraDevice != null) {
            return;
        }
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    public synchronized void stop() {
        if (mCameraDevice == null) {
            return;
        }
        closeCamera();
        stopBackgroundThread();
    }

    public void release() {
        stop();
        mTextureView = null;
        mCamera2Listener = null;
        mContext = null;
    }

    private boolean setUpCameraOutputs(CameraManager cameraManager) {
        try {
            if (mCameraSelector != null) {
                String[] cameraIdList = cameraManager.getCameraIdList();
                String targetId = mCameraSelector.selectCamera(Arrays.asList(cameraIdList));
                if (configCameraParams(cameraManager, targetId)) {
                    return true;
                }
            }

            if (!TextUtils.isEmpty(mSpecifiedCameraId) && configCameraParams(cameraManager, mSpecifiedCameraId)) {
                return true;
            }
            for (String cameraId : cameraManager.getCameraIdList()) {
                if (configCameraParams(cameraManager, cameraId)) {
                    return true;
                }
            }
        } catch (CameraAccessException | IllegalArgumentException exception) {
            Timber.e(exception, "setUpCameraOutputs");
            if (mCamera2Listener != null) {
                mCamera2Listener.onCameraError(exception);
            }
        } catch (NullPointerException nullPointerException) {
            Timber.e(nullPointerException, "setUpCameraOutputs");
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            if (mCamera2Listener != null) {
                mCamera2Listener.onCameraError(nullPointerException);
            }
        }
        return false;
    }

    private boolean configCameraParams(
            CameraManager manager,
            @CameraId String cameraId
    ) throws CameraAccessException, IllegalArgumentException {
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        StreamConfigurationMap configurationMap = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        );
        if (configurationMap == null) {
            return false;
        }

        Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (sensorOrientation != null) {
            mSensorOrientation = sensorOrientation;
        }
        this.mCameraId = cameraId;

        mPreviewSize = mSizeSelector.getBestSupportedSize(new ArrayList<>(Arrays.asList(configurationMap.getOutputSizes(SurfaceTexture.class))));

        if (mOutputProvider != null) {
            mOutputProvider.onAttach(mCamera2Handle, new OutputProvider.Components() {
                {
                    put(OutputProvider.ORIENTATION, getCameraOrientation(mRotation, cameraId));
                    put(OutputProvider.PREVIEW_SIZE, mPreviewSize);
                    put(OutputProvider.WORKER, mBackgroundHandler);
                    put(OutputProvider.STREAM_CONFIGURATION, configurationMap);
                }
            });
        }
        return true;
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Timber.e("openCamera failed, no camera permission!");
            return;
        }

        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Timber.e("Time out waiting to lock camera opening.");
                return;
            }
            if (setUpCameraOutputs(cameraManager)) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
                cameraManager.openCamera(mCameraId, mDeviceStateCallback, mBackgroundHandler);
            }else {
                mCameraOpenCloseLock.release();
            }
        } catch (CameraAccessException | SecurityException | IllegalArgumentException exception) {
            Timber.e(exception, "openCamera");
            mCameraOpenCloseLock.release();
            if (mCamera2Listener != null) {
                mCamera2Listener.onCameraError(exception);
            }
        } catch (InterruptedException exception) {
            Timber.e(exception, "openCamera");
            if (mCamera2Listener != null) {
                mCamera2Listener.onCameraError(exception);
            }
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();

            closeCameraSession();

            if (null != mOutputProvider) {
                mOutputProvider.onDetach();
                mOutputProvider = null;
            }

            closeCameraDevice();

            if (mCamera2Listener != null) {
                mCamera2Listener.onCameraClosed();
            }
        } catch (InterruptedException e) {
            if (mCamera2Listener != null) {
                mCamera2Listener.onCameraError(e);
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
        } catch (InterruptedException interruptedException) {
            Timber.e(interruptedException, "stopBackgroundThread");
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createPreviewSession(
            @Nullable Surface outputSurface,
            @Nullable CameraCaptureSession.StateCallback callback
    ) {
        closeCameraSession();

        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        if (texture == null) {
            return;
        }

        try {
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            );

            List<Surface> targets = new ArrayList<>();
            targets.add(surface);
            // added through CameraHandle
            if (outputSurface != null) {
                targets.add(outputSurface);
            }
            // added through provideSurface
            if (mOutputProvider != null) {
                Surface providedSurface = mOutputProvider.provideSurface();
                if (providedSurface != null && !targets.contains(providedSurface)) {
                    targets.add(providedSurface);
                }
            }

            for (Surface target : targets) {
                mPreviewRequestBuilder.addTarget(target);
            }

            // Here, we create a CameraCaptureSession for camera preview.
            CameraCaptureSession.StateCallback configureFailed = new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Timber.d("StateCallback.onConfigured()");
                    // The camera is already closed
                    if (null == mCameraDevice) {
                        return;
                    }
                    startPreview(session);
                    if (callback != null) {
                        callback.onConfigured(session);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Timber.d("StateCallback.onConfigureFailed()");

                    if (mCamera2Listener != null) {
                        mCamera2Listener.onCameraError(new Exception("configureFailed"));
                    }

                    if (callback != null) {
                        callback.onConfigureFailed(session);
                    }
                }
            };

            mCameraDevice.createCaptureSession(targets, configureFailed, mBackgroundHandler);
        } catch (CameraAccessException cameraAccessException) {
            Timber.e(cameraAccessException, "createCameraPreviewSession");
        }
    }

    private void startPreview(@NonNull CameraCaptureSession cameraCaptureSession) {
        if (mCameraDevice == null) {
            Timber.w("startPreview is called but camera is closed.");
            return;
        }

        // When the session is ready, we start displaying the preview.
        mCaptureSession = cameraCaptureSession;

        try {
            mCaptureSession.setRepeatingRequest(
                    mPreviewRequestBuilder.build(),
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                            Timber.d("onCaptureFailed");
                        }
                    },
                    mBackgroundHandler
            );
        } catch (Exception exception) {
            Timber.e(exception, "setRepeatingRequest");
        }
    }

    /**
     * close camera session.
     */
    private void closeCameraSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    private void closeCameraDevice() {
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined and the
     * size of `mTextureView` is fixed.
     *
     * <p>
     * This method's purpose is to correctly display the camera preview on the screen.
     * </p>
     * <p>
     * It solves two main problems:
     *
     * <ol>
     *     <li>Orientation Mismatch: The camera sensor has a fixed orientation (usually landscape),
     *     but the phone can be held in any orientation (portrait, landscape, upside down). This
     *     method applies the necessary rotation to the camera preview so that it appears upright to
     *     the user. For instance, if the phone is held in portrait (ROTATION_90), it rotates the
     *     preview stream by -90 or 270 degrees.</li>
     *     <li>Aspect Ratio Mismatch: The resolution of the camera preview (mPreviewSize) often has
     *     a different aspect ratio than the TextureView displaying it on the screen. To prevent the
     *     image from looking stretched or squashed, this method calculates a transformation matrix.
     *     Specifically, it scales the preview to completely fill the view, which may involve cropping
     *     parts of the image that don't fit. This is often called a "center-crop" effect.</li>
     * </ol>
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        if (!mFitPreview) {
            Timber.w("configureTransform is disabled!");
            return;
        }

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());

        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        float scale = Math.max((float) viewHeight / mPreviewSize.getHeight(), (float) viewWidth / mPreviewSize.getWidth());

        if (Surface.ROTATION_90 == mRotation || Surface.ROTATION_270 == mRotation) {
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate((90 * (mRotation - 2)) % 360, centerX, centerY);
            Timber.d("configureTransform when 90/270, scale = %f, rotate = %d", scale, (90 * (mRotation - 2)) % 360);
        } else if (Surface.ROTATION_180 == mRotation) {
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(180, centerX, centerY);
            Timber.d("configureTransform when 180, scale = %f, rotate = 180", scale);
        } else if (Surface.ROTATION_0 == mRotation) {
            matrix.postScale(scale, scale, centerX, centerY);
            Timber.d("configureTransform when 0, scale = %f, rotate = 0", scale);
        }

        Timber.d("camera orientation = %d, degree = %d", getCameraOrientation(mRotation, mCameraId), mRotation * 90);
        mTextureView.setTransform(matrix);
    }

    public static final class Builder {

        /**
         * 上下文，用于获取 CameraManager。
         */
        private Context context;

        /**
         * 传入 getWindowManager().getDefaultDisplay().getRotation() 的值即可。
         */
        private int rotation;

        /**
         * 是否镜像显示，只支持 textureView。
         */
        private boolean isMirror;

        /**
         * 预览显示的 view，目前仅支持 textureView。
         */
        private TextureView previewView;

        /**
         * transform preview so it correctly displays the camera preview on the screen.
         */
        private boolean fitPreview = true;

        /**
         * 相机选择器。
         */
        private CameraSelector cameraSelector;

        /**
         * 指定的相机 ID。
         */
        @CameraId
        private String specifiedCameraId;

        /**
         * 事件回调
         */
        private Camera2Listener mCamera2Listener;

        private OutputProvider outputProvider;

        private SizeSelector sizeSelector;

        public Builder() {
        }

        public Builder previewOn(TextureView textureView) {
            this.previewView = textureView;
            return this;
        }

        public Builder fitPreview(boolean fitPreview) {
            this.fitPreview = fitPreview;
            return this;
        }

        public Builder isMirror(boolean isMirror) {
            this.isMirror = isMirror;
            return this;
        }

        public Builder rotation(int rotation) {
            this.rotation = rotation;
            return this;
        }

        public Builder cameraId(@CameraId String cameraId) {
            this.specifiedCameraId = cameraId;
            return this;
        }

        public Builder cameraSelector(CameraSelector cameraSelector) {
            this.cameraSelector = cameraSelector;
            return this;
        }

        public Builder cameraListener(Camera2Listener val) {
            this.mCamera2Listener = val;
            return this;
        }

        public Builder outputProvider(OutputProvider outputProvider) {
            this.outputProvider = outputProvider;
            return this;
        }

        public Builder sizeSelector(SizeSelector sizeSelector) {
            this.sizeSelector = sizeSelector;
            return this;
        }

        public Builder context(Context val) {
            this.context = val;
            return this;
        }

        public Camera2Presenter build() {
            if (mCamera2Listener == null) {
                Timber.w("camera2Listener is null, callback will not be called!");
            }
            if (sizeSelector == null) {
                throw new NullPointerException("you must provide a sizeSelector!");
            }
            if (previewView == null) {
                throw new NullPointerException("you must preview on a textureView or a surfaceView!");
            }
            return new Camera2Presenter(this);
        }

        @NonNull
        @Override
        public String toString() {
            return "Builder{" +
                    "previewDisplayView=" + previewView +
                    ", isMirror=" + isMirror +
                    ", specificCameraId='" + specifiedCameraId + '\'' +
                    ", rotation=" + rotation +
                    ", context=" + context +
                    '}';
        }

    }

}
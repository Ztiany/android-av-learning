package me.ztiany.androidav.camera.camera2;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class Camera2Helper {

    private final Point maxPreviewSize;
    private final Point minPreviewSize;

    private final int rotation;
    private final Point previewViewSize;
    private final Point specificPreviewSize;
    private final boolean isMirror;

    public static final String CAMERA_ID_FRONT = "1";
    public static final String CAMERA_ID_BACK = "0";

    private String mCameraId;

    private String specificCameraId;
    private Camera2Listener camera2Listener;
    private TextureView mTextureView;

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

    private Camera2Helper(Camera2Helper.Builder builder) {
        mTextureView = builder.previewDisplayView;
        specificCameraId = builder.specificCameraId;
        camera2Listener = builder.camera2Listener;
        rotation = builder.rotation;
        previewViewSize = builder.previewViewSize;
        specificPreviewSize = builder.previewSize;
        maxPreviewSize = builder.maxPreviewSize;
        minPreviewSize = builder.minPreviewSize;
        isMirror = builder.isMirror;
        context = builder.context;

        if (isMirror) {
            mTextureView.setScaleX(-1);
        }

        Timber.d("camera builder %s", builder.toString());
    }

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

        Timber.i("getCameraOrientation: rotation = " + rotation + " result = " + result + " mSensorOrientation = " + mSensorOrientation);
        return result;
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture texture, int width, int height) {
            Timber.i("onSurfaceTextureAvailable: %d, %d", width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture texture, int width, int height) {
            Timber.i("onSurfaceTextureSizeChanged: %d, %d", width, height);
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture texture) {
            Timber.i("onSurfaceTextureDestroyed: ");
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture texture) {
        }

    };

    private final CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Timber.i("onOpened: ");
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
            if (camera2Listener != null) {
                camera2Listener.onCameraOpened(cameraDevice, mCameraId, mPreviewSize, getCameraOrientation(rotation, mCameraId), isMirror);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Timber.i("onDisconnected: ");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            if (camera2Listener != null) {
                camera2Listener.onCameraClosed();
            }
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Timber.i("onError: ");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;

            if (camera2Listener != null) {
                camera2Listener.onCameraError(new Exception("error occurred, code is " + error));
            }
        }

    };

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
            if (camera2Listener != null) {
                camera2Listener.onCameraError(new Exception("configureFailed"));
            }
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

    private ImageReader mImageReader;

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

    private Size getBestSupportedSize(List<Size> sizes) {

        Size defaultSize = sizes.get(0);
        Size[] tempSizes = sizes.toArray(new Size[0]);

        /*降序*/
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
            if (camera2Listener != null) {
                Timber.e(msg);
                camera2Listener.onCameraError(new Exception(msg));
            }
            return defaultSize;
        }

        Size bestSize = sizes.get(0);
        float previewViewRatio;
        if (previewViewSize != null) {
            previewViewRatio = (float) previewViewSize.x / (float) previewViewSize.y;
        } else {
            previewViewRatio = (float) bestSize.getWidth() / (float) bestSize.getHeight();
        }
        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio;
        }
        Timber.d("getBestSupportedSize: previewViewRatio = 1 / previewViewRatio = %f", previewViewRatio);

        for (Size s : sizes) {
            if (specificPreviewSize != null && specificPreviewSize.x == s.getWidth() && specificPreviewSize.y == s.getHeight()) {
                Timber.d("getBestSupportedSize: returning %dx%d", s.getWidth(), s.getHeight());
                return s;
            }
            /*
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
        camera2Listener = null;
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
            if (camera2Listener != null) {
                camera2Listener.onCameraError(e);
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
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
        mImageReader.setOnImageAvailableListener(new OnImageAvailableListenerImpl(), mBackgroundHandler);

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
        configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            cameraManager.openCamera(mCameraId, mDeviceStateCallback, mBackgroundHandler);
        } catch (CameraAccessException | InterruptedException e) {
            if (camera2Listener != null) {
                camera2Listener.onCameraError(e);
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
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
            if (camera2Listener != null) {
                camera2Listener.onCameraClosed();
            }
        } catch (InterruptedException e) {
            if (camera2Listener != null) {
                camera2Listener.onCameraError(e);
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
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(
                    Arrays.asList(surface, mImageReader.getSurface()),
                    mCaptureStateCallback,
                    mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            Timber.e(e, "createCameraPreviewSession");
        }
    }

    /**
     * <pre>
     *     TODO: improve me.
     * </pre>
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

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());

        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / mPreviewSize.getHeight(), (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate((90 * (rotation - 2)) % 360, centerX, centerY);
            Timber.i("configureTransform when 90/270, scale = %f, rotate = %d", scale, (90 * (rotation - 2)) % 360);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
            Timber.i("configureTransform when 180, rotate = 180");
        }

        Timber.i("configureTransform: " + getCameraOrientation(rotation, mCameraId) + "  " + rotation * 90);
        mTextureView.setTransform(matrix);
    }

    public static final class Builder {

        /**
         * 预览显示的view，目前仅支持textureView
         */
        private TextureView previewDisplayView;

        /**
         * 是否镜像显示，只支持textureView
         */
        private boolean isMirror;

        /**
         * 指定的相机ID
         */
        private String specificCameraId;

        /**
         * 事件回调
         */
        private Camera2Listener camera2Listener;

        /**
         * 屏幕的长宽，在选择最佳相机比例时用到
         */
        private Point previewViewSize;

        /**
         * 传入getWindowManager().getDefaultDisplay().getRotation()的值即可
         */
        private int rotation;

        /**
         * 指定的预览宽高，若系统支持则会以这个预览宽高进行预览
         */
        private Point previewSize;

        /**
         * 最大分辨率
         */
        private Point maxPreviewSize;

        /**
         * 最小分辨率
         */
        private Point minPreviewSize;

        /**
         * 上下文，用于获取CameraManager
         */
        private Context context;

        public Builder() {
        }


        public Builder previewOn(TextureView val) {
            previewDisplayView = val;
            return this;
        }

        public Builder isMirror(boolean val) {
            isMirror = val;
            return this;
        }

        public Builder previewSize(Point val) {
            previewSize = val;
            return this;
        }

        public Builder maxPreviewSize(Point val) {
            maxPreviewSize = val;
            return this;
        }

        public Builder minPreviewSize(Point val) {
            minPreviewSize = val;
            return this;
        }

        public Builder previewViewSize(Point val) {
            previewViewSize = val;
            return this;
        }

        public Builder rotation(int val) {
            rotation = val;
            return this;
        }

        public Builder specificCameraId(String val) {
            specificCameraId = val;
            return this;
        }

        public Builder cameraListener(Camera2Listener val) {
            camera2Listener = val;
            return this;
        }

        public Builder context(Context val) {
            context = val;
            return this;
        }

        public Camera2Helper build() {
            if (previewViewSize == null) {
                Timber.e("previewViewSize is null, now use default previewSize");
            }
            if (camera2Listener == null) {
                Timber.e("camera2Listener is null, callback will not be called");
            }
            if (previewDisplayView == null) {
                throw new NullPointerException("you must preview on a textureView or a surfaceView");
            }
            if (maxPreviewSize != null && minPreviewSize != null) {
                if (maxPreviewSize.x < minPreviewSize.x || maxPreviewSize.y < minPreviewSize.y) {
                    throw new IllegalArgumentException("maxPreviewSize must greater than minPreviewSize");
                }
            }
            return new Camera2Helper(this);
        }

        @NonNull
        @Override
        public String toString() {
            return "Builder{" +
                    "previewDisplayView=" + previewDisplayView +
                    ", isMirror=" + isMirror +
                    ", specificCameraId='" + specificCameraId + '\'' +
                    ", camera2Listener=" + camera2Listener +
                    ", previewViewSize=" + previewViewSize +
                    ", rotation=" + rotation +
                    ", previewSize=" + previewSize +
                    ", maxPreviewSize=" + maxPreviewSize +
                    ", minPreviewSize=" + minPreviewSize +
                    ", context=" + context +
                    '}';
        }

    }

    private class OnImageAvailableListenerImpl implements ImageReader.OnImageAvailableListener {

        private byte[] y;
        private byte[] u;
        private byte[] v;

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            // Y:U:V == 4:2:2
            if (camera2Listener != null && image.getFormat() == ImageFormat.YUV_420_888) {
                Image.Plane[] planes = image.getPlanes();
                // 重复使用同一批byte数组，减少gc频率
                if (y == null) {
                    ByteBuffer bufferY = planes[0].getBuffer();
                    ByteBuffer bufferU = planes[1].getBuffer();
                    ByteBuffer bufferV = planes[2].getBuffer();
                    Timber.i("Y limit = %d, position = %d, capacity = %d", bufferY.limit(), bufferY.position(), bufferY.capacity());
                    Timber.i("u limit = %d, position = %d, capacity = %d", bufferU.limit(), bufferU.position(), bufferU.capacity());
                    Timber.i("v limit = %d, position = %d, capacity = %d", bufferV.limit(), bufferV.position(), bufferV.capacity());
                    Timber.i("planesY: row-stride = %d, pixel-stride = %d", planes[0].getRowStride(), planes[0].getPixelStride());
                    Timber.i("planesU: row-stride = %d, pixel-stride = %d", planes[1].getRowStride(), planes[1].getPixelStride());
                    Timber.i("planesV: row-stride = %d, pixel-stride = %d", planes[2].getRowStride(), planes[2].getPixelStride());
                    Timber.i("preview-size = %dx%d", mPreviewSize.getWidth(), mPreviewSize.getHeight());
                    y = new byte[bufferY.limit() - bufferY.position()];
                    u = new byte[bufferU.limit() - bufferU.position()];
                    v = new byte[bufferV.limit() - bufferV.position()];
                }

                if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
                    planes[0].getBuffer().get(y);
                    planes[1].getBuffer().get(u);
                    planes[2].getBuffer().get(v);
                    camera2Listener.onPreview(y, u, v, mPreviewSize, planes[0].getRowStride());
                }

            }
            image.close();
        }
    }

}
package me.ztiany.androidav.camera.camera2;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import me.ztiany.androidav.R;
import me.ztiany.lib.avbase.camera.camera2.Camera2Presenter;
import me.ztiany.lib.avbase.camera.camera2.Camera2Listener;
import me.ztiany.lib.avbase.camera.camera2.CameraId;
import me.ztiany.lib.avbase.camera.camera2.DefaultSizeSelector;
import me.ztiany.lib.avbase.camera.camera2.MediaRecorderProvider;
import me.ztiany.lib.avbase.camera.camera2.VideoSpec;
import timber.log.Timber;

public class FloatingCameraService extends Service {

    private WindowManager mWindowManager;

    private TextureView mTextureView;

    private Camera2Presenter mCamera2Helper;

    private View mFloatingView;

    private MediaRecorderProvider mMediaRecorderProvider;

    private String mSessionId;

    private final FloatingCameraConnection.Capturer mCapturer = FloatingCameraConnection.newCapturer(this);

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Timber.d("onSurfaceTextureAvailable() called with: surface = [" + surface + "], width = [" + width + "], height = [" + height + "]");
            startCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            Timber.d("onSurfaceTextureSizeChanged() called with: surface = [" + surface + "], width = [" + width + "], height = [" + height + "]");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            Timber.d("onSurfaceTextureDestroyed() called with: surface = [" + surface + "]");
            stopCamera();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        }
    };

    private final Camera2Listener mCamera2Listener = new Camera2Listener() {

        @Override
        public void onCameraOpened(CameraDevice cameraDevice, String cameraId, final Size previewSize, final int displayOrientation, boolean isMirror) {
            Timber.d("onCameraOpened is called(): previewSize = " + previewSize.getWidth() + "x" + previewSize.getHeight());
        }

        @Override
        public void onCameraClosed() {
            Timber.d("onCameraClosed is called!");
        }

        @Override
        public void onCameraError(Exception exception) {
            Timber.e(exception, "onCameraError is called!");
        }

    };

    public static boolean isRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = am.getRunningServices(Integer.MAX_VALUE);
        int myUid = android.os.Process.myUid();
        String targetClassName = FloatingCameraService.class.getName();

        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.uid == myUid && targetClassName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("FloatingCameraService is created.");
        if (Settings.canDrawOverlays(this)) {
            initFloatingPreviewWindow();
        }

        mCapturer.init();
        mCapturer.setCapturingActionListener(new FloatingCameraConnection.CapturingActionListener() {
            @Override
            public void startCapturing(String sessionId, VideoSpec videoSpec) {
                doStartCapturing(sessionId, videoSpec);
            }

            @Override
            public void stopCapturing(String sessionId) {
                doStopCapturing(sessionId);
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("InflateParams")
    private void initFloatingPreviewWindow() {
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        mFloatingView = LayoutInflater.from(this).inflate(R.layout.camera_independent_window, null);
        mTextureView = mFloatingView.findViewById(R.id.texture_preview);
        mTextureView.setSurfaceTextureListener(surfaceTextureListener);

        WindowManager.LayoutParams params = buildFloatingWindowLayoutParams();
        mWindowManager.addView(mFloatingView, params);
    }

    @NonNull
    private static WindowManager.LayoutParams buildFloatingWindowLayoutParams() {
        int windowType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            windowType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                27,
                48,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        );

        layoutParams.gravity = Gravity.TOP | Gravity.END;

        return layoutParams;
    }

    private void startCamera() {
        if (mCamera2Helper == null) {
            mMediaRecorderProvider = new MediaRecorderProvider();

            mCamera2Helper = new Camera2Presenter.Builder()
                    .context(getApplicationContext())
                    .cameraListener(mCamera2Listener)
                    .cameraId(CameraId.BACK)
                    .previewOn(mTextureView)
                    .outputProvider(mMediaRecorderProvider)
                    .sizeSelector(
                            DefaultSizeSelector.newBuilder()
                                    .maxPreviewSize(new Size(1920, 1080))
                                    .minPreviewSize(new Size(0, 0))
                                    .previewViewSize(new Size(mTextureView.getWidth(), mTextureView.getHeight()))
                                    .build()
                    )
                    .rotation(((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation())
                    .build();
        }
        mCamera2Helper.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // connection
        mCapturer.destroy();
        // recording
        if (!TextUtils.isEmpty(mSessionId)) {
            doStopCapturing(mSessionId);
        }
        if (mMediaRecorderProvider != null) {
            mMediaRecorderProvider.release();
        }
        // camera
        destroyCamera();
        // floating window
        destroyFloatingWindow();
    }

    private void stopCamera() {
        if (mCamera2Helper != null) {
            mCamera2Helper.stop();
        }
    }

    private void destroyCamera() {
        if (mCamera2Helper != null) {
            mCamera2Helper.release();
        }
    }

    private void destroyFloatingWindow() {
        if (mWindowManager != null) {
            mWindowManager.removeView(mFloatingView);
            mFloatingView = null;
            mTextureView = null;
        }
    }

    private void doStartCapturing(String sessionId, VideoSpec videoSpec) {
        mSessionId = sessionId;
        Timber.d("doStartCapturing is called");

        mMediaRecorderProvider.start(videoSpec, succeeded -> {
            Timber.d("doStartCapturing result: %b", succeeded);

            if (succeeded) {
                mCapturer.notifyCapturerEvent(sessionId, CapturerEvent.STARTED);
            } else {
                mCapturer.notifyCapturerEvent(sessionId, CapturerEvent.ERROR);
            }
        });
    }

    private void doStopCapturing(String sessionId) {
        Timber.d("doStopCapturing is called");
        if (mMediaRecorderProvider.stop()) {
            mCapturer.notifyCapturerEvent(sessionId, CapturerEvent.STOPPED);
            mSessionId = null;
        }
    }

}
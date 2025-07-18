package me.ztiany.androidav.camera.camera2;

import android.content.pm.ActivityInfo;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import me.ztiany.androidav.R;
import me.ztiany.lib.avbase.camera.camera2.Camera2Presenter;
import me.ztiany.lib.avbase.camera.camera2.Camera2Listener;
import me.ztiany.lib.avbase.camera.camera2.CameraId;
import me.ztiany.lib.avbase.camera.camera2.DefaultSizeSelector;
import me.ztiany.lib.avbase.camera.camera2.FrameReader;
import me.ztiany.lib.avbase.utils.av.YUVUtils;
import timber.log.Timber;

public class Camera2EncodingActivity extends AppCompatActivity {

    private Camera2Presenter camera2Presenter;

    private TextureView textureView;

    // 图像帧数据，全局变量避免反复创建，降低gc频率
    private byte[] nv21;

    // 显示的旋转角度
    private int displayOrientation;

    // 是否手动镜像预览
    private boolean isMirrorPreview;

    // 实际打开的cameraId
    private String openedCameraId;

    private final H264Encoder mH264Encoder = new H264Encoder();

    private final FrameReader frameReader = new FrameReader();

    private final Camera2Listener mCamera2Listener = new Camera2Listener() {

        @Override
        public void onCameraOpened(CameraDevice cameraDevice, String cameraId, final Size previewSize, final int displayOrientation, boolean isMirror) {
            Timber.i("onCameraOpened:  previewSize = " + previewSize.getWidth() + "x" + previewSize.getHeight());
            Camera2EncodingActivity.this.displayOrientation = displayOrientation;
            Camera2EncodingActivity.this.isMirrorPreview = isMirror;
            Camera2EncodingActivity.this.openedCameraId = cameraId;
            mH264Encoder.stop();
            mH264Encoder.initCodec(previewSize.getWidth(), previewSize.getHeight(), displayOrientation);
        }

        @Override
        public void onCameraClosed() {
            Timber.i("onCameraClosed: ");
        }

        @Override
        public void onCameraError(Exception e) {
            Timber.e(e, "onCameraError");
        }

    };

    private void showPreview(
            final byte[] y,
            final byte[] u,
            final byte[] v,
            final Size previewSize,
            final int stride
    ) {
        if (nv21 == null) {
            nv21 = new byte[previewSize.getWidth() * previewSize.getHeight() * 3 / 2];
        }
        YUVUtils.nv21FromYUVCutToWidth(y, u, v, nv21, stride, previewSize.getWidth(), previewSize.getHeight());
        mH264Encoder.processCamaraData(nv21, previewSize, stride, displayOrientation, isMirrorPreview, openedCameraId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity_api2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        initView();
    }

    private void initView() {
        textureView = findViewById(R.id.texture_preview);
        textureView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                initCamera();
            }
        });
    }

    void initCamera() {
        frameReader.setFrameListener(this::showPreview);

        camera2Presenter = new Camera2Presenter.Builder()
                .context(getApplicationContext())
                .cameraListener(mCamera2Listener)
                .sizeSelector(
                        DefaultSizeSelector.newBuilder()
                                .maxPreviewSize(new Size(1920, 1080))
                                .minPreviewSize(new Size(0, 0))
                                .previewViewSize(new Size(textureView.getWidth(), textureView.getHeight()))
                                .build()
                )
                .cameraId(CameraId.BACK)
                .previewOn(textureView)
                .outputProvider(frameReader)
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();
        camera2Presenter.start();
    }

    @Override
    protected void onPause() {
        if (camera2Presenter != null) {
            camera2Presenter.stop();
        }
        if (mH264Encoder != null) {
            mH264Encoder.stop();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera2Presenter != null) {
            camera2Presenter.start();
        }
    }

    @Override
    protected void onDestroy() {
        if (camera2Presenter != null) {
            camera2Presenter.release();
        }
        super.onDestroy();
    }

    public void switchCamera(View view) {
        if (camera2Presenter != null) {
            camera2Presenter.switchCamera();
        }
    }

}

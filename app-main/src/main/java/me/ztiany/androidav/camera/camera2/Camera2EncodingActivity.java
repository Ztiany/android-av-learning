package me.ztiany.androidav.camera.camera2;

import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import me.ztiany.androidav.R;
import me.ztiany.lib.avbase.utils.av.YUVUtils;
import timber.log.Timber;


public class Camera2EncodingActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener {

    private Camera2Helper camera2Helper;
    private TextureView textureView;

    // 默认打开的 CAMERA
    private static final String CAMERA_ID = Camera2Helper.CAMERA_ID_BACK;

    // 图像帧数据，全局变量避免反复创建，降低gc频率
    private byte[] nv21;

    // 显示的旋转角度
    private int displayOrientation;

    // 是否手动镜像预览
    private boolean isMirrorPreview;

    // 实际打开的cameraId
    private String openedCameraId;

    private final H264Encoder mH264Encoder = new H264Encoder();

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
        public void onPreview(final byte[] y, final byte[] u, final byte[] v, final Size previewSize, final int stride) {
            if (nv21 == null) {
                nv21 = new byte[previewSize.getWidth() * previewSize.getHeight() * 3 / 2];
            }
            YUVUtils.nv21FromYUVCutToWidth(y, u, v, nv21, stride, previewSize.getWidth(), previewSize.getHeight());
            mH264Encoder.processCamaraData(nv21, previewSize, stride, displayOrientation, isMirrorPreview, openedCameraId);
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
        textureView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    void initCamera() {
        camera2Helper = new Camera2Helper.Builder()
                .cameraListener(mCamera2Listener)
                .maxPreviewSize(new Point(1920, 1080))
                .minPreviewSize(new Point(1280, 720))
                .specificCameraId(CAMERA_ID)
                .context(getApplicationContext())
                .previewOn(textureView)
                .previewViewSize(new Point(textureView.getWidth(), textureView.getHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();
        camera2Helper.start();
    }

    @Override
    public void onGlobalLayout() {
        textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        initCamera();
    }

    @Override
    protected void onPause() {
        if (camera2Helper != null) {
            camera2Helper.stop();
        }
        if (mH264Encoder != null) {
            mH264Encoder.stop();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera2Helper != null) {
            camera2Helper.start();
        }
    }

    @Override
    protected void onDestroy() {
        if (camera2Helper != null) {
            camera2Helper.release();
        }
        super.onDestroy();
    }

    public void switchCamera(View view) {
        if (camera2Helper != null) {
            camera2Helper.switchCamera();
        }
    }

}

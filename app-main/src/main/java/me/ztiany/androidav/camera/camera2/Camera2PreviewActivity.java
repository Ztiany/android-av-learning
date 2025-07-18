package me.ztiany.androidav.camera.camera2;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.ztiany.androidav.R;
import me.ztiany.lib.avbase.camera.camera2.Camera2Presenter;
import me.ztiany.lib.avbase.camera.camera2.Camera2Listener;
import me.ztiany.lib.avbase.camera.camera2.CameraId;
import me.ztiany.lib.avbase.camera.camera2.DefaultSizeSelector;
import me.ztiany.lib.avbase.camera.camera2.FrameReader;
import me.ztiany.lib.avbase.utils.av.YUVUtils;
import me.ztiany.lib.avbase.view.BorderImageView;
import timber.log.Timber;

public class Camera2PreviewActivity extends AppCompatActivity {

    private Camera2Presenter camera2Presenter;

    private TextureView textureView;

    // 用于显示原始预览数据
    private ImageView ivOriginFrame;

    // 用于显示和预览画面相同的图像数据
    private ImageView ivPreviewFrame;

    // 图像帧数据，全局变量避免反复创建，降低 gc 频率
    private byte[] nv21;

    // 显示的旋转角度
    private int displayOrientation;

    // 是否手动镜像预览
    private boolean isMirrorPreview;

    // 实际打开的 cameraId
    private String openedCameraId;

    // 当前获取的帧数
    private int currentIndex = 0;

    // 处理的间隔帧
    private static final int PROCESS_INTERVAL = 60;

    // 线程池
    private ExecutorService imageProcessExecutor;

    private final FrameReader frameReader = new FrameReader();

    private final YUVSaver yuvSaver = new YUVSaver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity_api2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        imageProcessExecutor = Executors.newSingleThreadExecutor();
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
                .cameraListener(mCamera2Listener)
                .sizeSelector(
                        DefaultSizeSelector.newBuilder()
                                .maxPreviewSize(new Size(1920, 1080))
                                .minPreviewSize(new Size(0, 0))
                                .previewViewSize(new Size(textureView.getWidth(), textureView.getHeight()))
                                .build()
                )
                .cameraId(CameraId.BACK)
                .context(getApplicationContext())
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
        if (imageProcessExecutor != null) {
            imageProcessExecutor.shutdown();
            imageProcessExecutor = null;
        }
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

    private final Camera2Listener mCamera2Listener = new Camera2Listener() {

        @SuppressLint("SetTextI18n")
        @Override
        public void onCameraOpened(
                CameraDevice cameraDevice,
                String cameraId,
                final Size previewSize,
                final int orientation,
                boolean isMirror
        ) {
            Timber.i("onCameraOpened:  previewSize = " + previewSize.getWidth() + "x" + previewSize.getHeight());

            Camera2PreviewActivity.this.displayOrientation = orientation;
            Camera2PreviewActivity.this.isMirrorPreview = isMirror;
            Camera2PreviewActivity.this.openedCameraId = cameraId;

            //在相机打开时，添加右上角的 view 用于显示原始数据和预览数据
            runOnUiThread(() -> {
                ivPreviewFrame = new BorderImageView(Camera2PreviewActivity.this);
                ivOriginFrame = new BorderImageView(Camera2PreviewActivity.this);
                TextView tvPreview = new TextView(Camera2PreviewActivity.this);
                TextView tvOrigin = new TextView(Camera2PreviewActivity.this);
                tvPreview.setTextColor(Color.WHITE);
                tvOrigin.setTextColor(Color.WHITE);
                tvPreview.setText("preview");
                tvOrigin.setText("origin");
                boolean needRotate = orientation % 180 != 0;
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                int longSide = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
                int shortSide = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);

                FrameLayout.LayoutParams previewLayoutParams = new FrameLayout.LayoutParams(
                        !needRotate ? longSide / 4 : shortSide / 4,
                        needRotate ? longSide / 4 : shortSide / 4
                );

                FrameLayout.LayoutParams originLayoutParams = new FrameLayout.LayoutParams(
                        longSide / 4, shortSide / 4
                );

                previewLayoutParams.gravity = Gravity.END | Gravity.TOP;
                originLayoutParams.gravity = Gravity.END | Gravity.TOP;
                previewLayoutParams.topMargin = originLayoutParams.height;
                ivPreviewFrame.setLayoutParams(previewLayoutParams);
                tvPreview.setLayoutParams(previewLayoutParams);
                ivOriginFrame.setLayoutParams(originLayoutParams);
                tvOrigin.setLayoutParams(originLayoutParams);

                ((FrameLayout) textureView.getParent()).addView(ivPreviewFrame);
                ((FrameLayout) textureView.getParent()).addView(ivOriginFrame);
                ((FrameLayout) textureView.getParent()).addView(tvPreview);
                ((FrameLayout) textureView.getParent()).addView(tvOrigin);
            });
        }

        @Override
        public void onCameraClosed() {
            Timber.i("onCameraClosed: ");
        }

        @Override
        public void onCameraError(Exception exception) {
            Timber.e(exception, "onCameraError");
        }
    };

    private void showPreview(final byte[] y, final byte[] u, final byte[] v, final Size previewSize, final int stride) {
        if (currentIndex++ % PROCESS_INTERVAL == 0) {
            imageProcessExecutor.execute(() -> {
                if (nv21 == null) {
                    nv21 = new byte[stride * previewSize.getHeight() * 3 / 2];
                }

                yuvSaver.saveYUV(y, u, v, previewSize, stride, displayOrientation, isMirrorPreview);

                YUVUtils.nv21FromYUV(y, u, v, nv21, stride, previewSize.getHeight());
                YUVImageDisplay.showYUVImage(
                        Camera2PreviewActivity.this,
                        nv21,
                        stride,
                        previewSize,
                        openedCameraId,
                        displayOrientation,
                        isMirrorPreview,
                        ivOriginFrame,
                        ivPreviewFrame
                );
            });
        }
    }

}
package me.ztiany.androidav.camera.camera2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import me.ztiany.androidav.MainActivity;
import me.ztiany.androidav.R;
import me.ztiany.lib.avbase.camera.camera2.VideoSpec;
import timber.log.Timber;

public class Camera2IndependentPreviewActivity extends AppCompatActivity {

    private String mSessionId;

    private final FloatingCameraConnection.Commander mCommander = FloatingCameraConnection.newCommander(this);

    private final ActivityResultLauncher<String[]> normalPermissionRequester = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.CAMERA))) {
                    checkOverlayPermission();
                } else {
                    Toast.makeText(this, "相机权限是必需的", Toast.LENGTH_SHORT).show();
                    finishAfterTransition();
                }
            }
    );

    private final ActivityResultLauncher<Intent> requestOverlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
                if (Settings.canDrawOverlays(Camera2IndependentPreviewActivity.this)) {
                    startFloatingService();
                } else {
                    Toast.makeText(Camera2IndependentPreviewActivity.this, "悬浮窗权限未授予", Toast.LENGTH_SHORT).show();
                    finishAfterTransition();
                }
            });

    private boolean mCapturing = false;

    private TextView mCapturingBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initCommander();

        setContentView(R.layout.camera_activity_independent_preview);

        findViewById(R.id.btn_start_service).setOnClickListener(view -> checkPermissionsAndStart());
        findViewById(R.id.btn_stop_service).setOnClickListener(view -> stopFloatingService());
        mCapturingBtn = findViewById(R.id.btn_start_capture);
        mCapturingBtn.setOnClickListener(view -> startOrStopCapturing());
    }

    @SuppressLint("SetTextI18n")
    private void initCommander() {
        mCommander.init();
        mCommander.setCapturingStateListener(new FloatingCameraConnection.CapturingStateListener() {
            @Override
            public void onCapturingStarted(String sessionId) {
                Timber.d("onCapturingStarted is called with sessionId: %s", sessionId);
                if (mCapturingBtn != null) {
                    mCapturingBtn.setText("Stop Capturing");
                }
                mCapturing = true;
            }

            @Override
            public void onCapturingFinished(String sessionId, boolean succeeded) {
                Timber.d("onCapturingFinished is called with sessionId: %s", sessionId);
                if (mCapturingBtn != null) {
                    mCapturingBtn.setText("Start Capturing");
                }
                mCapturing = false;
            }
        });
    }

    private void checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            normalPermissionRequester.launch(new String[]{Manifest.permission.CAMERA});
        } else {
            checkOverlayPermission();
        }
    }

    private void checkOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            startFloatingService();
        } else {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
            );
            Toast.makeText(this, "请授予悬浮窗权限以继续", Toast.LENGTH_LONG).show();
            requestOverlayPermissionLauncher.launch(intent);
        }
    }

    private void startFloatingService() {
        Toast.makeText(this, "启动悬浮相机服务", Toast.LENGTH_LONG).show();
        startService(new Intent(this, FloatingCameraService.class));
    }

    private void stopFloatingService() {
        stopService(new Intent(this, FloatingCameraService.class));
    }

    private void startOrStopCapturing() {
        if (!FloatingCameraService.isRunning(this)) {
            Toast.makeText(this, "悬浮相机服务还没有启动", Toast.LENGTH_LONG).show();
            return;
        }
        if (mCapturing) {
            mCommander.stopCapturing(mSessionId);
            mSessionId = "";
        } else {
            mSessionId = String.valueOf(System.currentTimeMillis());
            mCommander.startCapturing(mSessionId, new VideoSpec.Builder()
                    .setFrameRate(30)
                    .setVideoSize(new Size(1920, 1080))
                    .storePath(generateStorePath())
                    .build()
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCommander.destroy();
    }

    private String generateStorePath() {
        return getExternalFilesDir(null) + "/" + System.currentTimeMillis() + "-video.mp4";
    }

}
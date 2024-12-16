package me.ztiany.androidav.stream.livescreen.sender;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import me.ztiany.androidav.stream.livescreen.Constants;

public class ScreenSenderActivity extends AppCompatActivity {

    private MediaProjectionManager mediaProjectionManager;
    private SocketLiveServer mSocketLiveServer;
    private static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || requestCode != REQUEST_CODE) {
            return;
        }

        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            return;
        }

        mSocketLiveServer = new SocketLiveServer(Constants.SERVER_PORT);
        mSocketLiveServer.start(mediaProjection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocketLiveServer.close();
    }

}
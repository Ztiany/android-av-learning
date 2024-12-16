package me.ztiany.androidav.stream.livecamera.server;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.ztiany.androidav.R;
import me.ztiany.androidav.stream.SocketLive;
import me.ztiany.androidav.stream.livecamera.Constants;
import me.ztiany.androidav.stream.livecamera.DecodePlayerLiveH265;
import me.ztiany.androidav.stream.livecamera.EncodePushLiveH265;
import me.ztiany.androidav.stream.livecamera.LocalSurfaceView;

public class LiveCameraServerActivity extends AppCompatActivity {

    private LocalSurfaceView mLocalSurfaceView;
    private DecodePlayerLiveH265 mDecodePlayerLiveH265;
    private EncodePushLiveH265 mEncodePushLiveH265;
    private SocketLive mSocketLive;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_h265_livecamera);
        initView();
    }

    private void initView() {
        SurfaceView removeSurfaceView = findViewById(R.id.video_removeSurfaceView);
        mLocalSurfaceView = findViewById(R.id.video_localSurfaceView);

        removeSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                mDecodePlayerLiveH265 = new DecodePlayerLiveH265();
                mDecodePlayerLiveH265.initDecoder(holder.getSurface());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });
    }

    public void connect(View view) {
        if (mDecodePlayerLiveH265 == null) {
            return;
        }

        if (mEncodePushLiveH265 == null) {
            mSocketLive = new SocketLiveServer(data -> mDecodePlayerLiveH265.push(data), Constants.SERVER_PORT);
            mSocketLive.start();

            mEncodePushLiveH265 = new EncodePushLiveH265(mSocketLive, mLocalSurfaceView.getSize().width, mLocalSurfaceView.getSize().height);
            mLocalSurfaceView.setEncodePushLiveH265(mEncodePushLiveH265);
            mEncodePushLiveH265.startLive();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocketLive != null) {
            mSocketLive.close();
        }

        mLocalSurfaceView.stop();

        if (mEncodePushLiveH265 != null) {
            mEncodePushLiveH265.stop();
        }
        if (mDecodePlayerLiveH265 != null) {
            mDecodePlayerLiveH265.stop();
        }
    }

}
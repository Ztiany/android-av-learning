package me.ztiany.androidav.stream.livescreen.receiver;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.ztiany.androidav.R;
import me.ztiany.androidav.stream.livescreen.Constants;
import me.ztiany.lib.avbase.utils.ui.Views;
import timber.log.Timber;

public class ScreenReceiverActivity extends AppCompatActivity implements SocketLiveClient.SocketCallback {

    private Surface surface;
    private MediaCodec mediaCodec;
    private SocketLiveClient mScreenLive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView surfaceView = new SurfaceView(this);
        setContentView(surfaceView, Views.newMMLayoutParams());

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                surface = holder.getSurface();
                initSocket();
                initDecoder(surface);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });

    }

    public void initDecoder(Surface surface) {
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            final MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, Constants.WIDTH, Constants.HEIGHT);
            format.setInteger(MediaFormat.KEY_BIT_RATE, Constants.KEY_BIT_RATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, Constants.KEY_FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constants.KEY_I_FRAME_INTERVAL);
            mediaCodec.configure(format, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            Timber.e(e, "initDecoder");
        }
    }

    private void initSocket() {
        mScreenLive = new SocketLiveClient(this, Constants.SERVER_ADDRESS);
        mScreenLive.start();
    }

    @Override
    public void callBack(byte[] data) {
        int index = mediaCodec.dequeueInputBuffer(100000);
        if (index >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(data, 0, data.length);
            mediaCodec.queueInputBuffer(index, 0, data.length, System.currentTimeMillis(), 0);
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);

        while (outputBufferIndex > 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScreenLive.stop();
    }

}
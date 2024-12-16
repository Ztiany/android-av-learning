package me.ztiany.androidav.stream.livecamera;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DecodePlayerLiveH265 {

    private MediaCodec mediaCodec;

    public void initDecoder(Surface surface) {
        try {
            mediaCodec = MediaCodec.createDecoderByType("video/hevc");

            final MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, Constants.WIDTH, Constants.HEIGHT);
            format.setInteger(MediaFormat.KEY_BIT_RATE, Constants.KEY_BIT_RATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, Constants.KEY_FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constants.KEY_I_FRAME_INTERVAL);

            mediaCodec.configure(format, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void push(byte[] data) {
        int index = mediaCodec.dequeueInputBuffer(100000);
        if (index >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(data, 0, data.length);
            mediaCodec.queueInputBuffer(index, 0, data.length, System.currentTimeMillis(), 0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        while (outputBufferIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

    public void stop() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
    }

}

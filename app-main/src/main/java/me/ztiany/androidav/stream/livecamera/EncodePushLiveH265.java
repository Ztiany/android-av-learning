package me.ztiany.androidav.stream.livecamera;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.ztiany.lib.avbase.utils.av.YUVUtils;
import me.ztiany.androidav.stream.SocketLive;

public class EncodePushLiveH265 {

    private MediaCodec mediaCodec;

    private final SocketLive socketLive;

    //nv21转换成nv12的数据
    private byte[] nv12;

    //旋转之后的yuv数据
    private byte[] yuv;

    public static final int NAL_I = 19;
    public static final int NAL_VPS = 32;
    private byte[] vps_sps_pps_buf;
    private int frameIndex;
    private final int width, height;

    public EncodePushLiveH265(SocketLive socketLive, int width, int height) {
        this.socketLive = socketLive;
        this.width = width;
        this.height = height;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void startLive() {
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            //这里反着传是因为 YUV 数据已经被旋转了
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, height, width);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, Constants.KEY_BIT_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, Constants.KEY_FRAME_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constants.KEY_I_FRAME_INTERVAL);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();

            //初始化转换容器
            int bufferLength = width * height * 3 / 2;
            nv12 = new byte[bufferLength];
            yuv = new byte[bufferLength];
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void encodeFrame(byte[] input) {
        //两种方式都可以：
        //先转换，再旋转
        //YUVUtils.nv21toNV12(input, nv12);
        //YUVUtils.rotateNV12CW90D(nv12, yuv, width, height);

        //先旋转，再转换
        YUVUtils.nv21RotateCW(input, nv12, width, height, 90);
        YUVUtils.nv12FromNV21(nv12, yuv);

        int inputBufferIndex = mediaCodec.dequeueInputBuffer(100000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(yuv);
            long presentationTimeUs = computePresentationTime(frameIndex);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv.length, presentationTimeUs, 0);
            frameIndex++;
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
            dealFrame(outputBuffer, bufferInfo);
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

    private long computePresentationTime(long frameIndex) {
        return 240 + frameIndex * 1000000 / 15;
    }

    private void dealFrame(ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
        int offset = 4;
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
        int type = (bb.get(offset) & 0x7E) >> 1;
        if (type == NAL_VPS) {
            vps_sps_pps_buf = new byte[bufferInfo.size];
            bb.get(vps_sps_pps_buf);
        } else if (type == NAL_I) {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            byte[] newBuf = new byte[vps_sps_pps_buf.length + bytes.length];
            System.arraycopy(vps_sps_pps_buf, 0, newBuf, 0, vps_sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, vps_sps_pps_buf.length, bytes.length);
            this.socketLive.sendData(newBuf);
        } else {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            this.socketLive.sendData(bytes);
        }
    }

    public void stop() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
    }

}
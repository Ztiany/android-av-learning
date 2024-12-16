package me.ztiany.androidav.camera.camera2;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Size;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

import me.ztiany.lib.avbase.utils.Directory;
import me.ztiany.lib.avbase.utils.FileUtils;
import me.ztiany.lib.avbase.utils.IOUtils;
import me.ztiany.lib.avbase.utils.av.YUVUtils;
import timber.log.Timber;

class H264Encoder {

    private final LinkedBlockingDeque<byte[]> mLinkedBlockingDeque = new LinkedBlockingDeque<>();

    private volatile MediaCodec mediaCodec;
    private volatile boolean stopped = true;

    private byte[] nv21_rotated;
    private byte[] nv12;

    private int mFrameIndex;

    private FileOutputStream mFileOutputStream;
    private FileWriter mFileWriter;

    void initCodec(int width, int height, int displayOrientation) {
        Timber.d(+width + "], height = [" + height + "], displayOrientation = [" + displayOrientation + "]");

        if (!stopped) {
            throw new IllegalStateException("already initialized");
        }

        try {
            mFileOutputStream = new FileOutputStream(Directory.createSDCardRootAppTimeNamingPath(Directory.VIDEO_FORMAT_H264));
            mFileWriter = new FileWriter(Directory.createSDCardRootAppTimeNamingPath(Directory.VIDEO_FORMAT_TXT));
        } catch (IOException e) {
            Timber.e(e);
            return;
        }

        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat format;
            if ((displayOrientation / 90) % 2 == 1) {
                format = MediaFormat.createVideoFormat("video/avc", height, width);
            } else {
                format = MediaFormat.createVideoFormat("video/avc", width, height);
            }

            /*
             * 使用 MediaCodec 将 YUV 编码为 H264 等格式时，如果指定格式为 `COLOR_FormatYUV420SemiPlanar`，在部分机型上，
             * MediaCodec 自己会对输入的 YUV 图像的 u/v 进行交换，这就可能会导致格式不匹配的问题（结果就是编码出来的数据颜色不对）
             * 解决方案有以下几种：
             *      1. 在把 NV21 图像传给 MediaCodec 之前，先把 NV21 转成 NV12（毕竟这俩货仅仅只是 u/v 相反而已），但只是少数设备会有这种情况，适配起来估计有够呛的。不推荐
             *      2. 使用 Surface 模式，可以完美避免这种情况，但同时会丧失对原 YUV 图像的处理能力，不过可以使用 OpenGL 方式来处理图像。推荐
             *      3. 使用 COLOR_FormatYUV420Flexible 配合 MediaCodec.getInputImage()。推荐，但是如果填充 Image 官方并没有给示例，可以参考的代码是
             *          https://github.com/aosp/cts/blob/master/tests/tests/media/src/android/media/cts/CodecUtils.java
             */
            //NV12
            //format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            //I420
            //format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);

            //Should works with mediaCodec.getInputImage().
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);

            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 3);//可以为 1 3 5 等来控制质量
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (IOException e) {
            Timber.e(e);
            return;
        }

        stopped = false;
        startEncoder();
    }

    void stop() {
        mLinkedBlockingDeque.clear();
        stopped = true;
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        IOUtils.closeAllSafely(mFileOutputStream, mFileWriter);
        mFileOutputStream = null;
        mFileWriter = null;
        mFrameIndex = 0;
        nv21_rotated = null;
        nv12 = null;
    }

    //todo：适配横屏、前置摄像头
    void processCamaraData(byte[] nv21, Size previewSize, int stride, int displayOrientation, boolean isMirrorPreview, String openedCameraId) {
        if (nv21_rotated == null) {
            nv21_rotated = new byte[previewSize.getWidth() * previewSize.getHeight() * 3 / 2];
            nv12 = new byte[previewSize.getWidth() * previewSize.getHeight() * 3 / 2];
        }

        YUVUtils.nv21RotateCW(nv21, nv21_rotated, previewSize.getWidth(), previewSize.getHeight(), 90);
        YUVUtils.nv12FromNV21(nv21_rotated, nv12);

        while (mLinkedBlockingDeque.size() > 3) {
            mLinkedBlockingDeque.removeFirst();
            Timber.w("drop a frame");
        }

        byte[] bytes = new byte[nv12.length];
        System.arraycopy(nv12, 0, bytes, 0, nv12.length);
        mLinkedBlockingDeque.add(bytes);
    }

    private void startEncoder() {
        new Thread(() -> {
            while (!stopped) {
                try {
                    byte[] rawData = mLinkedBlockingDeque.takeFirst();
                    if (mediaCodec != null) {
                        encode(rawData);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(this::receiveDecoded).start();
    }

    private void receiveDecoded() {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int outIndex;
        ByteBuffer byteBuffer;

        while (!stopped && mediaCodec != null) {
            try {
                outIndex = mediaCodec.dequeueOutputBuffer(info, 100000);
            } catch (IllegalStateException e) {
                Timber.e(e);
                return;
            }

            if (outIndex < 0) {
                Timber.w("outIndex < 0");
                continue;
            }

            try {
                byteBuffer = mediaCodec.getOutputBuffer(outIndex);
            } catch (IllegalStateException e) {
                Timber.e(e);
                return;
            }
            byte[] encoded = new byte[byteBuffer.remaining()];
            byteBuffer.get(encoded);
            FileUtils.writeBytes(mFileOutputStream, encoded, true);
            FileUtils.writeContent(mFileWriter, encoded);
            mediaCodec.releaseOutputBuffer(outIndex, false);
        }
    }

    void encode(byte[] rawData) {
        int inIndex = mediaCodec.dequeueInputBuffer(100000);
        if (inIndex < 0) {
            Timber.w("inIndex < 0");
        }
        if (inIndex >= 0) {
            ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inIndex);
            byteBuffer.clear();
            byteBuffer.put(rawData, 0, rawData.length);
            mediaCodec.queueInputBuffer(inIndex, 0, rawData.length, computePresentationTime(mFrameIndex++), 0);
        }
    }

    private long computePresentationTime(long frameIndex) {
        /*240 是为了给播放器一定的时间来初始化，否则可能第一帧无法展示。*/
        return 240 + frameIndex * 1000000 / 15;
    }

}
package me.ztiany.androidav.stream.h264;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class H264Player implements Runnable {

    private static final String TAG = "H264Player";


    private String path;

    private MediaCodec mediaCodec;

    public H264Player(String path) {
        this.path = path;
        try {
            mediaCodec = MediaCodec.createDecoderByType("video/avc");
            MediaFormat mediaformat = MediaFormat.createVideoFormat("video/avc", 368, 384);
            mediaformat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaCodec.configure(mediaformat, null, null, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        mediaCodec.start();
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            decodeH264();
        } catch (Exception e) {
            Log.e(TAG, "run: " + e);
        }
    }

    private void decodeH264() {
        byte[] bytes = null;
        try {
            //一次性读取【生产中不会这样】
            bytes = getBytes(path);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();

        int startIndex = 0;
        int totalSize = bytes.length;

        while (true) {
            if (totalSize == 0 || startIndex >= totalSize) {
                break;
            }

            int nextFrameStart = findByFrame(bytes, startIndex + 2, totalSize);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int inIndex = mediaCodec.dequeueInputBuffer(10000);

            if (inIndex >= 0) {
                ByteBuffer byteBuffer = inputBuffers[inIndex];
                byteBuffer.clear();
                byteBuffer.put(bytes, startIndex, nextFrameStart - startIndex);
                mediaCodec.queueInputBuffer(inIndex, 0, nextFrameStart - startIndex, 0, 0);
                startIndex = nextFrameStart;
            } else {
                continue;
            }

            int outIndex = mediaCodec.dequeueOutputBuffer(info, 10000);

            if (outIndex >= 0) {

                ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);

                byteBuffer.position(info.offset);
                byteBuffer.limit(info.offset + info.size);

                byte[] ba = new byte[byteBuffer.remaining()];
                byteBuffer.get(ba);

                YuvImage yuvImage = new YuvImage(ba, ImageFormat.NV21, 368, 384, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, 368, 384), 100, baos);
                byte[] jdata = baos.toByteArray();//rgb
                Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);

                if (bmp != null) {
                    if (i > 5) {
                        try {
                            File myCaptureFile = new File(Environment.getExternalStorageDirectory(), "img.png");
                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
                            bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                            bos.flush();
                            bos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    i++;
                }

                try {
                    Thread.sleep(33);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mediaCodec.releaseOutputBuffer(outIndex, false);
            } else {

            }

        }

    }

    //匹配分隔符：00000001 后面就是帧分割
    private int i = 0;

    private int findByFrame(byte[] bytes, int start, int totalSize) {
        for (int i = start; i < totalSize - 4; i++) {
            if (bytes[i] == 0x00 && bytes[i + 1] == 0x00 && bytes[i + 2] == 0x00 && bytes[i + 3] == 0x01) {
                return i;
            }

        }
        return -1;
    }

    public byte[] getBytes(String path) throws IOException {
        InputStream is = new DataInputStream(new FileInputStream(path));
        int len;
        int size = 1024;
        byte[] buf;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        buf = new byte[size];
        while ((len = is.read(buf, 0, size)) != -1) {
            bos.write(buf, 0, len);
        }
        buf = bos.toByteArray();
        return buf;
    }

}
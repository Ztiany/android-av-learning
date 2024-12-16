package me.ztiany.androidav.stream.livescreen.sender;

import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.view.Surface;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

import me.ztiany.androidav.stream.livescreen.Constants;
import timber.log.Timber;

import static me.ztiany.androidav.stream.livescreen.Constants.HEIGHT;
import static me.ztiany.androidav.stream.livescreen.Constants.WIDTH;

public class CodecLiveH265 extends Thread {

    private MediaCodec mediaCodec;

    private final MediaProjection mediaProjection;
    private final SocketLiveServer mSocketLiveServer;

    public CodecLiveH265(SocketLiveServer socketLiveServer, MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        this.mSocketLiveServer = socketLiveServer;
    }

    public void startLive() {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, Constants.WIDTH, Constants.HEIGHT);

            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, Constants.KEY_BIT_RATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, Constants.KEY_FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constants.KEY_I_FRAME_INTERVAL);

            mediaCodec = MediaCodec.createEncoderByType("video/hevc");
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = mediaCodec.createInputSurface();

            //创建场地【将录频信息输出到 surface】
            mediaProjection.createVirtualDisplay(
                    "-display",
                    WIDTH, HEIGHT,
                    1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    surface,
                    null,
                    null
            );

        } catch (IOException e) {
            e.printStackTrace();
        }

        start();
    }

    @Override
    public void run() {
        mediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true) {
            try {
                int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                if (outputBufferId >= 0) {
                    //编码好的H265的数据
                    ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outputBufferId);

                    //写成文件就能够播放
                    //byte[] outData = new byte[bufferInfo.size];
                    //byteBuffer.get(outData);
                    //以字符串的方式写入，用于分析
                    //writeContent(outData);
                    //writeBytes(outData);

                    //默认编码出的数据，默认只有第一帧有 VPS/SPS/PPS，但是网络传输中需要每一 I 帧都有 VPS/SPS/PPS。
                    dealFrame(byteBuffer, bufferInfo);
                    mediaCodec.releaseOutputBuffer(outputBufferId, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static final int NAL_I = 19;//NAL_UNIT_TYPE
    public static final int NAL_VPS = 32;//NAL_UNIT_TYPE

    private byte[] vps_sps_pps_buf;//VPS/SPS/PPS 占了一帧

    private void dealFrame(ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
        //获取类型【读 H265 Header】
        int offset = 4;
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
        int type = (bb.get(offset) & 0x7E) >> 1;

        //根据不同类型进行处理
        if (type == NAL_VPS) {//如果是 VPS，则缓存下来
            vps_sps_pps_buf = new byte[bufferInfo.size];
            bb.get(vps_sps_pps_buf);
        } else if (type == NAL_I) {//每次 I 帧之前，都加上 VPS

            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            byte[] newBuf = new byte[vps_sps_pps_buf.length + bytes.length];
            System.arraycopy(vps_sps_pps_buf, 0, newBuf, 0, vps_sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, vps_sps_pps_buf.length, bytes.length);
            this.mSocketLiveServer.sendData(newBuf);

        } else {//其他的帧不需要
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            this.mSocketLiveServer.sendData(bytes);
        }

    }

    public void writeBytes(byte[] array) {
        FileOutputStream writer = null;
        try {
            writer = new FileOutputStream(Environment.getExternalStorageDirectory() + "/codec.h265", true);
            writer.write(array);
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String writeContent(byte[] array) {
        char[] HEX_CHAR_TABLE = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        Timber.d("writeContent: %s", sb.toString());
        FileWriter writer = null;
        try {
            //打开一个写文件器，构造函数中的第二个参数 true 表示以追加形式写文件。
            writer = new FileWriter(Environment.getExternalStorageDirectory() + "/codecH265.txt", true);
            writer.write(sb.toString());
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}

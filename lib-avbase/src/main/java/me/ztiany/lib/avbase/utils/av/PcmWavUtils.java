package me.ztiany.lib.avbase.utils.av;

import android.media.AudioRecord;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import me.ztiany.lib.avbase.utils.IOUtils;
import timber.log.Timber;

/**
 * pcm 与 wav 文件相关工具方法
 */
public class PcmWavUtils {

    private static final int WAV_HEADER_LENGTH = 44;

    private static byte[] readWavHeader(InputStream inputStream) throws IOException {
        if (inputStream.available() < WAV_HEADER_LENGTH) {
            return null;
        }

        byte[] buffer = new byte[WAV_HEADER_LENGTH];
        int remaining = WAV_HEADER_LENGTH;

        while (remaining > 0) {
            int read = inputStream.read(buffer, WAV_HEADER_LENGTH - remaining, remaining);
            remaining -= read;
        }

        return buffer;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Write
    ///////////////////////////////////////////////////////////////////////////

    /**
     * pcm 文件转 wav 文件。
     *
     * @param inFilename    源文件路径
     * @param outFilename   目标文件路径
     * @param sampleRate    sample rate【采样率】
     * @param channelConfig channel config【声道配置】
     * @param encoding      encoding【位深度】
     */
    @WorkerThread
    public static void pcmToWav(
            String inFilename,
            String outFilename,
            int sampleRate,
            int channelConfig,
            int encoding
    ) {
        FileInputStream in;
        FileOutputStream out;

        long totalAudioLen;
        long riffChunkDataSize;
        int channelCount = AudioFormatEx.getChannelCount(channelConfig);
        int bitsPerSample = AudioFormatEx.getBitsPerSample(encoding);
        int byteRate = bitsPerSample * sampleRate * channelCount / 8/*bit to byte*/;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding);
        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            // RIFF chunk 的 data 大小，即文件总长度减去 (riffChunkId + riffChunkDataSize) 8 字节后数据部分总大小。
            riffChunkDataSize = totalAudioLen + 44/*header*/ - 8/*(riffChunkId + riffChunkDataSize)*/;
            writeWaveFileHeader(out, totalAudioLen, riffChunkDataSize, sampleRate, channelCount, bitsPerSample, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加入 wav 文件头
     */
    private static void writeWaveFileHeader(
            FileOutputStream out,
            long totalAudioLen,
            long riffChunkDataSize,
            long sampleRate,
            int channelCount,
            int bitsPerSample,
            long byteRate
    ) throws IOException {

        byte[] header = new byte[WAV_HEADER_LENGTH];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';

        header[4] = (byte) (riffChunkDataSize & 0xff);
        header[5] = (byte) ((riffChunkDataSize >> 8) & 0xff);
        header[6] = (byte) ((riffChunkDataSize >> 16) & 0xff);
        header[7] = (byte) ((riffChunkDataSize >> 24) & 0xff);

        header[8] = 'W';  //WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';

        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;   // format = 1
        header[21] = 0;
        header[22] = (byte) channelCount;
        header[23] = 0;

        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);

        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);

        header[32] = (byte) (2 * bitsPerSample / 8); // block align
        header[33] = 0;
        header[34] = (byte) bitsPerSample;  // bits per sample
        header[35] = 0;

        header[36] = 'd'; //data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';

        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, WAV_HEADER_LENGTH);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Parse
    ///////////////////////////////////////////////////////////////////////////

    @Nullable
    public static WavFile parseWavFile(InputStream inputStream, boolean readWholeData, boolean autoClose) {
        try {
            byte[] header = readWavHeader(inputStream);
            if (header != null) {
                WavFile wavFile = fillWavInfo(header);
                if (readWholeData && wavFile != null) {
                    wavFile.audioData = readWavData(inputStream, false, false);
                }
                return wavFile;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (autoClose) {
                IOUtils.closeSafely(inputStream);
            }
        }

        return null;
    }

    private static WavFile fillWavInfo(byte[] header) {
        //step1：check RIFF
        if ('R' != header[0] || 'I' != header[1] || 'F' != header[2] || 'F' != header[3]) {
            return null;
        }

        WavFile wavHeader = new WavFile();

        wavHeader.channelCount = header[22];

        wavHeader.sampleRate = 0;
        wavHeader.sampleRate |= Byte.toUnsignedInt(header[24]);
        wavHeader.sampleRate |= (Byte.toUnsignedInt(header[25])) << 8;
        wavHeader.sampleRate |= (Byte.toUnsignedInt(header[26])) << 16;
        wavHeader.sampleRate |= (Byte.toUnsignedInt(header[27])) << 24;

        wavHeader.bitsPerSample = header[34];

        wavHeader.byteRate = 0;
        wavHeader.byteRate |= Byte.toUnsignedInt(header[28]);
        wavHeader.byteRate |= (Byte.toUnsignedInt(header[29])) << 8;
        wavHeader.byteRate |= (Byte.toUnsignedInt(header[30])) << 16;
        wavHeader.byteRate |= (Byte.toUnsignedInt(header[31])) << 24;

        wavHeader.audioLength = 0;
        wavHeader.audioLength |= Byte.toUnsignedInt(header[40]);
        wavHeader.audioLength |= (Byte.toUnsignedInt(header[41])) << 8;
        wavHeader.audioLength |= (Byte.toUnsignedInt(header[42])) << 16;
        wavHeader.audioLength |= (Byte.toUnsignedInt(header[43])) << 24;

        wavHeader.audioEncoding = AudioFormatEx.getAudioEncoding(wavHeader.bitsPerSample);
        wavHeader.channelConfig = AudioFormatEx.getChannelOutConfig(wavHeader.channelCount);

        return wavHeader;
    }

    public static class WavFile {

        private int channelCount;
        private int sampleRate;
        private int bitsPerSample;
        private int byteRate;
        private int channelConfig;
        private int audioEncoding;
        private int audioLength;
        private byte[] audioData;

        public int getChannelCount() {
            return channelCount;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public int getBitsPerSample() {
            return bitsPerSample;
        }

        public int getByteRate() {
            return byteRate;
        }

        public int getAudioEncoding() {
            return audioEncoding;
        }

        public int getChannelConfig() {
            return channelConfig;
        }

        public int getAudioLength() {
            return audioLength;
        }

        @Nullable
        public byte[] getAudioData() {
            return audioData;
        }

        @NonNull
        @Override
        public String toString() {
            return "WavHeader{" +
                    "channelCount=" + channelCount +
                    ", sampleRate=" + sampleRate +
                    ", bitsPerSample=" + bitsPerSample +
                    ", byteRate=" + byteRate +
                    ", channelConfig=" + channelConfig +
                    ", audioEncoding=" + audioEncoding +
                    ", audioLength=" + audioLength +
                    ", hasAudioData=" + (audioData != null) +
                    '}';
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Read
    ///////////////////////////////////////////////////////////////////////////

    public static byte[] readWavData(InputStream inputStream, boolean skipHeader, boolean autoClose) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        try {
            if (skipHeader) {
                readWavHeader(inputStream);
            }
            while ((read = inputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, read);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException exception) {
            Timber.e(exception, "readAudioData");
        } finally {
            if (autoClose) {
                IOUtils.closeSafely(inputStream);
            }
        }

        return null;
    }

}
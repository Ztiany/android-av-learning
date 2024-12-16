package me.ztiany.androidav.avapi.audio.mixing;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import me.ztiany.lib.avbase.utils.Directory;
import timber.log.Timber;

public class VideoAudioMixing {

    private static final int TIMEOUT = 1000;

    /**
     * @param startTimeUs 微妙
     * @param endTimeUs   微妙
     * @param videoVolume 0-100
     * @param bgVolume    0-100
     */
    public static void mixAudioTrack(
            final String videoInput,
            final String bgAudioInput,
            final String output,
            final int startTimeUs,
            final int endTimeUs,
            int videoVolume,
            int bgVolume
    ) throws Exception {
        //定义输出文件
        File bgPCM = Directory.createSDCardRootAppPath("bg.pcm");
        File videoPCM = Directory.createSDCardRootAppPath("video.pcm");
        File mixedPCM = Directory.createSDCardRootAppPath("mixed.pcm");
        File mixedWAV = Directory.createSDCardRootAppPath("mixed.wav");

        //TODO: 根据媒体时长调整时间。
        adjustStartAndEnd(videoInput, bgAudioInput, startTimeUs, endTimeUs);

        //extra pcm, than we has two pcm files which have the same duration.
        decodeToPCM(videoInput, videoPCM.getAbsolutePath(), startTimeUs, endTimeUs);
        decodeToPCM(bgAudioInput, bgPCM.getAbsolutePath(), startTimeUs, endTimeUs);
        //mix pcm
        mixPcm(videoPCM.getAbsolutePath(), bgPCM.getAbsolutePath(), mixedPCM.getAbsolutePath(), videoVolume, bgVolume);
        //pcm -> wav
        new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, 2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(mixedPCM.getAbsolutePath(), mixedWAV.getAbsolutePath());
        //mix video and wav
        mixVideoAndMusic(videoInput, output, startTimeUs, endTimeUs, mixedWAV);
    }

    private static void adjustStartAndEnd(String videoInput, String bgAudioInput, int startTimeUs, int endTimeUs) throws IOException {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

        mediaMetadataRetriever.setDataSource(videoInput);
        final int videoInputDurationMs = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        Timber.d("videoInputDurationMs = %d", videoInputDurationMs);
        mediaMetadataRetriever.setDataSource(bgAudioInput);

        mediaMetadataRetriever.setDataSource(bgAudioInput);
        final int bgAudioInputDurationMs = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        Timber.d("bgAudioInputDurationMs = %d", bgAudioInputDurationMs);
        mediaMetadataRetriever.release();

        final int cuttingDurationMs = (endTimeUs - startTimeUs) / 1000;
        Timber.d("cuttingDurationMs = %d", cuttingDurationMs);
    }

    private static void mixVideoAndMusic(String videoInput, String output, int startTimeUs, int endTimeUs, File wavFile) throws IOException {
        //媒体提取器，获取视频信息
        MediaExtractor videoExtractor = new MediaExtractor();
        videoExtractor.setDataSource(videoInput);
        //获取频中的视频信息
        int videoIndex = selectTrack(videoExtractor, false);
        MediaFormat videoFormat = videoExtractor.getTrackFormat(videoIndex);
        //获取视频中的音频信息
        int audioIndex = selectTrack(videoExtractor, true);
        MediaFormat audioFormat = videoExtractor.getTrackFormat(audioIndex);
        int audioBitrate = audioFormat.getInteger(MediaFormat.KEY_BIT_RATE);//用于后续 AAC 编码
        audioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);//确保输出的是 AAC


        //创建并配置混流器
        MediaMuxer mediaMuxer = new MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        //添加一个视频轨【使用原来的格式】
        int muxerVideoIndex = mediaMuxer.addTrack(videoFormat);
        //添加一个音频轨【使用原来的信息】
        int muxerAudioIndex = mediaMuxer.addTrack(audioFormat);
        //开启混流器
        mediaMuxer.start();


        //------------------------------------------------ 开始混入音频 ------------------------------------------------
        //待混入音频的 MediaExtractor
        MediaExtractor wavExtractor = new MediaExtractor();
        wavExtractor.setDataSource(wavFile.getAbsolutePath());
        int audioTrack = selectTrack(wavExtractor, true);
        wavExtractor.selectTrack(audioTrack);
        MediaFormat wavFormat = wavExtractor.getTrackFormat(audioTrack);

        int maxBufferSize;
        if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = wavFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } else {
            maxBufferSize = 100 * 1000;
        }

        MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate);//比特率
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);//音质等级
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxBufferSize);

        MediaCodec encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        encoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        boolean encodeDone = false;
        while (!encodeDone) {
            int inputBufferIndex = encoder.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {
                long sampleTime = wavExtractor.getSampleTime();
                if (sampleTime < 0) {
                    encoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    int flags = wavExtractor.getSampleFlags();
                    int size = wavExtractor.readSampleData(buffer, 0);
                    ByteBuffer inputBuffer = encoder.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(buffer);
                    inputBuffer.position(0);
                    encoder.queueInputBuffer(inputBufferIndex, 0, size, sampleTime, flags);
                    wavExtractor.advance();
                }
            }

            //获取编码完的数据
            int outputBufferIndex = encoder.dequeueOutputBuffer(info, TIMEOUT);
            while (outputBufferIndex >= 0) {
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    encodeDone = true;
                    break;
                }
                ByteBuffer encodeOutputBuffer = encoder.getOutputBuffer(outputBufferIndex);
                mediaMuxer.writeSampleData(muxerAudioIndex, encodeOutputBuffer, info);
                encodeOutputBuffer.clear();
                encoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = encoder.dequeueOutputBuffer(info, TIMEOUT);
            }
        }

        if (audioTrack >= 0) {
            videoExtractor.unselectTrack(audioTrack);
        }
        //------------------------------------------------ 混入音频完毕 ------------------------------------------------


        //------------------------------------------------ 开始混入视频 ------------------------------------------------
        //开始添加视频
        videoExtractor.selectTrack(videoIndex);
        videoExtractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        maxBufferSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        buffer = ByteBuffer.allocateDirect(maxBufferSize);

        //封装容器添加视频轨道信息
        while (true) {
            long sampleTimeUs = videoExtractor.getSampleTime();
            if (sampleTimeUs == -1) {
                break;
            }
            if (sampleTimeUs < startTimeUs) {
                videoExtractor.advance();
                continue;
            }
            if (sampleTimeUs > endTimeUs) {
                break;
            }

            //TODO: 为什么加 600？
            info.presentationTimeUs = sampleTimeUs - startTimeUs + 600;
            info.flags = videoExtractor.getSampleFlags();
            info.size = videoExtractor.readSampleData(buffer, 0);
            if (info.size < 0) {
                break;
            }
            mediaMuxer.writeSampleData(muxerVideoIndex, buffer, info);
            videoExtractor.advance();
        }
        //------------------------------------------------ 混入视频完毕 ------------------------------------------------

        try {
            wavExtractor.release();
            videoExtractor.release();
            encoder.stop();
            encoder.release();
            mediaMuxer.release();
        } catch (Exception e) {
            Timber.e(e, "mixVideoAndMusic");
        }
    }

    private static float normalizeVolume(int volume) {
        return volume / 100f * 1;
    }

    /**
     * 混音：线性叠加平均算法。
     */
    public static void mixPcm(String pcm1Path, String pcm2Path, String toPath, int vol1, int vol2) throws IOException {
        float volume1 = normalizeVolume(vol1);
        float volume2 = normalizeVolume(vol2);

        byte[] pcm1Buffer = new byte[2048];
        byte[] pcm2Buffer = new byte[2048];
        byte[] outputBuffer = new byte[2048];

        boolean end1 = false, end2 = false;
        short temp2, temp1;
        int temp;

        try (
                FileInputStream pcm1InputStream = new FileInputStream(pcm1Path);
                FileInputStream pcm2InputStream = new FileInputStream(pcm2Path);
                FileOutputStream pcmOutputStream = new FileOutputStream(toPath)
        ) {
            while (!end1 || !end2) {
                if (!end1) {
                    end1 = (pcm1InputStream.read(pcm1Buffer) == -1);
                    System.arraycopy(pcm1Buffer, 0, outputBuffer, 0, pcm1Buffer.length);//如果两个声音不一样长，那么保留第一个声音。
                }
                if (!end2) {
                    end2 = (pcm2InputStream.read(pcm2Buffer) == -1);

                    for (int i = 0; i < pcm2Buffer.length; i += 2) {
                        //组合声音
                        temp1 = (short) ((pcm1Buffer[i] & 0xff) | (pcm1Buffer[i + 1] & 0xff) << 8);//声音 1 的值，两个字节
                        temp2 = (short) ((pcm2Buffer[i] & 0xff) | (pcm2Buffer[i + 1] & 0xff) << 8);//声音 1 的值，两个字节
                        temp = (int) (temp2 * volume2 + temp1 * volume1);

                        //防止超出
                        if (temp > 32767) {
                            temp = 32767;
                        } else if (temp < -32768) {
                            temp = -32768;
                        }

                        outputBuffer[i] = (byte) (temp & 0xFF);
                        outputBuffer[i + 1] = (byte) ((temp >>> 8) & 0xFF);
                    }
                }

                pcmOutputStream.write(outputBuffer);
            }
        }
    }

    public static void decodeToPCM(String musicPath, String outPath, int startTime, int endTime) throws Exception {
        if (endTime < startTime) {
            return;
        }

        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(musicPath);
        int audioTrack = selectTrack(mediaExtractor, true);
        mediaExtractor.selectTrack(audioTrack);
        mediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        MediaFormat audioFormat = mediaExtractor.getTrackFormat(audioTrack);

        int maxBufferSize;
        if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } else {
            maxBufferSize = 100 * 1000;
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
        MediaCodec mediaCodec = MediaCodec.createDecoderByType(audioFormat.getString((MediaFormat.KEY_MIME)));
        mediaCodec.configure(audioFormat, null, null, 0);
        File pcmFile = new File(outPath);
        FileChannel writeChannel = new FileOutputStream(pcmFile).getChannel();
        mediaCodec.start();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int outputBufferIndex;

        while (true) {
            int decodeInputIndex = mediaCodec.dequeueInputBuffer(1000);
            if (decodeInputIndex >= 0) {
                long sampleTimeUs = mediaExtractor.getSampleTime();
                if (sampleTimeUs == -1) {
                    break;
                } else if (sampleTimeUs < startTime) {
                    mediaExtractor.advance();
                    continue;
                } else if (sampleTimeUs > endTime) {
                    break;
                }

                info.size = mediaExtractor.readSampleData(buffer, 0);
                info.presentationTimeUs = sampleTimeUs;
                info.flags = mediaExtractor.getSampleFlags();

                byte[] content = new byte[buffer.remaining()];
                buffer.get(content);
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(decodeInputIndex);
                inputBuffer.put(content);
                mediaCodec.queueInputBuffer(decodeInputIndex, 0, info.size, info.presentationTimeUs, info.flags);
                mediaExtractor.advance();
            }

            outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 1_000);
            while (outputBufferIndex >= 0) {
                ByteBuffer decodeOutputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                writeChannel.write(decodeOutputBuffer);
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 1_000);
            }
        }

        writeChannel.close();
        mediaExtractor.release();
        mediaCodec.stop();
        mediaCodec.release();
    }

    public static int selectTrack(MediaExtractor extractor, boolean audio) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (audio) {
                if (mime.startsWith("audio/")) {
                    return i;
                }
            } else {
                if (mime.startsWith("video/")) {
                    return i;
                }
            }
        }
        return -5;
    }

}

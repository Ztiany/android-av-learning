package me.ztiany.androidav.avapi.video.combine;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 视频拼接
 */
class VideoCombining {

    /**
     * 视频拼接
     *
     * @param inputPath1 视频路径 1
     * @param inputPath2 视频路径 2
     * @param outputPath 输出路径
     */
    public static boolean combine(String inputPath1, String inputPath2, String outputPath) throws IOException {
        MediaMuxer mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        MediaExtractor videoExtractor1 = new MediaExtractor();
        videoExtractor1.setDataSource(inputPath1);

        MediaExtractor videoExtractor2 = new MediaExtractor();
        videoExtractor2.setDataSource(inputPath2);

        int videoTrackIndex = -1;
        int audioTrackIndex = -1;

        long file1_duration = 0L;

        int sourceVideoTrack1 = -1;
        int sourceAudioTrack1 = -1;

        int sourceVideoTrack2 = -1;
        int sourceAudioTrack2 = -1;

        for (int index = 0; index < videoExtractor1.getTrackCount(); index++) {
            MediaFormat format = videoExtractor1.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            file1_duration = format.getLong(MediaFormat.KEY_DURATION);
            if (mime.startsWith("video/")) {
                sourceVideoTrack1 = index;
                videoTrackIndex = mediaMuxer.addTrack(format);
            } else if (mime.startsWith("audio/")) {
                sourceAudioTrack1 = index;
                audioTrackIndex = mediaMuxer.addTrack(format);
            }
        }

        for (int index = 0; index < videoExtractor2.getTrackCount(); index++) {
            MediaFormat format = videoExtractor2.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                sourceVideoTrack2 = index;
            } else if (mime.startsWith("audio/")) {
                sourceAudioTrack2 = index;
            }
        }

        mediaMuxer.start();

        //1.write first video track into muxer.
        videoExtractor1.selectTrack(sourceVideoTrack1);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        int sampleSize;
        while ((sampleSize = videoExtractor1.readSampleData(buffer, 0)) > 0) {
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor1.getSampleFlags();
            info.presentationTimeUs = videoExtractor1.getSampleTime();
            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
            videoExtractor1.advance();
        }

        //2.write first audio track into muxer.
        videoExtractor1.unselectTrack(sourceVideoTrack1);
        videoExtractor1.selectTrack(sourceAudioTrack1);
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500 * 1024);
        while ((sampleSize = videoExtractor1.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor1.getSampleFlags();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            info.presentationTimeUs = videoExtractor1.getSampleTime();
            mediaMuxer.writeSampleData(audioTrackIndex, buffer, info);
            videoExtractor1.advance();
        }

        //3.write second video track into muxer.
        videoExtractor2.selectTrack(sourceVideoTrack2);
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500 * 1024);
        while ((sampleSize = videoExtractor2.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor2.getSampleFlags();
            info.presentationTimeUs = videoExtractor2.getSampleTime() + file1_duration;
            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
            videoExtractor2.advance();
        }

        //4.write second audio track into muxer.
        videoExtractor2.unselectTrack(sourceVideoTrack2);
        videoExtractor2.selectTrack(sourceAudioTrack2);
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500 * 1024);
        while ((sampleSize = videoExtractor2.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor2.getSampleFlags();
            info.presentationTimeUs = videoExtractor2.getSampleTime() + file1_duration;
            mediaMuxer.writeSampleData(audioTrackIndex, buffer, info);
            videoExtractor2.advance();
        }

        videoExtractor1.release();
        videoExtractor2.release();
        mediaMuxer.stop();
        mediaMuxer.release();

        return true;
    }

}
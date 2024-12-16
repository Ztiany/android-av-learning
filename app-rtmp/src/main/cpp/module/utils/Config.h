#ifndef ANDROID_AV_CONFIG_H
#define ANDROID_AV_CONFIG_H

// 参考 FFmpeg
enum RTMPChannel {
    RTMP_NETWORK_CHANNEL = 2,   ///< channel for network-related messages (bandwidth report, ping, etc)
    RTMP_SYSTEM_CHANNEL,        ///< channel for sending server control messages
    RTMP_AUDIO_CHANNEL,         ///< channel for audio data
    RTMP_VIDEO_CHANNEL = 6,   ///< channel for video data
    RTMP_SOURCE_CHANNEL = 8,   ///< channel for a/v invokes
};

enum VideoType {
    YUV,
    X264
};

enum VideoFormat {
    I420,
    NV21,
    NV12
};

enum AudioType {
    PCM = 2, AAC_INFO, AAC_DATA
};

#endif //ANDROID_AV_CONFIG_H

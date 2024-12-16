#include "OpenSLESKit.h"

SLuint32 mapSlSampleRate(int32_t sampleRate) {
    SLuint32 result = -1;
    switch (sampleRate) {
        case 8000:
            result = SL_SAMPLINGRATE_8;
            break;
        case 11025:
            result = SL_SAMPLINGRATE_11_025;
            break;
        case 16000:
            result = SL_SAMPLINGRATE_16;
            break;
        case 22050:
            result = SL_SAMPLINGRATE_22_05;
            break;
        case 24000:
            result = SL_SAMPLINGRATE_24;
            break;
        case 32000:
            result = SL_SAMPLINGRATE_32;
            break;
        case 44100:
            result = SL_SAMPLINGRATE_44_1;
            break;
        case 48000:
            result = SL_SAMPLINGRATE_48;
            break;
        case 64000:
            result = SL_SAMPLINGRATE_64;
            break;
        case 88200:
            result = SL_SAMPLINGRATE_88_2;
            break;
        case 96000:
            result = SL_SAMPLINGRATE_96;
            break;
        case 192000:
            result = SL_SAMPLINGRATE_192;
            break;
        default:
            break;
    }
    return result;
}

SLuint32 mapSlChannel(int channelCount) {
    if (channelCount > 1) {
        return SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    } else {
        return SL_SPEAKER_FRONT_CENTER;
    }
}

SLuint16 mapSlFormat(int bitsPerSample) {
    SLuint16 result = -1;
    switch (bitsPerSample) {
        case 8:
            result = SL_PCMSAMPLEFORMAT_FIXED_8;
            break;
        case 16:
            result = SL_PCMSAMPLEFORMAT_FIXED_16;
            break;
        case 20:
            result = SL_PCMSAMPLEFORMAT_FIXED_20;
            break;
        case 24:
            result = SL_PCMSAMPLEFORMAT_FIXED_24;
            break;
        case 28:
            result = SL_PCMSAMPLEFORMAT_FIXED_28;
            break;
        case 32:
            result = SL_PCMSAMPLEFORMAT_FIXED_32;
            break;
        default:
            break;
    }
    return result;
}

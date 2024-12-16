#include "OpenSLESKit.h"
#include <log.h>

// SL 引擎
static SLObjectItf engineObject = nullptr;
static SLEngineItf engineInterface = nullptr;

//混音器
static SLObjectItf outputMixObject = nullptr;
static SLEnvironmentalReverbItf outputMixEnvironmentalReverb = nullptr;
// aux effect on the output mix, used by the buffer queue player
static const SLEnvironmentalReverbSettings reverbSettings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;

//播放器
static SLObjectItf playerObject = nullptr;
static SLPlayItf playerPlayInterface = nullptr;
static SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;

static bool createSLEngine() {
    SLresult sLResult;
    sLResult = slCreateEngine(&engineObject, 0, nullptr, 0, nullptr, nullptr);
    if (sLResult != SL_RESULT_SUCCESS) {
        LOGE("slCreateEngine error");
        return false;
    }
    LOGD("slCreateEngine success");

    sLResult = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    if (sLResult != SL_RESULT_SUCCESS) {
        LOGE("engineObject Realize error");
        return false;
    }
    LOGD("engineObject Realize success");

    sLResult = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineInterface);
    if (sLResult != SL_RESULT_SUCCESS) {
        LOGE("engineObject GetInterface(SL_IID_ENGINE) error");
        LOGE("engineObject GetInterface(SL_IID_ENGINE) error");
    }
    LOGD("engineObject GetInterface(SL_IID_ENGINE) success");
    return true;
}

static bool createAudioMix() {
    SLresult sLResult;

    // create output mix, with environmental reverb specified as a non-required interface
    const SLInterfaceID ids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean req[1] = {SL_BOOLEAN_FALSE};
    sLResult = (*engineInterface)->CreateOutputMix(engineInterface, &outputMixObject, 1, ids, req);
    if (sLResult != SL_RESULT_SUCCESS) {
        LOGE("CreateOutputMix error");
        return false;
    }
    LOGD("CreateOutputMix success");

    sLResult = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if (sLResult != SL_RESULT_SUCCESS) {
        LOGE("outputMixObject Realize error");
        return false;
    }
    LOGD("outputMixObject Realize success");

    // get the environmental reverb interface
    // this could fail if the environmental reverb effect is not available,
    // either because the feature is not present, excessive CPU load, or
    // the required MODIFY_AUDIO_SETTINGS permission was not requested and granted
    sLResult = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB, &outputMixEnvironmentalReverb);
    if (SL_RESULT_SUCCESS == sLResult) {
        // ignore unsuccessful result codes for environmental reverb, as it is optional for this example
        LOGD("outputMixObject GetInterface(SL_IID_ENVIRONMENTALREVERB) success");
        sLResult = (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(outputMixEnvironmentalReverb, &reverbSettings);
        if (SL_RESULT_SUCCESS == sLResult) {
            LOGD("outputMixObject SetEnvironmentalReverbProperties success");
        } else {
            LOGE("outputMixObject SetEnvironmentalReverbProperties error");
        }
    } else {
        LOGE("outputMixObject GetInterface(SL_IID_ENVIRONMENTALREVERB) error");
    }

    return true;
}

static bool createAudioPlayer(PcmPlayerConfig &pcmConfig) {
    SLresult sLResult;

    //创建 DataSink，用于指定输出目标
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink dataSink = {&outputMix, nullptr};

    //场景数据源，用于指定输入目标【这里使用的是缓存队列，缓存队列模式只能使用 PCM 数据，因此这里需要指定 PCM 的格式】
    SLDataLocator_AndroidSimpleBufferQueue que = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 5};
    //音频格式
    SLDataFormat_PCM pcm = {
            SL_DATAFORMAT_PCM,
            static_cast<SLuint32>(pcmConfig.channelCount),//声道数
            mapSlSampleRate(pcmConfig.sampleRate),
            mapSlFormat(pcmConfig.bitsPerSample),
            mapSlFormat(pcmConfig.bitsPerSample),
            mapSlChannel(pcmConfig.channelCount),
            SL_BYTEORDER_LITTLEENDIAN //字节序为小端
    };
    SLDataSource dataSource = {&que, &pcm};

    //根据 sink 和 source 来创建播放器
    const SLInterfaceID ids[2] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME};
    const SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

    sLResult = (*engineInterface)->CreateAudioPlayer(
            engineInterface,
            &playerObject,
            &dataSource,
            &dataSink,
            2,
            ids,
            req
    );
    if (sLResult != SL_RESULT_SUCCESS) {
        LOGE("CreateAudioPlayer error");
        return false;
    }
    LOGD("CreateAudioPlayer success");

    sLResult = (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
    if (sLResult != SL_RESULT_SUCCESS) {
        LOGE("playerObject Realize error");
        return false;
    }
    LOGD("playerObject Realize  success");

    //获取 Player 接口
    sLResult = (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &playerPlayInterface);
    if (sLResult != SL_RESULT_SUCCESS) {
        LOGE("playerObject GetInterface(SL_IID_PLAY) error");
        return false;
    }
    LOGD("playerObject GetInterface(SL_IID_PLAY)  success");

    sLResult = (*playerObject)->GetInterface(playerObject, SL_IID_BUFFERQUEUE, &bqPlayerBufferQueue);
    if (sLResult != SL_RESULT_SUCCESS) {
        LOGE("playerObject GetInterface(SL_IID_BUFFERQUEUE) error");
    }
    LOGD("playerObject GetInterface(SL_IID_BUFFERQUEUE)  success");

    return true;
}

//========================================== Play PCM File Synchronously ==========================================
static FILE *pcmFile = nullptr;
static char *pcmBuffer = nullptr;
static int pcmBufferLength = 0;

int32_t commonPcmBufferSize(PcmPlayerConfig &pcmPlayerConfig, int durationPerBufferInMS) {
    int32_t bufferSize = pcmPlayerConfig.sampleRate * durationPerBufferInMS * pcmPlayerConfig.channelCount / 1000;
    LOGI("commonPcmBufferSize result %d for time %d", bufferSize, durationPerBufferInMS);
    return bufferSize;
}

bool createPcmPlayer(PcmPlayerConfig &pcmConfig) {
    bool success = createSLEngine();
    if (success) {
        success = createAudioMix();
    }
    if (success) {
        createAudioPlayer(pcmConfig);
    }
    return success;
}

void synchronousPcmCallback(SLAndroidSimpleBufferQueueItf bf, void *context) {
    if (!pcmBuffer) {
        LOGE("synchronousPcmCallback is called, but pcmBuffer == null");
        return;
    }
    if (!pcmFile) {
        LOGE("synchronousPcmCallback is called, but pcmFile == null");
        return;
    }
    if (feof(pcmFile) == 0) {
        size_t len = fread(pcmBuffer, 1, pcmBufferLength, pcmFile);
        if (len > 0) {
            (*bf)->Enqueue(bf, pcmBuffer, len);
        } else {
            LOGD("synchronousPcmCallback is called, but read nothing");
        }
    } else {
        LOGD("synchronousPcmCallback is called, but pcmFile reached EOF");
        (*playerPlayInterface)->SetPlayState(playerPlayInterface, SL_PLAYSTATE_PAUSED);
    }
}

static void releasePcmFileAndBuffer() {
    if (pcmFile) {
        fclose(pcmFile);
        pcmFile = nullptr;
    }
    if (pcmBuffer) {
        delete[] pcmBuffer;
        pcmBuffer = nullptr;
    }
}

static bool reInitPcmFileAndBuffer(const char *filePath, PcmPlayConfig &pcmPlayConfig) {
    releasePcmFileAndBuffer();
    pcmBuffer = new char[pcmPlayConfig.bufferSize];
    pcmBufferLength = pcmPlayConfig.bufferSize;
    pcmFile = fopen(filePath, "rb");
    if (!pcmFile) {
        LOGE("open %s error", filePath);
        return false;
    }
    return true;
}

bool startPcmPlayerForFilePath(const char *filePath, PcmPlayConfig &pcmPlayConfig) {
    if (reInitPcmFileAndBuffer(filePath, pcmPlayConfig)) {
        SLresult sLResult;

        //设置回调函数，播放队列空调用
        sLResult = (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue, synchronousPcmCallback, nullptr);
        if (sLResult != SL_RESULT_SUCCESS) {
            LOGE("RegisterCallback error");
            return false;
        }
        LOGD("RegisterCallback success");

        //设置为播放状态
        sLResult = (*playerPlayInterface)->SetPlayState(playerPlayInterface, SL_PLAYSTATE_PLAYING);
        if (sLResult != SL_RESULT_SUCCESS) {
            LOGE("SetPlayState(SL_PLAYSTATE_PLAYING) error");
            return false;
        }
        LOGD("SetPlayState(SL_PLAYSTATE_PLAYING) success");

        //启动队列回调
        (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, "", 1);
        return true;
    }

    LOGE("startPcmPlayerForFilePath error");
    return false;
}

bool pausePcmPlayer() {
    if (playerPlayInterface) {
        SLresult sLResult = (*playerPlayInterface)->SetPlayState(playerPlayInterface, SL_PLAYSTATE_PAUSED);
        if (sLResult != SL_RESULT_SUCCESS) {
            LOGE("pausePcmPlayer error");
            return false;
        }
        LOGD("pausePcmPlayer success");
        return true;
    }
    return false;
}

bool resumePcmPlayer() {
    if (playerPlayInterface) {
        SLresult sLResult = (*playerPlayInterface)->SetPlayState(playerPlayInterface, SL_PLAYSTATE_PLAYING);
        if (sLResult != SL_RESULT_SUCCESS) {
            LOGE("resumePcmPlayer error");
            return false;
        }
        LOGD("resumePcmPlayer success");
        return true;
    }
    return false;
}

bool stopPcmPlayerForFilePath() {
    releasePcmFileAndBuffer();
    if (playerPlayInterface) {
        (*playerPlayInterface)->SetPlayState(playerPlayInterface, SL_PLAYSTATE_STOPPED);
    }

    // destroy buffer queue audio player object, and invalidate all associated interfaces
    if (playerObject != nullptr) {
        (*playerObject)->Destroy(playerObject);
        playerObject = nullptr;
        playerPlayInterface = nullptr;
        bqPlayerBufferQueue = nullptr;
    }

    // destroy output mix object, and invalidate all associated interfaces
    if (outputMixObject != nullptr) {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = nullptr;
        outputMixEnvironmentalReverb = nullptr;
    }

    // destroy engine object, and invalidate all associated interfaces
    if (engineObject != nullptr) {
        (*engineObject)->Destroy(engineObject);
        engineObject = nullptr;
        engineInterface = nullptr;
    }

    LOGD("stopPcmPlayerForFilePath success");
    return true;
}

//========================================== Play PCM File Synchronously ==========================================

package me.ztiany.rtmp.common;

import timber.log.Timber;

public class RtmpPusher {

    static {
        System.loadLibrary("android_rtmp");
    }

    public static final int VIDEO_FORMAT_I420 = 0;
    public static final int VIDEO_FORMAT_NV21 = 1;
    public static final int VIDEO_FORMAT_NV12 = 2;

    public static final int VIDEO_TYPE_YUV = 0;
    public static final int VIDEO_TYPE_X264 = 1;
    public static final int AUDIO_TYPE_PCM = 2;
    public static final int AUDIO_TYPE_AAC_INFO = 3;
    public static final int AUDIO_TYPE_AAC_DATA = 4;

    private Callback mCallback;

    private static final RtmpPusher M_RTMP_PUSHER = new RtmpPusher();

    private RtmpPusher() {

    }

    public static RtmpPusher getInstance() {
        return M_RTMP_PUSHER;
    }

    public native void init();

    public native void initVideoCodec(int width, int height, int fps, int bitrate, int format);

    public native void initAudioCodec(int sampleRate, int channels);

    public native void start(String url);

    public native void sendVideoPacket(byte[] data, int videoType, long tms);

    public native void sendAudioPacket(byte[] data, int audioType, long tms);

    public native void stop();

    public native void release();

    public interface Callback {
        void onInitSuccess();

        void onInitFailed();

        void onSendError();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Calling by jni.
    ///////////////////////////////////////////////////////////////////////////
    public void onInitResult(boolean succeeded) {
        Timber.d("onInitResult() called with: succeeded = [" + succeeded + "]");
        if (mCallback != null) {
            if (succeeded) {
                mCallback.onInitSuccess();
            } else {
                mCallback.onInitFailed();
            }
        }
    }

    public void onSendError() {
        Timber.d("onSendError() called");
        if (mCallback != null) {
            mCallback.onSendError();
        }
    }

}

package me.ztiany.lib.avbase.camera.camera2;

import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class MediaRecorderProvider implements OutputProvider {

    private final MediaRecorder mMediaRecorder;

    private final AtomicBoolean mIsRecording = new AtomicBoolean(false);

    private Integer mOrientationHint;

    private Camera2Handle mCamera2Handle;

    private final List<Size> mSupportedSize = new ArrayList<>();

    public MediaRecorderProvider() {
        mMediaRecorder = new MediaRecorder();
    }

    @Override
    public void onAttach(@NonNull Camera2Handle camera2Handle, @NonNull Components components) {
        Timber.d("onAttach is called. mOrientationHint = %s", components.require(ORIENTATION).toString());
        StreamConfigurationMap streamConfigurationMap = components.require(STREAM_CONFIGURATION);
        Size[] outputSizes = streamConfigurationMap.getOutputSizes(MediaRecorder.class);
        Timber.d("output sizes for MediaRecorder:");
        for (Size outputSize : outputSizes) {
            Timber.d(outputSize.toString());
        }

        mCamera2Handle = camera2Handle;
        mOrientationHint = components.require(ORIENTATION);
        mSupportedSize.clear();
        mSupportedSize.addAll(Arrays.asList(outputSizes));
    }

    @Override
    public void onDetach() {
        Timber.d("onDetach is called.");
        release();
    }

    public void release() {
        stop(false);
        try {
            mMediaRecorder.release();
        } catch (Exception exception) {
            Timber.e(exception, "MediaRecorderProvider.release()");
        }
    }

    public void start(@NonNull VideoSpec videoSpec, @Nullable StartCallback startCallback) {
        if (!mIsRecording.compareAndSet(false, true)) {
            Timber.w("MediaRecorderProvider is already started!");
            if (startCallback != null) {
                startCallback.onResult(false);
            }
            return;
        }

        Timber.d("MediaRecorderProvider.start()");

        Size targetSize = null;
        for (Size size : mSupportedSize) {
            if (size.getWidth() == videoSpec.videoWidth
                    && size.getHeight() == videoSpec.videoHeight) {
                targetSize = size;
                break;
            }
        }

        if (targetSize == null) {
            mIsRecording.set(false);
            if (startCallback != null) {
                startCallback.onResult(false);
            }
            Timber.d("MediaRecorderProvider.start is called, but no supported size found!");
            return;
        }

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoSize(targetSize.getWidth(), targetSize.getHeight());
        mMediaRecorder.setVideoFrameRate(videoSpec.frameRate);
        mMediaRecorder.setOrientationHint(mOrientationHint);
        mMediaRecorder.setOutputFile(videoSpec.storePath);

        try {
            mMediaRecorder.prepare();
        } catch (IOException ioException) {
            mIsRecording.set(false);
            Timber.e(ioException, "MediaRecorderProvider.start()");
            if (startCallback != null) {
                startCallback.onResult(false);
            }
        }

        mCamera2Handle.startCapturingCameraSession(mMediaRecorder.getSurface(), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                mMediaRecorder.start();
                if (startCallback != null) {
                    startCallback.onResult(true);
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                mIsRecording.set(false);
                if (startCallback != null) {
                    startCallback.onResult(false);
                }
            }
        });
    }

    public boolean stop() {
        return stop(true);
    }

    public boolean stop(boolean recoverPreview) {
        if (!mIsRecording.compareAndSet(true, false)) {
            return false;
        }

        Timber.d("MediaRecorderProvider.stop()");

        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            if (recoverPreview) {
                mCamera2Handle.stopCapturingCameraSession();
            }
        } catch (Exception exception) {
            Timber.e(exception, "MediaRecorderProvider.stop()");
            return false;
        }
        return true;
    }

    public interface StartCallback {
        void onResult(boolean succeeded);
    }

}
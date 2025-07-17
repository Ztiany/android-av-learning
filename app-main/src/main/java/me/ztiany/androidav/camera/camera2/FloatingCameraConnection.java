package me.ztiany.androidav.camera.camera2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import me.ztiany.lib.avbase.camera.camera2.VideoSpec;
import timber.log.Timber;

public class FloatingCameraConnection {

    private static final String ACTION_FLOATING_CAPTURING_OP = "ACTION_FLOATING_CAPTURING_OP";

    /**
     * true means start and false means stop.
     */
    private static final String KEY_FOR_CAPTURING_OP = "KEY_FOR_CAPTURING_OP";

    private static final String KEY_FOR_CAPTURING_SPEC = "KEY_FOR_CAPTURING_SPEC";

    private static final String KEY_FOR_SESSION = "KEY_FOR_SESSION";

    private static final String ACTION_FLOATING_CAPTURING_EVENT = "ACTION_FLOATING_CAPTURING_EVENT";

    /**
     * refers to {@link CapturerEvent}.
     */
    private static final String KEY_FOR_CAPTURING_EVENT = "KEY_FOR_CAPTURING_EVENT";

    public static Capturer newCapturer(Context context) {
        return new Capturer(context);
    }

    public static Commander newCommander(Context context) {
        return new Commander(context);
    }

    private FloatingCameraConnection() {
    }

    public interface CapturingActionListener {

        void startCapturing(String sessionId, VideoSpec videoSpec);

        void stopCapturing(String sessionId);

    }

    public interface CapturingStateListener {
        void onCapturingStarted(String sessionId);

        void onCapturingFinished(String sessionId, boolean succeeded);
    }

    public static class Capturer {

        private final Context mContext;

        @Nullable
        private CapturingActionListener mCapturingActionListener;

        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                dealWithNewAction(intent);
            }

        };

        public Capturer(Context context) {
            mContext = context;
        }

        public void init() {
            LocalBroadcastManager.getInstance(mContext).registerReceiver(
                    mReceiver,
                    new IntentFilter(ACTION_FLOATING_CAPTURING_OP)
            );
        }

        public void destroy() {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        }

        public void setCapturingActionListener(@Nullable CapturingActionListener capturingActionListener) {
            mCapturingActionListener = capturingActionListener;
        }

        private void dealWithNewAction(Intent intent) {
            if (mCapturingActionListener == null) {
                return;
            }

            if (!ACTION_FLOATING_CAPTURING_OP.equals(intent.getAction())) {
                return;
            }

            if (!intent.hasExtra(KEY_FOR_CAPTURING_OP)) {
                return;
            }

            String sessionId = intent.getStringExtra(KEY_FOR_SESSION);
            if (TextUtils.isEmpty(sessionId)) {
                Timber.w("sessionId is empty!");
                return;
            }

            boolean startCapturing = intent.getBooleanExtra(KEY_FOR_CAPTURING_OP, false);

            if (!startCapturing) {
                mCapturingActionListener.stopCapturing(sessionId);
                return;
            }

            VideoSpec videoSpec = intent.getParcelableExtra(KEY_FOR_CAPTURING_SPEC);
            if (videoSpec == null) {
                Timber.w("videoSpec is null!");
                return;
            }
            mCapturingActionListener.startCapturing(sessionId, videoSpec);
        }

        public void notifyCapturerEvent(String sessionId, @CapturerEvent int event) {
            Intent intent = new Intent(ACTION_FLOATING_CAPTURING_EVENT);
            intent.putExtra(KEY_FOR_CAPTURING_EVENT, event);
            intent.putExtra(KEY_FOR_SESSION, sessionId);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }

    }

    public static class Commander {

        private final Context mContext;

        @Nullable
        private CapturingStateListener mCapturingStateListener;


        private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                dealWithNewAction(intent);
            }

        };

        public Commander(Context context) {
            mContext = context;
        }

        public void startCapturing(String sessionId, VideoSpec videoSpec) {
            Intent intent = new Intent(ACTION_FLOATING_CAPTURING_OP);
            intent.putExtra(KEY_FOR_CAPTURING_OP, true);
            intent.putExtra(KEY_FOR_SESSION, sessionId);
            intent.putExtra(KEY_FOR_CAPTURING_SPEC, videoSpec);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }

        public void stopCapturing(String sessionId) {
            Intent intent = new Intent(ACTION_FLOATING_CAPTURING_OP);
            intent.putExtra(KEY_FOR_CAPTURING_OP, false);
            intent.putExtra(KEY_FOR_SESSION, sessionId);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }

        public void setCapturingStateListener(@Nullable CapturingStateListener capturingStateListener) {
            mCapturingStateListener = capturingStateListener;
        }

        public void init() {
            LocalBroadcastManager.getInstance(mContext).registerReceiver(
                    mBroadcastReceiver,
                    new IntentFilter(ACTION_FLOATING_CAPTURING_EVENT)
            );
        }

        public void destroy() {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiver);
        }

        private void dealWithNewAction(Intent intent) {
            if (mCapturingStateListener == null) {
                return;
            }

            if (!ACTION_FLOATING_CAPTURING_EVENT.equals(intent.getAction())) {
                return;
            }

            String sessionId = intent.getStringExtra(KEY_FOR_SESSION);
            if (TextUtils.isEmpty(sessionId)) {
                Timber.w("sessionId is empty!");
                return;
            }

            int event = intent.getIntExtra(KEY_FOR_CAPTURING_EVENT, -1);

            if (event == CapturerEvent.STARTED) {
                mCapturingStateListener.onCapturingStarted(sessionId);
                return;
            }

            if (event == CapturerEvent.STOPPED) {
                mCapturingStateListener.onCapturingFinished(sessionId, true);
                return;
            }

            if (event == CapturerEvent.ERROR) {
                mCapturingStateListener.onCapturingFinished(sessionId, false);
            }
        }

    }

}
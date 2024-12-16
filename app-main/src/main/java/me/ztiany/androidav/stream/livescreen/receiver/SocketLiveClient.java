package me.ztiany.androidav.stream.livescreen.receiver;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

import timber.log.Timber;

public class SocketLiveClient {

    private final SocketCallback socketCallback;
    private DemoWebSocketClient mDemoWebSocketClient;
    private final String serverAddress;

    public SocketLiveClient(SocketCallback socketCallback, String serverAddress) {
        this.socketCallback = socketCallback;
        this.serverAddress = serverAddress;
    }

    public void start() {
        try {
            URI url = new URI(serverAddress);
            mDemoWebSocketClient = new DemoWebSocketClient(url);
            mDemoWebSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class DemoWebSocketClient extends WebSocketClient {

        public DemoWebSocketClient(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            Timber.i("onOpen");
        }

        @Override
        public void onMessage(String s) {
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            socketCallback.callBack(buf);
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            Timber.i("onClose");
        }

        @Override
        public void onError(Exception e) {
            Timber.i("onError: %s", e.toString());
        }
    }

    public interface SocketCallback {
        void callBack(byte[] data);
    }

    public void stop() {
        mDemoWebSocketClient.close();
    }

}

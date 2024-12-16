package me.ztiany.androidav.stream.livecamera.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

import me.ztiany.androidav.stream.SocketLive;

public class SocketLiveClient implements SocketLive {

    private final String mServerAddress;

    private final SocketCallback socketCallback;
    private DemoWebSocketClient mDemoWebSocketClient;

    public SocketLiveClient(SocketCallback socketCallback, String serverAddress) {
        this.socketCallback = socketCallback;
        this.mServerAddress = serverAddress;
    }

    @Override
    public void start() {
        try {
            URI url = new URI(mServerAddress);
            mDemoWebSocketClient = new DemoWebSocketClient(url);
            mDemoWebSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        mDemoWebSocketClient.close();
    }

    @Override
    public void sendData(byte[] bytes) {
        if (mDemoWebSocketClient != null && (mDemoWebSocketClient.isOpen())) {
            mDemoWebSocketClient.send(bytes);
        }
    }

    private class DemoWebSocketClient extends WebSocketClient {

        public DemoWebSocketClient(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {

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

        }

        @Override
        public void onError(Exception e) {

        }
    }

}
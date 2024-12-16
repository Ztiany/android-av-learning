package me.ztiany.androidav.stream.livescreen.sender;

import android.media.projection.MediaProjection;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

import timber.log.Timber;

public class SocketLiveServer {

    private WebSocket webSocket;
    private final WebSocketServer webSocketServer;

    public SocketLiveServer(int port) {
        webSocketServer = new WebSocketServer(new InetSocketAddress(port)) {
            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                SocketLiveServer.this.webSocket = webSocket;
            }

            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
                Timber.i("onClose");
            }

            @Override
            public void onMessage(WebSocket webSocket, String s) {
            }

            @Override
            public void onError(WebSocket webSocket, Exception e) {
                Timber.i("onError: %s", e.toString());
            }

            @Override
            public void onStart() {

            }
        };
    }

    public void start(MediaProjection mediaProjection) {
        webSocketServer.start();
        CodecLiveH265 codecLiveH265 = new CodecLiveH265(this, mediaProjection);
        codecLiveH265.startLive();
    }

    public void close() {
        try {
            webSocketServer.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendData(byte[] bytes) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(bytes);
        }
    }

}

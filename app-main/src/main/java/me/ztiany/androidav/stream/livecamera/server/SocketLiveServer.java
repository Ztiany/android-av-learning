package me.ztiany.androidav.stream.livecamera.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import me.ztiany.androidav.stream.SocketLive;

public class SocketLiveServer implements SocketLive {

    private WebSocket webSocket;
    private final WebSocketServer webSocketServer;

    public SocketLiveServer(SocketLive.SocketCallback socketCallback, int port) {
        webSocketServer = new WebSocketServer(new InetSocketAddress(port)) {

            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                SocketLiveServer.this.webSocket = webSocket;
            }

            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {

            }

            @Override
            public void onMessage(WebSocket webSocket, String s) {

            }

            @Override
            public void onMessage(WebSocket conn, ByteBuffer bytes) {
                byte[] buf = new byte[bytes.remaining()];
                bytes.get(buf);
                socketCallback.callBack(buf);
            }

            @Override
            public void onError(WebSocket webSocket, Exception e) {
            }

            @Override
            public void onStart() {

            }
        };
    }

    @Override
    public void start() {
        webSocketServer.start();
    }

    @Override
    public void close() {
        try {
            webSocketServer.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendData(byte[] bytes) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(bytes);
        }
    }

}
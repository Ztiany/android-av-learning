package me.ztiany.androidav.stream;

public interface SocketLive {

    void start();

    void close();

    void sendData(byte[] bytes);

    interface SocketCallback {
        void callBack(byte[] data);
    }

}

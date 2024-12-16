package me.ztiany.androidav.stream.livescreen;

public class Constants {

    public static final int WIDTH = 720;
    public static final int HEIGHT = 1280;
    public static final int KEY_BIT_RATE = WIDTH * HEIGHT * 2;
    public static final int KEY_FRAME_RATE = 20;
    public static final int KEY_I_FRAME_INTERVAL = 1;

    public static final int SERVER_PORT = 12001;
    public static final String SERVER_ADDRESS = "ws://192.168.11.52:" + SERVER_PORT;

}

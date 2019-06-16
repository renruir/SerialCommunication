package com.ctftek.serialcommunication;

public interface UpdateInfo {

    void updateServerInfo(boolean isStart);

    void updateConnectInfo(String info);

    void updateCommandInfo(byte[] command);

    void updateState();

    void sendData2Serial(String data);
}

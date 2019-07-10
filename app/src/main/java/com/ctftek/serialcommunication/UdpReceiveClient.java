package com.ctftek.serialcommunication;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class UdpReceiveClient {

    private final static String TAG = UdpReceiveClient.class.getName();
    private UDPReceiverThread udpReceiverThread = null;
    private DatagramSocket dp;
    private String recCommand;
    private int mPort;
    private UDPReceiveService.DataCallBack mDataCallBack;

    public UdpReceiveClient(int port, UDPReceiveService.DataCallBack dataCallBack) {
        mPort = port;
        mDataCallBack = dataCallBack;
    }

    public void startUDPReceiver() {
        if (udpReceiverThread != null) {
            return;
        }
        udpReceiverThread = new UDPReceiverThread();
        udpReceiverThread.start();
        Log.d(TAG, "startUDPReceiver success");
    }

    private class UDPReceiverThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    if (dp == null) {
                        dp = new DatagramSocket(null);
                        dp.setReuseAddress(true);
                        dp.bind(new InetSocketAddress(mPort));
                    }
                    byte[] buffer = new byte[6];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    dp.receive(packet);
                    recCommand = bytesHexString(buffer);
                    Log.i(TAG, "收到命令: " + recCommand);
                    Log.d(TAG, "mDataCallBack: "+mDataCallBack);
                    mDataCallBack.updateCommandInfo(buffer);
//                    mDataCallBack.sendData2Serial(recCommand);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static String bytesHexString(byte[] b) {
        String ret = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }


}

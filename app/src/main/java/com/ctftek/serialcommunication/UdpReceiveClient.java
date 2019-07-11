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
    private UpdateInfo mDataCallBack;

    public UdpReceiveClient(int port, UpdateInfo dataCallBack) {
        mPort = port;
        mDataCallBack = dataCallBack;
        Log.d(TAG, "mDataCallBack: " + mDataCallBack);
    }

    public void startUDPReceiver() {
        if (udpReceiverThread != null) {
            return;
        }
        udpReceiverThread = new UDPReceiverThread();
        udpReceiverThread.start();
        mDataCallBack.updateServerInfo(true);
        Log.d(TAG, "startUDPReceiver success");
    }

    public void stopUDPReceiver() {
        if (udpReceiverThread.isAlive()) {
            udpReceiverThread.interrupt();
            mDataCallBack.updateState();
        }
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
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    dp.receive(packet);
                    String result = new String(packet.getData(), packet.getOffset(), packet.getLength());
                    recCommand = DataUtils.bytesHexString(subBytes(packet.getData(), 0, packet.getLength()));
                    mDataCallBack.updateCommandInfo(subBytes(packet.getData(), 0, packet.getLength()));
                    mDataCallBack.sendData2Serial(recCommand);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

}

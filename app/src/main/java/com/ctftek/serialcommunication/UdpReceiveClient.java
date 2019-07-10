package com.ctftek.serialcommunication;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class UdpReceiveClient {

    private final static String TAG = UdpReceiveClient.class.getName();
    private UDPReceiverThread udpReceiverThread = null;
    private DatagramSocket dp;

    public UdpReceiveClient(){

    }

    public void startUDPReceiver(){
        if(udpReceiverThread != null){
            return;
        }
        udpReceiverThread = new UDPReceiverThread();
        udpReceiverThread.start();
    }

    private class UDPReceiverThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    int port = 60001;
                    if(dp==null){
                        dp = new DatagramSocket(null);
                        dp.setReuseAddress(true);
                        dp.bind(new InetSocketAddress(port));
                    }
                    byte[] by = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(by,by.length);
                    dp.receive(packet);
                    String str = new String(packet.getData(), 0,packet.getLength());
                    System.out.println("接收到的数据为：" + str);
                    Log.v("WANGRUI", "已获取服务器端发过来的数据。。。。。"+str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

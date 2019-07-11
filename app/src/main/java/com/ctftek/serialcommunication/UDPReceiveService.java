package com.ctftek.serialcommunication;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


public class UDPReceiveService extends Service {
    private final static String TAG = UDPReceiveService.class.getName();
    private IBinder myBinder;
    private int port;
    UdpReceiveClient udpReceiveClient;
    private UpdateInfo dataCallBack;

    @Override
    public void onCreate() {
        super.onCreate();
        myBinder = new MyBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        port = intent.getIntExtra("port", 65535);
        Log.d(TAG, "onBind port: "+port);
        return myBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private void startUDPReceiver(int p, UpdateInfo callBack) {
        udpReceiveClient = new UdpReceiveClient(p, callBack);
        udpReceiveClient.startUDPReceiver();
    }

    public class MyBinder extends Binder {
        public UDPReceiveService getService() {
            Log.d(TAG, "onstart");
            return UDPReceiveService.this;
        }
    }

    public void setCallBack(UpdateInfo mCallback){
        dataCallBack = mCallback;
        startUDPReceiver(port, dataCallBack);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        udpReceiveClient.stopUDPReceiver();
        return super.onUnbind(intent);
    }

}

package com.ctftek.serialcommunication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android_serialport_api.SerialPortFinder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, UpdateInfo {
    private final static String TAG = MainActivity.class.getName();

    private final static int DEFAULT_DEV_SERIAL = 6;

    private Application mApplication;
    private Button setupServiceBtn;
    private Button stopServiceBtn;
    private EditText mPort;
    private TextView myIpdress;
    private TextView mConnectInfo;
    private TextView mRecInfo;
    private Spinner mBaudrateSpinner;
    private String[] baudRates;
    private int baudRate;
    private Spinner mDevDevicesSpinner;
    private String[] devDevices;
    private String[] devDevicesPath;
    private String devDevice;

    private SharedPreferences sp;

    private SerialPortUtil serialPortUtil;
    private SerialPortFinder mSerialPortFinder;

    private UDPReceiveService udpReceiveService;
    private boolean mBind = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "handleMessage: " + msg.what);
            if (msg.what == 0) {
                setupServiceBtn.setText(msg.getData().getString("isStart"));
            } else if (msg.what == 1) {
                mConnectInfo.setText(msg.getData().getString("connectInfo"));
            } else if (msg.what == 2) {
                mRecInfo.setText(msg.getData().getString("commandInfo"));
            } else {
                setupServiceBtn.setText("启动服务");
                mConnectInfo.setText("未发现连接设备");
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sp = getSharedPreferences("app_info", Context.MODE_PRIVATE);
        String historyPort = sp.getString("port", "0");

        mApplication = (Application) getApplication();
        mSerialPortFinder = mApplication.mSerialPortFinder;
        EventBus.getDefault().register(this);

        setContentView(R.layout.activity_main);
        setupServiceBtn = (Button) findViewById(R.id.setup_ss);
        stopServiceBtn = (Button) findViewById(R.id.stop_ss);
        setupServiceBtn.setOnClickListener(this);
        stopServiceBtn.setOnClickListener(this);
        mPort = (EditText) findViewById(R.id.port);
        mPort.setText(historyPort);
        myIpdress = (TextView) findViewById(R.id.ip_address);
        mConnectInfo = (TextView) findViewById(R.id.connected_devices);
        mRecInfo = (TextView) findViewById(R.id.rec_info);
        mBaudrateSpinner = (Spinner) findViewById(R.id.baud_rate_spinner);
        mDevDevicesSpinner = (Spinner) findViewById(R.id.dev_devices);

        myIpdress.setText(getIPAddress(this) + "");
        baudRates = getResources().getStringArray(R.array.baud_rate_spinner_values);
        mBaudrateSpinner.setSelection(1, true);

        devDevices = mSerialPortFinder.getAllDevices();
        devDevicesPath = mSerialPortFinder.getAllDevicesPath();

        ArrayAdapter devicesArrayAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, devDevices);
        mDevDevicesSpinner.setAdapter(devicesArrayAdapter);
        if (devDevicesPath.length != 0) {
            mDevDevicesSpinner.setSelection(DEFAULT_DEV_SERIAL, true);
            devDevice = devDevicesPath[DEFAULT_DEV_SERIAL];
        }

        mDevDevicesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                devDevice = devDevicesPath[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mBaudrateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "onItemSelected: " + baudRates[i]);
                baudRate = Integer.parseInt(baudRates[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if (!historyPort.isEmpty() && !historyPort.equals("0")) {
            startUDPRecService();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.setup_ss:
                startUDPRecService();
                break;
            case R.id.stop_ss:
                unbindService(mConnection);
                mPort.setFocusableInTouchMode(true);//服务停止，端口号可编辑
                mPort.setFocusable(true);
                mPort.requestFocus();
                setupServiceBtn.setEnabled(true);
                break;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: ");
            UDPReceiveService.MyBinder binder = (UDPReceiveService.MyBinder) service;
            udpReceiveService = binder.getService();
            mBind = true;
            udpReceiveService.setCallBack(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBind = false;
        }
    };

    private void startUDPRecService() {
        try {
            if (mPort.getText().toString().isEmpty()) {
                Toast.makeText(this, "请输入端口号", Toast.LENGTH_LONG).show();
                return;
            }
            int port = Integer.parseInt(mPort.getText().toString());
            if (!(port > 1024 && port < 65535)) {
                Toast.makeText(this, "端口号应大于1024，小于65535", Toast.LENGTH_LONG).show();
                return;
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("port", mPort.getText().toString());
            editor.commit();
            mPort.setFocusable(false);//服务已启动，设置不可编辑端口号
            mPort.setFocusableInTouchMode(false);
            setupServiceBtn.setEnabled(false);//服务已启动，该按钮不可用

            Intent intent = new Intent(this, UDPReceiveService.class);
            intent.putExtra("port", port);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getIPAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getSubtype() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                try {
                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();

                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();

                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getSubtype() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());//得到IPV4地址
                return ipAddress;
            }
        } else {
            Log.d(TAG, "getIPAddress: 网络未打开");
            return "请开启网络";
        }
        return null;
    }

    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }


    @Override
    public void updateServerInfo(boolean isStart) {
        Message msg = new Message();
        msg.what = 0;
        Bundle bundle = new Bundle();
        if (isStart) {
            bundle.putString("isStart", "已开启");
        }
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    @Override
    public void updateConnectInfo(String info) {
        Message msg = new Message();
        msg.what = 1;
        Bundle bundle = new Bundle();
        bundle.putString("connectInfo", info);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    @Override
    public void updateCommandInfo(byte[] command) {
        Message msg = new Message();
        msg.what = 2;
        Bundle bundle = new Bundle();
        bundle.putString("commandInfo", DataUtils.bytesHexString(command));
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    @Override
    public void updateState() {
        Message msg = new Message();
        msg.what = 3;
        mHandler.sendMessage(msg);
    }

    @Override
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void sendData2Serial(String data) {
        serialPortUtil = new SerialPortUtil();
        serialPortUtil.openSerialPort(devDevice, baudRate);
        serialPortUtil.sendSerialPort(data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        ClientManager.shutDown();
        unbindService(mConnection);

    }
}
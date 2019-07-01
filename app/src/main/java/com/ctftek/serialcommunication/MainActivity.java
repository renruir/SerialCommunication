package com.ctftek.serialcommunication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
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

    private SerialPortUtil serialPortUtil;
    private SerialPortFinder mSerialPortFinder;


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

        mApplication = (Application) getApplication();
        mSerialPortFinder = mApplication.mSerialPortFinder;

        setContentView(R.layout.activity_main);
        setupServiceBtn = (Button) findViewById(R.id.setup_ss);
        stopServiceBtn = (Button) findViewById(R.id.stop_ss);
        setupServiceBtn.setOnClickListener(this);
        stopServiceBtn.setOnClickListener(this);
        mPort = (EditText) findViewById(R.id.port);
        myIpdress = (TextView) findViewById(R.id.ip_address);
        mConnectInfo = (TextView) findViewById(R.id.connected_devices);
        mRecInfo = (TextView) findViewById(R.id.rec_info);
        mBaudrateSpinner = (Spinner) findViewById(R.id.baud_rate_spinner);
        mDevDevicesSpinner = (Spinner)findViewById(R.id.dev_devices);

        myIpdress.setText(getIPAddress(this) + "");
        baudRates = getResources().getStringArray(R.array.baud_rate_spinner_values);
        mBaudrateSpinner.setSelection(1,true);

        devDevices = mSerialPortFinder.getAllDevices();
        devDevicesPath = mSerialPortFinder.getAllDevicesPath();

        ArrayAdapter devicesArrayAdapter = new ArrayAdapter<CharSequence>(this,android.R.layout.simple_spinner_item, devDevices);
        mDevDevicesSpinner.setAdapter(devicesArrayAdapter);
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
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.setup_ss:
                if (mPort.getText().toString().isEmpty()) {
                    Toast.makeText(this, "请输入端口号", Toast.LENGTH_LONG).show();
                    return;
                }
                int port = Integer.parseInt(mPort.getText().toString());
                if (!(port > 1024 && port < 65535)) {
                    Toast.makeText(this, "端口号应大于1024，小于65535", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    ClientManager.startServer(port, this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.stop_ss:
                ClientManager.shutDown();
                break;
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


    public void doSomething(String ret) {
        Message msg = Message.obtain();
        msg.obj = (Object) ret;
        mHandler.sendMessage(msg);

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
            ret += hex.toUpperCase() + " ";
        }
        return ret;
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
        bundle.putString("commandInfo", bytesHexString(command));
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
        EventBus.getDefault().register(this);
//        EventBus.getDefault().unregister(this);
        serialPortUtil.sendSerialPort(data);
    }


}
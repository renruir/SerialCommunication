package com.ctftek.serialcommunication;

import android.text.style.UpdateAppearance;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientManager {

    private static final String TAG = "ClientManager";

    private static ServerThread serverThread = null;
    private static int sum = 0;
    private static Map<String, Socket> clientMap = new HashMap<>();
    private static List<String> clientList = new ArrayList<>();

    private static UpdateInfo updateInfo;

    private static class ServerThread implements Runnable {

        private ServerSocket server;
        private boolean isExit = false;// 一个boolean类型的判断 默认是退出状态false

        // 构造方法初始化
        public ServerThread(int port) {
            try {
                server = new ServerSocket(port);
                Log.d(TAG, "启动server，端口号：" + port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * 1.获得远程服务器的IP 地址.
         * InetAddress inetAddress = socket.getInetAddress();
         * 2.获得远程服务器的端口.
         * int port = socket.getPort();
         * 3. 获得客户本地的IP 地址.
         * InetAddress localAddress = socket.getLocalAddress();
         * 4.获得客户本地的端口.
         * int localPort = socket.getLocalPort();
         * 5.获取本地的地址和端口号
         * SocketAddress localSocketAddress = socket.getLocalSocketAddress();
         * 6.获得远程的地址和端口号
         * SocketAddress remoteSocketAddress = socket.getRemoteSocketAddress();
         */
        @Override
        public void run() {
            try {
                while (!isExit) {
                    // 等待连接
                    Log.d(TAG, "server: " + server);
                    final Socket socket = server.accept();
                    Log.d(TAG, "连接设备的IP地址及端口号：" + socket.getRemoteSocketAddress().toString());
                    updateInfo.updateConnectInfo(socket.getRemoteSocketAddress().toString());
                    /**
                     * 因为考虑到多手机连接的情况 所以加入线程锁 只允许单线程工作
                     */
                    new Thread(new Runnable() {

                        private String text;

                        @Override
                        public void run() {
                            try {
                                synchronized (this) {
                                    // 在这里考虑到线程总数的计算 也代表着连接手机的数量
                                    ++sum;
                                    // 存入到集合和Map中为群发和单独发送做准备
                                    String string = socket.getRemoteSocketAddress().toString();
                                    clientList.add(string);
                                    clientMap.put(string, socket);
                                }

                                // 定义输入输出流
                                InputStream is = socket.getInputStream();
                                OutputStream os = socket.getOutputStream();

                                // 接下来考虑输入流的读取显示到PC端和返回是否收到
                                byte[] buffer = new byte[6];
                                int len;
                                while ((len = is.read(buffer)) != -1) {
                                    text = bytesHexString(buffer);
                                    Log.d(TAG, "收到的数据为：" + text);
//                                    os.write("已收到消息".getBytes("utf-8"));\
                                    updateInfo.updateCommandInfo(buffer);
                                    updateInfo.sendData2Serial(text);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                Log.d(TAG, "关闭连接：" + socket.getRemoteSocketAddress().toString());
                                synchronized (this) {
                                    --sum;
                                    String string = socket.getRemoteSocketAddress().toString();
                                    clientMap.remove(string);
                                    clientList.remove(string);
                                }
                            }
                        }
                    }).start();

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 关闭server
        public void stop() {
            isExit = true;
            if (server != null) {
                try {
                    server.close();
                    Log.d(TAG, "已关闭server");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    // 启动server
    public static ServerThread startServer(int port, UpdateInfo mUpdateInfo) throws Exception {
        Log.d(TAG, "开启server");
        if (mUpdateInfo != null) {
            updateInfo = mUpdateInfo;
        } else {
            throw new Exception("开启服务失败");
        }
        if (serverThread != null) {
            Log.d(TAG, "server不为null正在重启server");
            // 以下为关闭server和socket
            shutDown();
        }
        // 初始化
        serverThread = new ServerThread(port);
        new Thread(serverThread).start();
        Log.d(TAG, "开启server成功");
        updateInfo.updateServerInfo(true);
        return serverThread;
    }

    public static boolean isStart(){
        if(clientList.isEmpty()){
            return false;
        } else {
            return true;
        }
    }


    // 发送消息的方法
    public static boolean sendMessage(String name, String mag) {
        try {
            Socket socket = clientMap.get(name);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(mag.getBytes("utf-8"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 群发的方法
    public static boolean sendMsgAll(String msg) {
        try {
            for (Socket socket : clientMap.values()) {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(msg.getBytes("utf-8"));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 获取线程总数的方法，也等同于<获取已连接了多少台手机>的方法+
    public static int sumTotal() {
        return sum;
    }

    // 一个获取list集合的方法，取到所有连接server的手机的ip和端口号的集合
    public static List<String> getTotalClients() {
        return clientList;
    }

    public static void shutDown() {
        for (Socket socket : clientMap.values()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        serverThread.stop();
        clientMap.clear();
        clientList.clear();
        updateInfo.updateState();
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
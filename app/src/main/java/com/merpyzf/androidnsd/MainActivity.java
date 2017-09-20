package com.merpyzf.androidnsd;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Android的网络服务发现
 * <p>
 * 如何创建NSD应用，使其能够在本地网络内广播子的名称和连接信息
 * 并扫描其它正在做同样事情的应用信息。
 */
public class MainActivity extends AppCompatActivity {

    private Button btn_register;
    private Button btn_scan;
    private TextView tv_scan_result;
    private static final String TAG = "wk";
    private NsdManager.ResolveListener mResolveListener;
    private NsdServiceInfo mService;
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.RegistrationListener mRegistrationListener;
    private Handler mHandler  = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {


            Toast.makeText(getApplicationContext(),(String)message.obj,Toast.LENGTH_SHORT).show();




            return false;
        }
    });

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_register = (Button) findViewById(R.id.btn_register);
        btn_scan = (Button) findViewById(R.id.btn_scan);
        tv_scan_result = (TextView) findViewById(R.id.tv_scan_result);

        startServer();

        // 获取系统网络服务管理器，准备之后进行注册
        mNsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //注册服务
                registerService(6666);

            }
        });


        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //发现局域网下的服务
                discoverService();

            }
        });


    }

    /**
     * 第一步:
     * 注册NSD服务，如果不打算在本地网络上广播app服务
     *
     * @param port 本程序的端口号
     */
    private void registerService(int port) {


        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        //设置服务名为NsdChat,并且该名称将对局域网中使用NSD查找本地服务的设备可见
        serviceInfo.setServiceName("merpyzf-transfer");
        //设置服务类型，指定使用的协议和传输层。 语法是_<protocol>._<transportlayer>
        serviceInfo.setServiceType("_http._tcp.");
        serviceInfo.setPort(port);


        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
                Log.i(TAG, "==> onRegistrationFailed");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {


                Log.i(TAG, "==> onUnregistrationFailed");

            }

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {

                String mServiceName = nsdServiceInfo.getServiceName();
                int port = nsdServiceInfo.getPort();
                Log.i(TAG, "==> onServiceRegistered " + mServiceName + port);


            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {

                Log.i(TAG, "==> onServiceUnregistered");

            }
        };


        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    }


    /**
     * 发现网络服务
     */
    private void discoverService() {


        // 发现网络服务时就会触发该事件
        // 可以通过switch或if获取那些你真正关心的服务
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Toast.makeText(getApplicationContext(), "Stop Discovery Failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Toast.makeText(getApplicationContext(),
                        "Start Discovery Failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Toast.makeText(getApplicationContext(), "Service Lost", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {


                Toast.makeText(getApplicationContext(), "Service Found", Toast.LENGTH_SHORT).show();

                //使用if获取自己关心的服务
                if (serviceInfo.getServiceName().contains("merpyzf")) {

                    mNsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {

                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {

                            String hostAddress = serviceInfo.getHost().getHostAddress();

                            int port = serviceInfo.getPort();

                            Toast.makeText(getApplicationContext(), hostAddress + ":" + port, Toast.LENGTH_SHORT).show();

                            startClient(hostAddress, port);

                        }
                    });
                }


            }


            @Override
            public void onDiscoveryStopped(String serviceType) {
                Toast.makeText(getApplicationContext(), "Discovery Stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Toast.makeText(getApplicationContext(), "Discovery Started", Toast.LENGTH_SHORT).show();
            }
        };


        //注册网络发现服务
        mNsdManager.discoverServices("_http._tcp", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

    }


    /**
     * 开启一个socket的客户端连接
     *
     * @param hostAddress
     * @param port
     */
    public void startClient(final String hostAddress, final int port) {


        new Thread(new Runnable() {
            @Override
            public void run() {


                Socket socket = new Socket();
                try {
                    //连接
                    socket.connect(new InetSocketAddress(hostAddress, port));
                    OutputStream os = socket.getOutputStream();

                    os.write("Hello Server".getBytes());

                    os.flush();

                    byte[] buff = new byte[1024];
                    InputStream is = socket.getInputStream();

                    int lenth = is.read(buff);
                    Log.i(TAG, "lenth=====>" + lenth);
                    String msg = new String(buff, 0, lenth);

                    Log.i(TAG, msg);

                    Message message = Message.obtain();
                    message.obj = msg;
                    mHandler.sendMessage(message);

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                    try {
                        socket.close();
                        socket = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            }
        }).start();


    }


    /**
     * 开启一个socketServer的客户端连接
     */
    public void startServer() {


        new Thread(new Runnable() {
            @Override
            public void run() {

                Socket socket = null;
                ServerSocket serverSocket = null;
                try {
                    serverSocket = new ServerSocket(6666);

                    while (true) {


                        Log.i("wk", "serverSocket创建成功,等待连接。。。。");

                        socket = serverSocket.accept();

                        Log.i("wk", "有设备连接: " + socket.getInetAddress().getHostAddress());
                        final Socket finalSocket = socket;


                        Message message = Message.obtain();
                        message.obj = "有设备连接: " + socket.getInetAddress().getHostAddress();
                        mHandler.sendMessage(message);



                        byte[] buff = new byte[1024];

                        int length = socket.getInputStream().read(buff, 0, buff.length);

                        Log.i(TAG, "收到的信息->" + new String(buff, 0, length));

                        Message message1 = Message.obtain();
                        message1.obj = "收到的信息->" + new String(buff, 0, length);
                        mHandler.sendMessage(message1);



                        OutputStream os = socket.getOutputStream();

                        os.write("Hello,Client".getBytes());
                        os.flush();

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                    if (socket != null) {

                        try {
                            socket.close();
                            socket = null;

                            serverSocket.close();
                            serverSocket = null;

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                }


            }
        }).start();


    }


}

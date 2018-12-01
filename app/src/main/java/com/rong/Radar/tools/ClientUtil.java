package com.rong.Radar.tools;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;


/**
 * 客户端(连接端)工具类
 */
public class ClientUtil {

    public static final String TAG = "BluetoothManagerUtil";
    private Handler mainHandler;

    ///////////////////////////////////////////////////////////////////////////
    // 单例模式
    private ClientUtil() {
        bufferQueue = new LinkedList<>();
        lock = new Object();
    }

    public static synchronized ClientUtil getInstance() {
        return SingletonHolder.instance;
    }

    private static final class SingletonHolder {
        private static ClientUtil instance = new ClientUtil();
    }
    ///////////////////////////////////////////////////////////////////////////

    private String serverBlueToothAddress;  //连接蓝牙地址
    private BluetoothSocket socket = null; // 客户端socket
    private BluetoothAdapter bluetoothAdapter;

//    /**
//     * 打开蓝牙,注册扫描蓝牙的广播 onCreate()中执行.连接页面调用
//     */
//    public void onCreate(Activity activity) {
//        registerBluetoothScanReceiver(activity);
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (null != bluetoothAdapter) { //本地蓝牙存在...
//            if (!bluetoothAdapter.isEnabled()) { //判断蓝牙是否被打开...
//                // 发送打开蓝牙的意图，系统会弹出一个提示对话框,打开蓝牙是需要传递intent的...
//                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                //打开本机的蓝牙功能...使用startActivityForResult（）方法...这里我们开启的这个Activity是需要它返回执行结果给主Activity的...
//                activity.startActivityForResult(enableIntent, Activity.RESULT_FIRST_USER);
//
//                Intent displayIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//                // 设置蓝牙的可见性，最大值3600秒，默认120秒，0表示永远可见(作为客户端，可见性可以不设置，服务端必须要设置)
//                displayIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
//                //这里只需要开启另一个activity，让其一直显示蓝牙...没必要把信息返回..因此调用startActivity()
//                activity.startActivity(displayIntent);
//
//                // 直接打开蓝牙
//                bluetoothAdapter.enable();//这步才是真正打开蓝牙的部分....
//                Log.d(TAG, "打开蓝牙成功");
//            } else {
//                Log.d(TAG, "蓝牙已经打开了...");
//            }
//        } else {
//            Log.d(TAG, "当前设备没有蓝牙模块");
//        }
//    }


//    /**
//     * 扫描设备 onResume()中执行.连接页面调用
//     */
//    public List<BluetoothDeviceInfo> scanDevice() {
//        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
//            LogUtil.e(TAG, "蓝牙状态异常");
//            return null;
//        }
//        List<BluetoothDeviceInfo> bluetoothDeviceInfoList = new ArrayList<>();
//        if (bluetoothAdapter.isDiscovering()) { // 如果正在处于扫描过程...
//            /** 停止扫描 */
//            bluetoothAdapter.cancelDiscovery(); // 取消扫描...
//        } else {
//            // 每次扫描前都先判断一下是否存在已经配对过的设备
//            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
//            if (pairedDevices.size() > 0) {
//                BluetoothDeviceInfo bluetoothDeviceInfo;
//                for (BluetoothDevice device : pairedDevices) {
//                    bluetoothDeviceInfo = new BluetoothDeviceInfo(device.getName() + "", device.getAddress() + "");
//                    bluetoothDeviceInfoList.add(bluetoothDeviceInfo);
//                    LogUtil.d(TAG, "已经匹配过的设备:" + bluetoothDeviceInfo.toString());
//                }
//            } else {
//                LogUtil.d(TAG, "没有已经配对过的设备");
//            }
//            /* 开始搜索 */
//            bluetoothAdapter.startDiscovery();
//        }
//        return bluetoothDeviceInfoList;
//    }

    /**
     * 通过Mac地址去尝试连接一个设备.连接页面调用
     */
    public void connectRemoteDevice(final String serverBlueToothAddress, BlueToothConnectCallback connectInterface) {
        this.serverBlueToothAddress = serverBlueToothAddress;
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(serverBlueToothAddress);
//        ThreadPoolUtil.execute(new ConnectRunnable(device, connectInterface));
        new Thread(new ConnectRunnable(device, connectInterface)).start();
    }

//    /**
//     * 广播反注册.连接页面调用
//     */
//    public void unregisterReceiver(Activity activity) {
//        if (receiver != null && receiver.getAbortBroadcast()) {
//            activity.unregisterReceiver(receiver);
//        }
//    }

    /**
     * 发送消息,在通信页面使用
     */
    public void sendMessage(String message) {
        try {
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init(Handler handler, BluetoothAdapter bluetoothAdapter) {
        this.mainHandler = handler;
        this.bluetoothAdapter = bluetoothAdapter;
    }

    private ReadThread readThread;

    /**
     * 收到消息的监听事件,在通信页面注册这个事件
     */
    public void setOnReceivedMessageListener(ReceivedMessageListener listener) {
        if (listener != null) {
            // 可以开启读数据线程
            //     MainHandler.getInstance().post(new ReadRunnable(listener));
//            ThreadPoolUtil.execute(new ReadRunnable(listener));
//            new Thread(new ReadRunnable(listener)).start();
            stopReadMessage();
            if(scanBufferThread == null){
                scanBufferThread = new ScanBufferThread(listener);
                scanBufferThread.start();
            }
            if (readThread == null) {
                readThread = new ReadThread(listener);
                readThread.start();
            }
        }
    }

    public void stopReadMessage() {
        if (readThread != null) {
            readThread.stopRead();
            readThread = null;
        }
        if (scanBufferThread != null) {
            scanBufferThread.stopScan();
            scanBufferThread = null;
        }
    }

    /**
     * 关闭蓝牙,在app退出时调用
     */
    public void onExit() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
            // 关闭蓝牙
            bluetoothAdapter.disable();
            bluetoothAdapter = null;
        }
        closeCloseable(writer, socket);
    }

    public interface BlueToothConnectCallback {
        void connecting(String serverBlueToothAddress);

        void connectSuccess(String serverBlueToothAddress);

        void connectFailure(Exception e);
    }

    /**
     * 连接线程
     */
    class ConnectRunnable implements Runnable {
        private BluetoothDevice device; // 蓝牙设备
        private BlueToothConnectCallback connectInterface;


        public ConnectRunnable(BluetoothDevice device, BlueToothConnectCallback connectInterface) {
            this.device = device;
            this.connectInterface = connectInterface;
        }

        @Override
        public void run() {
            if (null != device) {
                try {
                    if (socket != null) {
                        closeCloseable(socket);
                    }
                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    // 连接
                    Log.d(TAG, "正在连接 " + serverBlueToothAddress);
                    connectInterface.connecting(serverBlueToothAddress);
//                    Message.obtain(handler, MESSAGE_TYPE_SEND, "请稍候，正在连接服务器: " + serverBlueToothAddress).sendToTarget();

                    socket.connect();
                    if (mainHandler != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                connectInterface.connectSuccess(serverBlueToothAddress);
                                Log.d(TAG, "连接 " + serverBlueToothAddress + " 成功 ");
                            }
                        });
                    }
                    // 如果实现了连接，那么服务端和客户端就共享一个RFFCOMM信道...
//                    Message.obtain(handler, MESSAGE_TYPE_SEND, "已经连接上服务端！可以发送信息").sendToTarget();
                    // 如果连接成功了...这步就会执行...更新UI界面...否则走catch（IOException e）
//                    Message.obtain(handler, MESSAGE_ID_REFRESH_UI).sendToTarget();

                    // 屏蔽点击事件
//                    listViewMessage.setOnItemClickListener(null);
                } catch (final IOException e) {
                    if (mainHandler != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                connectInterface.connectFailure(e);
                                Log.d(TAG, "连接" + serverBlueToothAddress + "失败 " + e.getMessage());
                            }
                        });
                    }
//                    e.printStackTrace();
                }
            }
        }
    }

    public interface ReceivedMessageListener {
        void onReceiveMessage(String finalContent);

        void onConnectionInterrupt(Exception e);
    }

    private BufferedWriter writer = null;
    private Queue<String> bufferQueue = null;
    private Object lock = null;
    private ScanBufferThread scanBufferThread;

    private class ScanBufferThread extends Thread{
        private boolean isRunnig = true;
        private boolean isFinish = true;
        private StringBuffer buffer = null;
        private ReceivedMessageListener listener;
        private static final  char startStr  = '[';
        private  static final  char endStr = ']';
        public ScanBufferThread(ReceivedMessageListener listener){
            this.listener = listener;
        }
        public void scanNotify(){
            if(this.getState() == State.WAITING){
                synchronized (this){
                    this.notify();
                }
            }
        }
        public void scanWait(){
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            super.run();
            while (isRunnig){
                if(bufferQueue != null && !bufferQueue.isEmpty() && isFinish){
                    scanBuffer();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else {
                    scanWait();
                }
            }
        }

        public void stopScan() {
            isRunnig = false;
            this.interrupt();
        }

        private void scanBuffer() {
            isFinish = false;
            synchronized (lock){
                final String s = bufferQueue.poll();
                final int length = s.length();
                for(int i = 0 ; i < length;i ++){
                    final char c = s.charAt(i);
                    if(startStr == c){
                        if(buffer != null){
                            buffer.reverse();
                            buffer = null;
                        }
                        buffer = new StringBuffer();
                        buffer.append(c);
                    }else if(endStr == c){
                        if(buffer != null){
                            buffer.append(c);
//                            if (mainHandler != null) {
//                                mainHandler.post(new Runnable() {
//                                    @Override
//                                    public void run() {
                                        listener.onReceiveMessage(buffer.toString());
                                        buffer.reverse();
                                        buffer = null;
//                                    }
//                                });
                            }
//                        }
                    }else if(buffer != null){
                        buffer.append(c);
                    }
                }
            }
            isFinish = true;
        }

    }



    class ReadThread extends Thread {
        private ReceivedMessageListener listener;
        boolean isReading = true;
        InputStream reader = null;
        byte[] buffer = null;

        public ReadThread(ReceivedMessageListener listener) {
            this.listener = listener;
        }

        public void run() {
            try {
                if (reader == null) {
                    reader = socket.getInputStream();
                }
                while (isReading) {
                    if (reader != null) {
                        while(reader.available()==0){
                        }
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] b = new byte[1024];
                        int n = reader.read(b);
                        buffer = new byte[n];
                        System.arraycopy(b,0,buffer,0,n);
//                        while ((n) != -1) {
//                            bos.write(b, 0, n);
//                        }
//                        bos.close();
//                        buffer = bos.toByteArray();
                        final String finalContent = new String(buffer);
                        synchronized (lock){
                            bufferQueue.add(finalContent);
                            if(scanBufferThread != null){
                                scanBufferThread.scanNotify();
                            }
                        }

                    }
                }
            } catch (final IOException e) {
                if (mainHandler != null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "连接中断 " + e.getMessage());
                            listener.onConnectionInterrupt(e);
                        }
                    });
                    closeCloseable(reader);
                }
                // 连接断开
//                Message.obtain(handler, MESSAGE_ID_DISCONNECT).sendToTarget();
            }
        }

        public void stopRead() {
            isReading = false;
            this.interrupt();
        }
    }

//    private BroadcastReceiver registerBluetoothScanReceiver(Activity activity) {
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        activity.registerReceiver(receiver, filter);
//        return receiver;
//    }

    public void setOnFoundUnBondDeviceListener(OnFoundUnBondDeviceListener onFoundUnBondDeviceListener) {
        this.onFoundUnBondDeviceListener = onFoundUnBondDeviceListener;
    }

    private OnFoundUnBondDeviceListener onFoundUnBondDeviceListener;

    public interface OnFoundUnBondDeviceListener {
        void foundUnBondDevice(BluetoothDevice unBondDevice);
    }

    private void closeCloseable(Closeable... closeable) {
        if (null != closeable && closeable.length > 0) {
            for (int i = 0; i < closeable.length; i++) {
                if (closeable[i] != null) {
                    try {
                        closeable[i].close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        closeable[i] = null;
                    }
                }
            }
        }
    }

//    /**
//     * 下面是注册receiver监听，注册广播...说一下为什么要注册广播...
//     * 因为蓝牙的通信，需要进行设备的搜索，搜索到设备后我们才能够实现连接..如果没有搜索，那还谈什么连接...
//     * 因此我们需要搜索，搜索的过程中系统会自动发出三个广播...这三个广播为：
//     * ACTION_DISCOVERY_START:开始搜索...
//     * ACTION_DISCOVERY_FINISH:搜索结束...
//     * ACTION_FOUND:正在搜索...一共三个过程...因为我们需要对这三个响应过程进行接收，然后实现一些功能，因此
//     * 我们需要对广播进行注册...知道广播的人应该都知道，想要对广播进行接收，必须进行注册，否则是接收不到的...
//     */
//    private BroadcastReceiver receiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {//正在搜索过程...
//                // 通过EXTRA_DEVICE附加域来得到一个BluetoothDevice设备
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                // 如果这个设备是不曾配对过的，添加到list列表
//                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//                    if (null != onFoundUnBondDeviceListener) {
//                        Log.d(TAG, "发现没有配对过的设备:" + parseDevice2BluetoothDeviceInfo(device));
//                        onFoundUnBondDeviceListener.foundUnBondDevice(device);
//                    }
//                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {//搜索结束后的过程...
//                    Log.d(TAG, "没有发现设备");
//                }
//            }
//        }
//    };

//    private String parseDevice2BluetoothDeviceInfo(BluetoothDevice device) {
//        if (device == null) {
//            return "device == null";
//        }
//        return new BluetoothDeviceInfo(device.getName(), device.getAddress()).toString();
//    }
}


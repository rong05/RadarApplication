package com.rong.Radar.tools;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.rong.Radar.MainActivity;
import com.rong.Radar.MainActivity2;
import com.rong.Radar.datas.ImageBuffer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import static com.rong.Radar.serivce.BluetoothLeService.UUID_SERVICE;


/**
 * 客户端(连接端)工具类
 */
public class ClientUtil {

    public static final String TAG = "BluetoothManagerUtil";
    private Handler mainHandler;
    private Context mContext;


    final UUID UUID_SERVICE = UUID.fromString("0003CDD2-0000-1000-8000-00805F9B0131");
    //
    //  设备特征值UUID, 需固件配合同时修改
    //
    final UUID UUID_WRITE = UUID.fromString("0003CDD2-0000-1000-8000-00805F9B0131");  // 用于发送数据到设备
    final UUID UUID_NOTIFICATION = UUID.fromString("0003CDD2-0000-1000-8000-00805F9B0131"); // 用于接收设备推送的数据

    ///////////////////////////////////////////////////////////////////////////
    // 单例模式
    private ClientUtil() {
        bufferQueue = new LinkedList<>();
        lock = new Object();
    }



    public static synchronized ClientUtil getInstance() {
        return SingletonHolder.instance;
    }

    public void setContext(Context context) {
        this.mContext = mContext;
    }

    private static final class SingletonHolder {
        private static ClientUtil instance = new ClientUtil();
    }
    ///////////////////////////////////////////////////////////////////////////

    private String serverBlueToothAddress;  //连接蓝牙地址
//    private BluetoothSocket socket = null; // 客户端socket
    private BluetoothAdapter bluetoothAdapter;

    /**
     * 通过Mac地址去尝试连接一个设备.连接页面调用
     */
    public void connectRemoteDevice(final String serverBlueToothAddress, BlueToothConnectCallback connectInterface) {
        this.serverBlueToothAddress = serverBlueToothAddress;
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(serverBlueToothAddress);
//        ThreadPoolUtil.execute(new ConnectRunnable(device, connectInterface));
        new Thread(new ConnectRunnable(device, connectInterface)).start();
    }

    /**
     * 发送消息,在通信页面使用
     */
    public void sendMessage(String message) {
//        try {
//            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//            writer.write(message + "\n");
//            writer.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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
        closeCloseable();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
            // 关闭蓝牙
            bluetoothAdapter.disable();
            bluetoothAdapter = null;
        }

    }

    public interface BlueToothConnectCallback {
        void connecting(String serverBlueToothAddress);

        void connectSuccess(String serverBlueToothAddress);

        void connectFailure(Exception e);
    }

    private BluetoothDevice mDevice; // 蓝牙设备

    private BlueToothConnectCallback mConnectInterface;
    /**
     * 连接线程
     */
    class ConnectRunnable implements Runnable {




        public ConnectRunnable(BluetoothDevice device, BlueToothConnectCallback connectInterface) {
            mDevice = device;
            mConnectInterface = connectInterface;
        }

        @Override
        public void run() {
            if (null != mDevice) {
//                try {
//                if (socket != null) {
                closeCloseable();
//                }

                mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallback);

//                    if(Build.VERSION.SDK_INT >= 10){
//                        try {
//                            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
//                            socket =  (BluetoothSocket) m.invoke(device, UUID.fromString("0003CDD2-0000-1000-8000-00805F9B0131"));
//                        } catch (Exception e) {
//                            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
//                        }
//                    }else {
//                        socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("0003CDD2-0000-1000-8000-00805F9B0131"));
//                    }
//                    if(BluetoothDevice.DEVICE_TYPE_LE == device.getType()){
//                        //socket.connect()
//                    }
//                    // 连接
//                    Log.d(TAG, "正在连接 "+device.getName()+ ",address=" + serverBlueToothAddress + " ,type = " + device.getType());
//                    connectInterface.connecting(device.getAddress());
////                    Message.obtain(handler, MESSAGE_TYPE_SEND, "请稍候，正在连接服务器: " + serverBlueToothAddress).sendToTarget();
//                    Log.d(TAG, "正在连接  _>>>>>>>>>>>");
//                    //socket.connect();
//                    Class<?> clazz = socket.getRemoteDevice().getClass();
//                    Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
//                    Method m = clazz.getMethod("createRfcommSocket", paramTypes);
//                    Object[] params = new Object[] {Integer.valueOf(1)};
//                    socket = (BluetoothSocket) m.invoke(socket.getRemoteDevice(), params);
//                    socket.connect();
//                if (mainHandler != null) {
//                    mainHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            connectInterface.connectSuccess(serverBlueToothAddress);
//                            Log.d(TAG, "连接 " + serverBlueToothAddress + " 成功 ");
//                        }
//                    });
//                }
            }
            // 如果实现了连接，那么服务端和客户端就共享一个RFFCOMM信道...
//                    Message.obtain(handler, MESSAGE_TYPE_SEND, "已经连接上服务端！可以发送信息").sendToTarget();
            // 如果连接成功了...这步就会执行...更新UI界面...否则走catch（IOException e）
//                    Message.obtain(handler, MESSAGE_ID_REFRESH_UI).sendToTarget();

            // 屏蔽点击事件
//                    listViewMessage.setOnItemClickListener(null);
//                } catch (final IOException e) {
//                    if (mainHandler != null) {
//                        mainHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                connectInterface.connectFailure(e);
//                                Log.d(TAG, "连接" + serverBlueToothAddress + "失败 " + e.getMessage());
//                            }
//                        });
//                    }
////                    e.printStackTrace();
//                } catch (NoSuchMethodException e) {
//                    e.printStackTrace();
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                } catch (InvocationTargetException e) {
//                    e.printStackTrace();
//                }
        }
    }

    public interface ReceivedMessageListener {
        void onReceiveMessage(ImageBuffer finalContent);

        void onConnectionInterrupt(Exception e);
    }

    private BufferedWriter writer = null;
    private Queue<String> bufferQueue = null;
    private Object lock = null;
    private ScanBufferThread scanBufferThread;

    private class ScanBufferThread extends Thread{
        private boolean isRunning = true;
        private boolean isFinish = true;
        private StringBuffer headBuffer = null;
        private StringBuffer nameBuffer = null;
        private boolean isExiteHead = false;
        private boolean isExiteName = false;
        private boolean isExiteSize = false;
        private StringBuffer sizeBuffer = null;
        private ByteBuffer byteBuffer = null;
        private ImageBuffer imageBuffer;
        private ReceivedMessageListener listener;
        private static final  char startStr  = '$';
        private  static final  char endStr = ',';
        private static final String separator = ":";
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
            while (isRunning){
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
            isRunning = false;
            this.interrupt();
        }

        private void scanBuffer() {
            isFinish = false;
            synchronized (lock){
                final String s = bufferQueue.poll();
                final int length = s.length();
                for(int i = 0 ; i < length;i ++){
                    final char c = s.charAt(i);
                    if(startStr == c && (headBuffer == null || headBuffer.length() < 2)){
                        if(headBuffer != null && headBuffer.length() == 2){
                            headBuffer.append(c);
                            nameBuffer = new StringBuffer();
                            imageBuffer = new ImageBuffer();
                        }
                        headBuffer = new StringBuffer();
                        headBuffer.append(c);
                    }else if(endStr == c){
                        if(nameBuffer != null && sizeBuffer == null){
                            final String name= nameBuffer.toString();
                            final String[] names = name.split(separator);
                            if(names.length == 2){
                                imageBuffer.setName(names[1]);
                                sizeBuffer = new StringBuffer();
                            }else {
                                Log.e(TAG,"数据错误 name = " + name);
                                headBuffer.reverse();
                                headBuffer = null;
                                nameBuffer.reverse();
                                nameBuffer = null;
                                imageBuffer = null;
                            }
//                            buffer.append(c);
//                            if (mainHandler != null) {
//                                mainHandler.post(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        listener.onReceiveMessage(buffer.toString());
//                                        buffer.reverse();
//                                        buffer = null;
//                                    }
//                                });
                            }
                            if(sizeBuffer != null){
                                final String name= sizeBuffer.toString();
                                final String[] names = name.split(separator);
                                if(names.length == 2 && isInteger(names[1])){
                                    final int size = Integer.parseInt(names[1]);
                                    imageBuffer.setSize(size);
                                    byteBuffer = ByteBuffer.allocate(size);
                                }else {
                                    Log.e(TAG,"数据错误 size = " + name);
                                    headBuffer.reverse();
                                    headBuffer = null;
                                    nameBuffer.reverse();
                                    nameBuffer = null;
                                    sizeBuffer.reverse();
                                    sizeBuffer = null;
                                    imageBuffer = null;
                                }
                            }
//                        }
                    }else if(nameBuffer != null){
                        nameBuffer.append(c);
                    }else if(sizeBuffer != null){
                        sizeBuffer.append(c);
                    }else if(byteBuffer != null){
                        byte[] b = charToBytes(c);
                        byteBuffer.put(b[0]);
                        byteBuffer.put(b[1]);
                        if(byteBuffer.hasRemaining()){
                            imageBuffer.setByteBuffer(byteBuffer);
                            if(listener != null){
                                listener.onReceiveMessage(imageBuffer);
                            }
                            headBuffer.reverse();
                            headBuffer = null;
                            nameBuffer.reverse();
                            nameBuffer = null;
                            sizeBuffer.reverse();
                            sizeBuffer = null;
                            imageBuffer = null;
                        }
                    }
                }
            }
            isFinish = true;
        }

    }

    private  byte[] charToByte(char c) {
        byte[] b = new byte[2];
        b[0] = (byte) ((c & 0xFF00) >> 8);
        b[1] = (byte) (c & 0xFF);
        return b;
    }

    /**
     * 将一个char字符转换位字节数组（2个字节），b[0]存储高位字符，大端
     *
     * @param c 字符（java char 2个字节）
     * @return 代表字符的字节数组
     */
    private byte[] charToBytes(char c)
    {
        byte[] b = new byte[2];
        b[0] = (byte) (c >>> 8);
        b[1] = (byte) c;
        return b;
    }

    /*方法二：推荐，速度最快
     * 判断是否为整数
     * @param str 传入的字符串
     * @return 是整数返回true,否则返回false
     */
    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
        }catch (Exception e){
            return false;
        }
        return true;
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
//                    reader = socket.getInputStream();
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
                    closeCloseable();
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

    private void closeCloseable() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }
//    private String TAG = "haha";
    private BluetoothGatt mBluetoothGatt;
    private boolean isServiceConnected;
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("haha", "onConnectionStateChange: " + newState);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                String err = "Cannot connect device with error status: " + status;

                gatt.close();
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
                if (mDevice != null) {
                    mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallback);
                }
                Log.e(TAG, err);
                return;
            }


            if (newState == BluetoothProfile.STATE_CONNECTED) {//当蓝牙设备已经连接

//获取ble设备上面的服务
//                Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                Log.i("haha", "Attempting to start service discovery:" +

                        mBluetoothGatt.discoverServices());

                Log.d("haha", "onConnectionStateChange: " + "连接成功");

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//当设备无法连接
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
                gatt.close();
                if (mDevice != null) {
                    mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallback);
                }
            }


        }

        //发现服务回调。
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("haha", "onServicesDiscovered: " + "发现服务 : " + status);
            super.onServicesDiscovered(gatt,status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isServiceConnected = true;

                boolean serviceFound = false;
                Log.d("haha", "onServicesDiscovered: " + "发现服务 : " + status);


                Log.d(TAG, "onServicesDiscovered: " + "读取数据0");

//                    Log.d(TAG, "onServicesDiscovered--" + "ACTION_GATT_SERVICES_DISCOVERED");
                    List<BluetoothGattService> bluetoothGattServices = gatt.getServices();
                    //发现服务是可以在这里查找支持的所有服务
//                        BluetoothGattService bluetoothGattService = gatt.getService(UUID.randomUUID());
                    for (BluetoothGattService bluetoothGattService : bluetoothGattServices) {
                        UUID uuid = bluetoothGattService.getUuid();
                        Log.d(TAG, "onServicesDiscovered--uuid=" + uuid);
                        List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = bluetoothGattService.getCharacteristics();
                        Log.d(TAG, "onServicesDiscovered--遍历特征值=");
                        /*获取指定服务uuid的特征值*/
//                        BluetoothGattCharacteristic mBluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(uuid);
//                            gatt.readCharacteristic(mBluetoothGattCharacteristic);
                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattCharacteristics) {
                            if(bluetoothGattCharacteristic != null) {
                                Log.d(TAG, "onServicesDiscovered--特征值 uuid=" + bluetoothGattCharacteristic.getUuid());
//                                gatt.readCharacteristic(bluetoothGattCharacterisbluetoothGattCharacteristictic);
//                                bluetoothGattCharacteristic.getValue();

                                Log.d(TAG, "onServicesDiscovered--指定服务uuid的特征值不为空=--");
                                final int charaProp = bluetoothGattCharacteristic.getProperties();

//                                bluetoothGattCharacteristic.getWriteType()==BluetoothGattCharacteristic.PROPERTY_READ
                                /*如果该字符串可读*/
                                if (gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)) {
                                    Log.d(TAG, "onServicesDiscovered--设置通知成功=--" + uuid);
                                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                    Log.d(TAG, "onServicesDiscovered--字符串可读--");
                                    gatt.readCharacteristic(bluetoothGattCharacteristic);
//                                    byte[] value = new byte[20];
//                                    bluetoothGattCharacteristic.setValue(value[0], BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//                                    String writeBytes = "HYL";
//                                    bluetoothGattCharacteristic.setValue(writeBytes.getBytes());
                                }
                                if((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                                /*3.再从指定的Characteristic中，我们可以通过getDescriptor()方法来获取该特征所包含的descriptor
				以上的BluetoothGattService、BluetoothGattCharacteristic、BluetoothGattDescriptor。
				我们都可以通过其getUuid()方法，来获取其对应的Uuid，从而判断是否是自己需要的。*/
                                    List<BluetoothGattDescriptor> bluetoothGattDescriptors = bluetoothGattCharacteristic.getDescriptors();
                                    Log.d(TAG, "onServicesDiscovered--遍历Descriptor=");
                                    for (BluetoothGattDescriptor bluetoothGattDescriptor : bluetoothGattDescriptors) {
                                        Log.d(TAG, "onServicesDiscovered--Descriptor uuid=" + bluetoothGattDescriptor.getUuid());
//                                    bluetoothGattDescriptor.getValue();
                                        boolean b1 = bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                                        if (b1) {
                                            mBluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
                                            Log.d(TAG, "startRead: " + "监听收数据");
                                            break;
                                        }
                                    }
                                }
                                    serviceFound = true;
                                    break;

                                }

                            }
                        }
                        if(serviceFound){
                            break;
                        }
                    }

//                if (mBluetoothGatt != null && isServiceConnected) {
//
//                    Method getUuidsMethod = null;
//                    try {
//                        getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
//                        ParcelUuid[] uuids = (ParcelUuid[]) getUuidsMethod.invoke(bluetoothAdapter, null);
//
////                        for (ParcelUuid uuid: uuids) {
//                            Log.d(TAG, "UUID: " + uuids.length);
////                        }
//                        if(uuids != null && uuids.length > 0) {
//                            for (ParcelUuid uuid : uuids) {
//                                Log.d(TAG, "UUID: " + uuid.getUuid().toString());
//                                BluetoothGattService gattService = mBluetoothGatt.getService(uuid.getUuid());
//                                if (gattService != null) {
//                                    Log.d(TAG,">>>>>1111");
//                                    BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(uuid.getUuid());
//                                    boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
//                                    if (b) {
//                                        Log.d(TAG,">>>>>222");
//                                        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
//                                        for (BluetoothGattDescriptor descriptor : descriptors) {
//
//                                            boolean b1 = descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
//                                            if (b1) {
//                                                mBluetoothGatt.writeDescriptor(descriptor);
//                                                Log.d(TAG, "startRead: " + "监听收数据");
//                                            }
//
//                                        }
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                   } catch (NoSuchMethodException e) {
//                        e.printStackTrace();
//                    } catch (IllegalAccessException e) {
//                        e.printStackTrace();
//                    } catch (InvocationTargetException e) {
//                        e.printStackTrace();
//                    }
//                }

                serviceFound = true;

            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
//            Log.d(TAG, "read value: " + characteristic.getValue());
            Log.d(TAG, "callback characteristic read status " + status
                    + " in thread " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] bytes = characteristic.getValue();
                Log.d(TAG, "read value: " + new String(bytes));
                if (mainHandler != null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mConnectInterface.connectSuccess(serverBlueToothAddress);
                            Log.d(TAG, "连接 " + serverBlueToothAddress + " 成功 ");
                        }
                    });
                }
            }


        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite: " + "设置成功");
            if (mainHandler != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mConnectInterface.connectSuccess(serverBlueToothAddress);
                        Log.d(TAG, "连接 " + serverBlueToothAddress + " 成功 ");
                    }
                });
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorRead: " + "设置成功");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: " + "发送成功");

            boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
            mBluetoothGatt.readCharacteristic(characteristic);
        }

        @Override
        public final void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
//            Log.d(TAG, "onCharacteristicChanged: " + value);
//            String s0 = Integer.toHexString(value[0] & 0xFF);
//            String s = Integer.toHexString(value[1] & 0xFF);
//            Log.d(TAG, "onCharacteristicChanged: " + s0 + "、" + s);
////            textView1.setText("收到: " + s0 + "、" + s);
//            for (byte b : value) {
//                Log.d(TAG, "onCharacteristicChanged: " + b);
//            }


        }

    };

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



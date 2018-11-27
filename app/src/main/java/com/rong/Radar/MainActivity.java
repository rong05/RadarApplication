package com.rong.Radar;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.rong.Radar.adapter.DialogItemAdapter;
import com.rong.Radar.serivce.BluetoothLeService;
import com.rong.Radar.tools.BleAdvertisedData;
import com.rong.Radar.tools.BleUtil;
import com.rong.Radar.tools.ClientUtil;
import com.rong.Radar.view.PointBuffer;
import com.rong.Radar.view.RadarView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private RadarView rv_radar;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private DialogItemAdapter adapter;
    private AlertDialog alertDialog;
    private ClientUtil clientUtil;
    private Queue<String> messageQueue;
    private Object lock;
    private List<PointBuffer> pointBufferList;

    String[] allPermissions=new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void init() {
        applypermission();
        //@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
//首先获取BluetoothManager
         bluetoothManager=(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //获取BluetoothAdapter
        if (bluetoothManager != null) {
             mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        // 用BroadcastReceiver来取得搜索结果
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);//搜索发现设备
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//状态改变
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);//行动扫描模式改变了
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//动作状态发生了变化
        registerReceiver(searchDevices, intent);
        clientUtil = ClientUtil.getInstance();
        clientUtil.init(mHandler,mBluetoothAdapter);
        messageQueue = new LinkedList<>();
        lock = new Object();
        pointBufferList = new ArrayList<>();
    }

    /**
     * 蓝牙接收广播
     */
    private BroadcastReceiver searchDevices = new BroadcastReceiver() {
        //接收
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle b = intent.getExtras();
            Object[] lstName = b.keySet().toArray();

            // 显示所有收到的消息及其细节
            for (int i = 0; i < lstName.length; i++) {
                String keyName = lstName[i].toString();
                Log.e("bluetooth", keyName + ">>>" + String.valueOf(b.get(keyName)));
            }

            // 搜索发现设备时，取得设备的信息；注意，这里有可能重复搜索同一设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                final BluetoothDevice device; device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                onRegisterBltReceiver.onBluetoothDevice(device);
                if(device != null) {
                    Log.e("tag", "device name: " + device.getName() + " address: " + device.getAddress());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(liDevices.containsKey(device.getAddress())){
                                return;
                            }
                            liDevices.put(device.getAddress(),device);
                            // mLeDeviceListAdapter.addDevice(device);
                            mHandler.sendEmptyMessage(1);
                        }
                    });

                }
            }
            //状态改变时
            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING://正在配对
                        Log.d("BlueToothTestActivity", "正在配对......");
//                        onRegisterBltReceiver.onBltIng(device);
                        Toast.makeText(MainActivity.this, "正在配对......", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothDevice.BOND_BONDED://配对结束
                        Log.d("BlueToothTestActivity", "完成配对");
//                        onRegisterBltReceiver.onBltEnd(device);
                        Toast.makeText(MainActivity.this, "完成配对", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothDevice.BOND_NONE://取消配对/未配对
                        Log.d("BlueToothTestActivity", "取消配对");
//                        onRegisterBltReceiver.onBltNone(device);
                        Toast.makeText(MainActivity.this, "取消配对", Toast.LENGTH_SHORT).show();
                    default:
                        break;
                }
            }
        }
    };

    /**
     * 反注册广播取消蓝牙的配对
     *
     * @param context
     */
    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(searchDevices);
        if (mBluetoothAdapter != null)
            mBluetoothAdapter.cancelDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSearthBltDevice();
        unregisterReceiver(this);
        clientUtil.stopReadMessage();
        clientUtil.onExit();
    }

    /**
     * 判断是否支持蓝牙，并打开蓝牙
     * 获取到BluetoothAdapter之后，还需要判断是否支持蓝牙，以及蓝牙是否打开。
     * 如果没打开，需要让用户打开蓝牙：
     */
    public void checkBleDevice(Context context) {
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(enableBtIntent);
            }
        } else {
            Log.i("blueTooth", "该手机不支持蓝牙");
        }
    }

    /**
     * 搜索蓝牙设备
     * 通过调用BluetoothAdapter的startLeScan()搜索BLE设备。
     * 调用此方法时需要传入 BluetoothAdapter.LeScanCallback参数。
     * 因此你需要实现 BluetoothAdapter.LeScanCallback接口，BLE设备的搜索结果将通过这个callback返回。
     * <p/>
     * 由于搜索需要尽量减少功耗，因此在实际使用时需要注意：
     * 1、当找到对应的设备后，立即停止扫描；
     * 2、不要循环搜索设备，为每次搜索设置适合的时间限制。避免设备不在可用范围的时候持续不停扫描，消耗电量。
     * <p/>
     * 如果你只需要搜索指定UUID的外设，你可以调用 startLeScan(UUID[], BluetoothAdapter.LeScanCallback)方法。
     * 其中UUID数组指定你的应用程序所支持的GATT Services的UUID。
     * <p/>
     * 注意：搜索时，你只能搜索传统蓝牙设备或者BLE设备，两者完全独立，不可同时被搜索。
     */
    private boolean startSearthBltDevice(Context context) {
        //开始搜索设备，当搜索到一个设备的时候就应该将它添加到设备集合中，保存起来
        checkBleDevice(context);
        //如果当前发现了新的设备，则停止继续扫描，当前扫描到的新设备会通过广播推向新的逻辑
        if (mBluetoothAdapter.isDiscovering())
            stopSearthBltDevice();
        Log.i("bluetooth", "本机蓝牙地址：" + mBluetoothAdapter.getAddress());
        //开始搜索
        mBluetoothAdapter.startDiscovery();
        //这里的true并不是代表搜索到了设备，而是表示搜索成功开始。
        return true;
    }

    public boolean stopSearthBltDevice() {
        //暂停搜索设备
        if(mBluetoothAdapter!=null)
            return mBluetoothAdapter.cancelDiscovery();
        return false;
    }

    private void initView() {
        rv_radar = findViewById(R.id.rv_radar);
    }

    //蓝牙设备
    private Map<String,BluetoothDevice> liDevices = new HashMap<>();

    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1: // Notify change
                    Log.d("SacanLeDevice", "3 step  cnnect device");
//                    if (!CoushionApplication.isconnect && liDevices.size() > 0) {// 如果没有连接，并且已经扫描到蓝牙设备
//                        SharedPreferences mySharedPreferences = getSharedPreferences(
//                                "device", Activity.MODE_PRIVATE);
//                        final String address = mySharedPreferences.getString(
//                                "DeviceAddress", "");// 获取上次链接的设备地址
//                        if (!address.equals("")) {
//                            for (int i = 0; i < liDevices.size(); i++) {
//                                if (liDevices.get(i).getAddress().equals(address)) {
////                                    BluetoothLeService.connect(address);//根据地址链接蓝牙设备
//                                    break;
//                                }
//                            }
//                        }
//                    }
                    if(alertDialog == null ) {
                        adapter = new DialogItemAdapter(MainActivity.this, liDevices);
                        View view = View.inflate(getApplicationContext(), R.layout.dialog_view, null);
                        alertDialog = new AlertDialog
                                .Builder(MainActivity.this)
                                .setView(view)
                                .create();
                        alertDialog.setCanceledOnTouchOutside(false);
                        alertDialog.show();
                        listView = alertDialog.findViewById(R.id.lv_device);
                        listView.setAdapter(adapter);
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                alertDialog.cancel();
                                alertDialog = null;
                                stopSearthBltDevice();
                                BluetoothDevice device = (BluetoothDevice) parent.getAdapter().getItem(position);
                                if(device != null) {
                                    Log.d("SacanLeDevice", device.toString());
                                    clientUtil.connectRemoteDevice(device.getAddress(),blueToothConnectCallback);
                                }

                            }
                        });
                    }else {
                        adapter.setList(liDevices);
                        adapter.notifyDataSetChanged();
                        if(!alertDialog.isShowing()){
                            alertDialog.show();
                        }else {
                            alertDialog.onContentChanged();
                        }
                    }
                    break;

                case 0:

                    break;
            }
        }
    };


    private ClientUtil.BlueToothConnectCallback blueToothConnectCallback = new ClientUtil.BlueToothConnectCallback() {
        @Override
        public void connecting(String serverBlueToothAddress) {

        }

        @Override
        public void connectSuccess(String serverBlueToothAddress) {
            clientUtil.setOnReceivedMessageListener(receivedMessageListener);
        }

        @Override
        public void connectFailure(Exception e) {
            Toast.makeText(MainActivity.this,"蓝牙连接失败！",Toast.LENGTH_LONG).show();
            e.printStackTrace();

        }
    };

    private ClientUtil.ReceivedMessageListener receivedMessageListener = new ClientUtil.ReceivedMessageListener(){

        @Override
        public void onReceiveMessage(String finalContent) {
            Log.d("MainActivity",finalContent);
        }

        @Override
        public void onConnectionInterrupt(Exception e) {
            Toast.makeText(MainActivity.this,"数据读取失败！",Toast.LENGTH_LONG).show();
            e.printStackTrace();
            if(!isDestroyed()) {
                clientUtil.setOnReceivedMessageListener(receivedMessageListener);
            }
        }
    };

    private ListView listView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //重写OptionsItemSelected(MenuItem item)来响应菜单项(MenuItem)的点击事件（根据id来区分是哪个item）
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.start:
                Toast.makeText(this, "开始扫描", Toast.LENGTH_SHORT).show();
                startSearthBltDevice(this);
                break;
//            case R.id.over:
//                Toast.makeText(this, "结束游戏", Toast.LENGTH_SHORT).show();
//                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void applypermission(){
        if(Build.VERSION.SDK_INT>=23){
            boolean needapply=false;
            for(int i=0;i<allPermissions.length;i++){
                int checkPermission= ContextCompat.checkSelfPermission(getApplicationContext(),
                        allPermissions[i]);
                if(checkPermission!=PackageManager.PERMISSION_GRANTED){
                    needapply=true;
                }
            }
            if(needapply){
                ActivityCompat.requestPermissions(MainActivity.this,allPermissions,1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i=0;i<grantResults.length;i++){
            if(grantResults[i]== PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this, permissions[i]+"已授权",Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(MainActivity.this,permissions[i]+"拒绝授权",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class PointReadThread extends Thread{
        private boolean isRunning = true;
        private boolean isFinish = true;
        private PointBuffer pointBuffer = null;
        private List<PointBuffer> msgPoints = new ArrayList<>();
        private static final  String startStr  = "[";
        private  static final  String endStr = "]";
        private static final String BREAKPOINT = ",";
        private int allSize = 0;
        private StringBuilder builder = null;
        private boolean isPointBuffer = false;
        public void readNotify(){
            if(this.getState() == State.WAITING){
                synchronized (this) {
                    this.notify();
                }
            }
        }
        public void readWait(){
            try {
                synchronized (this){
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
                if(messageQueue!= null && !messageQueue.isEmpty() && isFinish){
                    scanMessage();
                }else {
                    readWait();
                }
            }
        }
        private void scanMessage() {
            isFinish = false;
            synchronized (lock){
                String msg = messageQueue.poll();
                if(TextUtils.isEmpty(msg)){
                    isFinish = true;
                    return;
                }
                final String[] strings = msg.split(BREAKPOINT);
                final int length = strings.length;
            }
            isFinish = true;
        }

        private void error(){
            Log.e("SSS","数据有误！");
            if(builder != null) {
                builder.reverse();
                builder = null;
            }
            allSize = 0;
            pointBuffer = null;
            isPointBuffer = false;
            if(!msgPoints.isEmpty()) {
                msgPoints.clear();
            }
        }
    }


    /*方法二：推荐，速度最快
     * 判断是否为整数
     * @param str 传入的字符串
     * @return 是整数返回true,否则返回false
     */
    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

}

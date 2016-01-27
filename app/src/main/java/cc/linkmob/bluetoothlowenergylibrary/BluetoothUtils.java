package cc.linkmob.bluetoothlowenergylibrary;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2015 LinkMob.cc
 * <p/>
 * Contributors: Title
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothUtils {

    /**
     * 搜索时的上下文
     */
    private Activity mActivity;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    /**
     * 是否正在扫描
     */
    private boolean mScanning;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private static final int REQUEST_ENABLE_BT = 1;
    private static BluetoothUtils instance;

    /**
     * 获取该类的方法实例
     *
     * @param context 上下文对象
     * @return 本类的实例
     * @throws Exception 不支持ble
     */
    public static BluetoothUtils getInstance(Context context) throws Exception {
        if (instance == null) {
            synchronized (BluetoothUtils.class) {
                if (instance == null) {
                    instance = new BluetoothUtils();
                }
            }
        }
        if (instance.isSupportBLE(context)) {

            if (!instance.isBleEnable()) {
                instance.turnOnBlueTooth((Activity) context);
            }
            return instance;
        } else {
            throw new Exception("this device doesn't support bluetooth low energy!");
        }
    }

    private BluetoothUtils() {
        mHandler = new Handler();
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * 是否支持蓝牙ble
     *
     * @param context 上下文
     * @return 是否支持ble true表示支持
     */
    public boolean isSupportBLE(Context context) {
        return (isSupportBlueTooth() && isSupportBle(context));
    }

    /**
     * 搜索ble设备
     *
     * @param activity 开启搜索操作的Activity
     * @param enable   true 表示开始搜索，false表示停止搜索
     */
    public void scanLeDevice(final Activity activity, final boolean enable) {
//        mActivity = activity;
//        if (enable && (!mScanning)) {
//            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    activity.invalidateOptionsMenu();
//                }
//            }, SCAN_PERIOD);
//            mScanning = true;
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
//        } else if ((!enable) && mScanning) {
//            mScanning = false;
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            activity.invalidateOptionsMenu();
//        }
        scanLeDevice(activity, null, enable);
    }

    /**
     * 搜索ble设备
     *
     * @param activity 开启搜索操作的Activity
     * @param uuids    要查找的service的数组 Array of services to look for
     * @param enable   true 表示开始搜索，false表示停止搜索
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void scanLeDevice(final Activity activity, UUID[] uuids, final boolean enable) {
        mActivity = activity;
        if (!isBleEnable()) {
            turnOnBlueTooth(activity);
        }
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    activity.invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            mScanning = true;
            if (uuids == null || uuids.length <= 0) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);
            }
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        activity.invalidateOptionsMenu();
    }

    /**
     * 蓝牙是否打开
     *
     * @return true表示已经打开
     */
    public boolean isBleEnable() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * 是否正在搜索
     *
     * @return
     */
    public boolean isScanning() {
        return mScanning;
    }

    /**
     * 打开蓝牙
     */
    public void turnOnBlueTooth(Activity activity) {
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!isBleEnable()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        //打开成功的回调在onActivityResult方法中：resultCode == RESULT_OK则打开成功
    }

    /**
     * 关闭蓝牙
     */
    public boolean turnOffBlueTooth() {
        return !mBluetoothAdapter.isEnabled() || mBluetoothAdapter.disable();
    }

    /**
     * 是否支持蓝牙
     *
     * @return true 支持，false 不支持
     */
    private boolean isSupportBlueTooth() {
        return mBluetoothAdapter != null;
    }

    /**
     * 是否支持蓝牙
     *
     * @return true 支持，false 不支持
     */
    private boolean isSupportBle(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //  device为搜到的数据，搜到一个数据回调一次
                            if (onBluetoothUtilStatusChangeListener != null) {
                                onBluetoothUtilStatusChangeListener.onFindDevice(device);
                            }
                        }
                    });
                }
            };


    private BluetoothLeService mBluetoothLeService;
    private final static String TAG = "BluetoothUtils";


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                // 连接Server初始化失败时的回调
                if (onBluetoothUtilStatusChangeListener != null) {
                    onBluetoothUtilStatusChangeListener.onLeServiceInitFailed();
                }
            }
            // Automatically connects to the device upon successful start-up initialization.
            Log.d(TAG, "connecting...");
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }

    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                // 已经连接
                if (onBluetoothUtilStatusChangeListener != null) {

                    onBluetoothUtilStatusChangeListener.onConnected();
                    isConnected = true;
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //断开连接
                if (onBluetoothUtilStatusChangeListener != null) {
                    onBluetoothUtilStatusChangeListener.onDisconnected();
                    isConnected = false;
                }
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                // Show all the supported services and characteristics on the user interface.
                // 在用户接口上展示所有的services and characteristics
                //搜索到的service集合
                if (onBluetoothUtilStatusChangeListener != null) {
                    List<BluetoothGattService> Services = mBluetoothLeService.getSupportedGattServices();
                    if (Services != null && Services.size() > 0) {
                        onBluetoothUtilStatusChangeListener.onFindGattServices(Services);
                        if (mServiceUUID != null) {
                            BluetoothGattService gattService = mBluetoothLeService.getSupportedGattService(mServiceUUID);
                            if (gattService != null) {

                                onBluetoothUtilStatusChangeListener.onFindGattService(gattService);
                                if (mCharacteristicsUUID != null) {
                                    BluetoothGattCharacteristic BluetoothGattCharacteristic = gattService.getCharacteristic(mCharacteristicsUUID);
                                    if (BluetoothGattCharacteristic != null) {
                                        onBluetoothUtilStatusChangeListener.onFindGattCharacteristic(BluetoothGattCharacteristic);
                                    } else {
                                        Log.e(TAG, "BluetoothGattCharacteristic is null");
                                    }
                                }
                            } else {
                                Log.e(TAG, "BluetoothGattService is null");
                            }
                        }
                    }
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //获取到数据
                if (onBluetoothUtilStatusChangeListener != null) {
                    onBluetoothUtilStatusChangeListener.onFindData(intent.getStringExtra(BluetoothLeService.EXTRA_UUID), intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                }
            }else if (BluetoothLeService.ACTION_DATA_SEND.equals(action)){
                //成功发送数据
                if (onBluetoothUtilStatusChangeListener != null) {
                    onBluetoothUtilStatusChangeListener.onSendData(intent.getStringExtra(BluetoothLeService.EXTRA_UUID), intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                }
            }
        }
    };

    /**
     * Enables or disables notification on a give characteristic.
     * 设置发送信息的通知
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        mBluetoothLeService.setCharacteristicNotification(characteristic, enabled);

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_SEND);
        return intentFilter;
    }

//    /**
//     * 自动连接设备 在activity的生命周期内调用
//     *
//     * @param context       上下文
//     * @param deviceAddress 需要连接的设备地址
//     */
//    public void onResume(Context context, String deviceAddress) {
//        connectDevice(context, deviceAddress);
//    }

    /**
     * 有连接设备时的设备地址确定
     */
    private String mDeviceAddress;

    /**
     * 连接设备时赋值
     */
    private UUID mServiceUUID;
    /**
     * 连接设备时赋值
     */
    private UUID mCharacteristicsUUID;

//    private BluetoothGattService mBluetoothGattService;

    /**
     * 连接设备
     *
     * @param context       上下文
     * @param deviceAddress 要连接的地址
     * @param serviceUUID   要连接的service的UUID
     * @param characterUUID 要连接的Service中的characteristics的UUID
     */
    public void connectDevice(Context context, String deviceAddress, UUID serviceUUID, UUID characterUUID) {
        mCharacteristicsUUID = characterUUID;
        connectDevice(context, deviceAddress, serviceUUID);
    }

    /**
     * 连接设备
     *
     * @param context       上下文
     * @param deviceAddress 要连接的地址
     * @param serviceUUID   要连接的service的UUID
     */
    public void connectDevice(Context context, String deviceAddress, UUID serviceUUID) {
//        if(isConnected()){
//            Log.e("MainActivity","please disconnect the device before the connection");
//        }
        mServiceUUID = serviceUUID;
        connectDevice(context, deviceAddress);
    }

    /**
     * 设备是否是连接状态
     */
    private boolean isConnected = false;

//    public boolean isConnected() {
//        return false;
//    }

    private boolean isRegisterReceiver = false;
    private boolean isBindService = false;

    /**
     * 连接设备
     *
     * @param context       上下文
     * @param deviceAddress 要连接的地址
     */
    public void connectDevice(Context context, String deviceAddress) {
        if (isConnected) {
            Log.e("MainActivity", "please disconnect the device before the connection");
            return;
        }
        mDeviceAddress = deviceAddress;
        // 连接时停止扫描
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mActivity.invalidateOptionsMenu();
        if (!isRegisterReceiver) {
            context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            isRegisterReceiver = true;
        }
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(deviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
        if (isBindService) {

//        }else{
            context.unbindService(mServiceConnection);
//            context.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
//            isBindService = true;
        }
        context.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        isBindService = true;
    }

    /**
     * 断开连接
     */
    public void disconnecDevice() {
        mBluetoothLeService.disconnect();
//        onDestroy(context);
    }

    /**
     * 注销广播
     *
     * @param context 上下文
     */
    public void onDestroy(Context context) {
        if (isBindService) {
            context.unbindService(mServiceConnection);
            isBindService = false;
        }
        context.stopService(new Intent(context, BluetoothLeService.class));
        if (isRegisterReceiver && mGattUpdateReceiver != null) {
            context.unregisterReceiver(mGattUpdateReceiver);
            isRegisterReceiver = false;
        }
        if (mBluetoothLeService != null) {

            mBluetoothLeService.close();
            mBluetoothLeService.mBluetoothGatt = null;

            mBluetoothLeService = null;
            System.gc();
        }
    }


    /**
     * 发送数据
     *
     * @param notifyCharacter 要发向的Characteristic
     * @param msg             要发出的内容
     * @return true, if the write operation was initiated successfully
     * @throws NullPointerException notifyCharacter为空
     */

    public boolean sendData(BluetoothGattCharacteristic notifyCharacter, String msg) throws NullPointerException {
        // 判断链接状态
        if(!isConnected){
            Log.e(TAG,"the remote Device doesn't connected!");
            return false;
        }
        if (notifyCharacter != null) {

            notifyCharacter.setValue(msg.getBytes());
            return mBluetoothLeService.mBluetoothGatt.writeCharacteristic(notifyCharacter);
        } else {
            throw new NullPointerException("the BluetoothGattCharacteristic is null");
        }
    }

    /**
     * 设置监听器
     *
     * @param onBluetoothUtilStatusChangeLinsener 蓝牙状态监听器
     */
    public void setOnBluetoothUtilStatusChangeLinsener(OnBluetoothUtilStatusChangeListener onBluetoothUtilStatusChangeLinsener) {
        this.onBluetoothUtilStatusChangeListener = onBluetoothUtilStatusChangeLinsener;
    }


    private OnBluetoothUtilStatusChangeListener onBluetoothUtilStatusChangeListener;

    /**
     * BluetoothUtil状态变化监听器
     */
    public interface OnBluetoothUtilStatusChangeListener {

        /**
         * 搜索到设备时回调
         *
         * @param device 搜索到的设备
         */
        void onFindDevice(BluetoothDevice device);

        /**
         * ble连接服务初始化失败时的回调
         */
        void onLeServiceInitFailed();

        /**
         * 搜索到ble上的services时回调
         *
         * @param services 搜索到的service集合
         */
        void onFindGattServices(List<BluetoothGattService> services);

        /**
         * 当收到新数据时回调
         *
         * @param uuid 发送数据的uuid
         * @param data 发送的数据
         */
        void onFindData(String uuid, String data);

        /**
         * 连接上之后ble时回调
         */
        void onConnected();

        /**
         * 断开后回调
         */
        void onDisconnected();

        /**
         * 用{@link #connectDevice(Context, String, UUID)}或者{@link #connectDevice(Context, String, UUID, UUID)}搜索到ble上的services时回调
         *
         * @param service 根据连接时的UUID 搜索到的service
         */
        void onFindGattService(BluetoothGattService service);

        /**
         * 用{@link #connectDevice(Context, String, UUID, UUID)}搜索到ble上的characteristic时回调
         *
         * @param characteristic 根据连接时的UUID 搜索到的characteristic
         */
        void onFindGattCharacteristic(BluetoothGattCharacteristic characteristic);

        /**
         * 发送数据成功时回调
         * @param UUID 发送数据characteristic的UUID
         * @param data 发送的内容
         */
        void onSendData(String UUID, String data);
    }

//    /**
//     * 自定义异常 当用户的设备不支持ble功能时抛出
//     */
//    public static class BluetoothNotSupportException extends Exception {
//        public BluetoothNotSupportException() {
//            super("this device doesn't support bluetooth low energy!");
//        }
//    }
}




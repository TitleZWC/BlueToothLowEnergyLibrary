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

/**
 * Created by WenChao on 2016/1/11.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothUtils {

    /**
     * 搜索时的上下文
     */
    private Activity mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    /**
     * 是否正在扫描
     */
    public boolean mScanning;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private static final int REQUEST_ENABLE_BT = 1;
    private boolean isSupportBlueToothLowEnergy;
    private static BluetoothUtils instance;

    public static BluetoothUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (BluetoothUtils.class) {
                if (instance == null) {
                    instance = new BluetoothUtils(context);
                }
            }
        }
        return instance;
    }

    private BluetoothUtils(Context context) {
        mHandler = new Handler();
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Checks if Bluetooth is supported on the device.
//        if (isSupportBlueTooth() && isSupportBLE(context)) {
//
//            isSupportBlueToothLowEnergy = true;
//        } else {
//            isSupportBlueToothLowEnergy = false;
//        }
        isSupportBlueToothLowEnergy = isSupportBLE(context);
    }

    /**
     * 是否支持蓝牙ble
     *
     * @param context
     * @return true 表示支持
     */
    public boolean isSupportBLE(Context context) {
        return (isSupportBlueTooth() && isSupportBle(context));
    }


    /**
     * 搜索ble设备
     *
     * @param enable true 表示开始搜索，false表示停止搜索
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void scanLeDevice(Activity context, final boolean enable) throws BluetoothNotSupportException {
        if (!isSupportBlueToothLowEnergy) {
            throw new BluetoothNotSupportException("该设备不支持蓝牙V4");
        }
        if (!isBleEnable()) {
            turnOnBlueTooth(context);
        }
        mContext = context;
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * 蓝牙是否打开
     *
     * @return true表示已经打开
     */
    public boolean isBleEnable() {
        return !mBluetoothAdapter.isEnabled();
    }


    /**
     * 打开蓝牙
     */
    public void turnOnBlueTooth(Activity context) throws BluetoothNotSupportException {
        if (!isSupportBlueToothLowEnergy) {
            throw new BluetoothNotSupportException("该设备不支持蓝牙V4");
        } else {

            // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user to grant permission to enable it.
            if (!isBleEnable()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    /**
     * 关闭蓝牙
     */
    public void turnOffBlueTooth() throws BluetoothNotSupportException {
        if (!isSupportBlueToothLowEnergy) {
            throw new BluetoothNotSupportException("该设备不支持蓝牙V4");
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
            }
        }
    }

    /**
     * 是否支持蓝牙
     *
     * @return true 支持，false 不支持
     */
    private boolean isSupportBlueTooth() {
        if (mBluetoothAdapter != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否支持蓝牙
     *
     * @return true 支持，false 不支持
     */
    private boolean isSupportBle(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return true;
        } else {
            return false;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //  device为搜到的数据，搜到一个数据回调一次
                            if (onBluetoothUtilStatusChangeLinsener != null) {
                                onBluetoothUtilStatusChangeLinsener.onFindDevice(device);
                            }

//                            mLeDeviceListAdapter.addDevice(device);
//                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };


//////////////////////////////////////////////////////////////////////////////////////////////////////

    private BluetoothLeService mBluetoothLeService;
    private final static String TAG = "BluetoothUtils";


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
//                finish();
                // 连接Server初始化失败时的回调
                if (onBluetoothUtilStatusChangeLinsener != null) {
                    onBluetoothUtilStatusChangeLinsener.onLeServiceInitFailed();
                }
            }
            // Automatically connects to the device upon successful start-up initialization.
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
                if (onBluetoothUtilStatusChangeLinsener != null) {
                    onBluetoothUtilStatusChangeLinsener.onConnectStarted();
                }
//                mConnected = true;
//                updateConnectionState(R.string.connected);
//                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //断开连接
                if (onBluetoothUtilStatusChangeLinsener != null) {
                    onBluetoothUtilStatusChangeLinsener.onDisconnectStarted();
                }
//                mConnected = false;
//                updateConnectionState(R.string.disconnected);
//                invalidateOptionsMenu();
//                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                // 在用户接口上展示所有的services and characteristics
                //搜索到的service集合

                if (onBluetoothUtilStatusChangeLinsener != null) {
                    onBluetoothUtilStatusChangeLinsener.onFindGattServices(mBluetoothLeService.getSupportedGattServices());
                }
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //获取到数据
                if (onBluetoothUtilStatusChangeLinsener != null) {
                    onBluetoothUtilStatusChangeLinsener.onFindData(intent.getStringExtra(BluetoothLeService.EXTRA_UUID), intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                }
//                mDataField.setText(intent.getStringExtra("data"));
//                mDataField.setText(intent.getStringExtra("data"));
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
//    private final ExpandableListView.OnChildClickListener servicesListClickListner =
//            new ExpandableListView.OnChildClickListener() {
//                @Override
//                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
//                                            int childPosition, long id) {
//                    if (mGattCharacteristics != null) {
//                        final BluetoothGattCharacteristic characteristic =
//                                mGattCharacteristics.get(groupPosition).get(childPosition);
//                        final int charaProp = characteristic.getProperties();
//
//
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                            // If there is an active notification on a characteristic, clear
//                            // it first so it doesn't update the data field on the user interface.
//                            if (mNotifyCharacteristic != null) {
//                                mBluetoothLeService.setCharacteristicNotification(
//                                        mNotifyCharacteristic, false);
//                                mNotifyCharacteristic = null;
//                            }
//                            mBluetoothLeService.readCharacteristic(characteristic);
//                        }
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                            mNotifyCharacteristic = characteristic;
//                            mBluetoothLeService.setCharacteristicNotification(
//                                    characteristic, true);
//                        }
//                        return true;
//                    }
//                    return false;
//                }
//            };

//    private void suibian(BluetoothGattCharacteristic characteristic) {
//        //TOD  处理发来的信息
//        final int charaProp = characteristic.getProperties();
//        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//            // If there is an active notification on a characteristic, clear
//            // it first so it doesn't update the data field on the user interface.
//            if (characteristic != null) {
//                mBluetoothLeService.setCharacteristicNotification(
//                        characteristic, false);
//                characteristic = null;
//            }
//            mBluetoothLeService.readCharacteristic(characteristic);
//        }
//        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//            characteristic = characteristic;
//            mBluetoothLeService.setCharacteristicNotification(
//                    characteristic, true);
//        }
//    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    /**
     * 自动连接设备 在activity的生命周期内调用
     *
     * @param context
     * @param deviceAddress
     */
    public void onResume(Context context, String deviceAddress) {
        connectDevice(context, deviceAddress);
    }

    /**
     * 有连接设备时的设备地址确定
     */
    private String mDeviceAddress;

    /**
     * 连接设备
     *
     * @param context
     * @param deviceAddress 要连接的地址
     */
    public void connectDevice(Context context, String deviceAddress) {
        mDeviceAddress = deviceAddress;
        // 连接时停止扫描
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);

        context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            //开始连接回调，用于修改页面进度条为转动状态
            if (onBluetoothUtilStatusChangeLinsener != null) {
                onBluetoothUtilStatusChangeLinsener.onConnectStart();
            }
            final boolean result = mBluetoothLeService.connect(deviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    /**
     * 断开连接
     */
    public void disconnectionDevice() {
        // 开始断开连接回调，用于修改页面进度条为转动状态
        if (onBluetoothUtilStatusChangeLinsener != null) {
            onBluetoothUtilStatusChangeLinsener.onDisonnectStart();
        }
        mBluetoothLeService.disconnect();
    }

    /**
     * 注销广播  在activity的生命周期内调用
     *
     * @param context
     */
    public void onDestroy(Context context) {
        context.unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_connect:
//                Toast.makeText(this, "" + mBluetoothLeService.connect(mDeviceAddress), Toast.LENGTH_SHORT).show();
//                return true;
//            case R.id.menu_disconnect:
//                mBluetoothLeService.disconnect();
//                return true;
//            case android.R.id.home:
//                onBackPressed();
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }


    /**
     * 发送数据
     *
     * @param notifyCharacteristic 要发向的Characteristic
     * @param msg                  要发出的内容
     */
    public void sendData(BluetoothGattCharacteristic notifyCharacteristic, String msg) {

//        while (true)
//        {
//            try
//            {
//                Message localMessage = new Message();
//                localMessage.what = 15;
//                localMessage.arg1 = this.edtSend.length();
//                if (this.bHexSend.booleanValue())
//                {
//                    this.mNotifyCharacteristic.setValue(Common.HexStringToBytes(this.edtSend.getText().toString()));

//                    this.mHandler.sendMessage(localMessage);
//                    return;
//                }
//            }
        notifyCharacteristic.setValue(msg.getBytes());
        mBluetoothLeService.mBluetoothGatt.writeCharacteristic(notifyCharacteristic);
//        }
    }


    public void setOnBluetoothUtilStatusChangeLinsener(OnBluetoothUtilStatusChangeLinsener onBluetoothUtilStatusChangeLinsener) {
        this.onBluetoothUtilStatusChangeLinsener = onBluetoothUtilStatusChangeLinsener;
    }


    private OnBluetoothUtilStatusChangeLinsener onBluetoothUtilStatusChangeLinsener;

    /**
     * BluetoothUtil状态变化监听器
     */
    public interface OnBluetoothUtilStatusChangeLinsener {

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
         * 开始连接时回调
         */
        void onConnectStart();

        /**
         * 当用户选择断开连接时回调
         */
        void onDisonnectStart();

        /**
         * 搜索到ble上的service时回调
         *
         * @param supportedGattServices 搜索到的service集合
         */
        void onFindGattServices(List<BluetoothGattService> supportedGattServices);

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
        void onConnectStarted();

        /**
         * 断开后回调
         */
        void onDisconnectStarted();
    }

    /**
     * 自定义异常 当用户的设备不支持ble功能时抛出
     */
    public class BluetoothNotSupportException extends Exception {
        public BluetoothNotSupportException(String detailMessage) {
            super(detailMessage);
        }
    }
}




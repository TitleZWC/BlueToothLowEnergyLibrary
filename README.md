# BluetoothLowEnergyLibrary
a library to ble

一、添加依赖

Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:

allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}

Step 2. Add the dependency:

	dependencies {
	        compile 'com.github.TitleZWC:BlueToothLowEnergyLibrary:v0.1-alpha'
	}
 
二、声明并初始化BluetoothUtils。
 private BluetoothUtils mBluetoothUtil;
 
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		try {
            mBluetoothUtil = BluetoothUtils.getInstance(this);
        } catch (BluetoothNotSupportException e) {
            e.printStackTrace();
        }
	}

三、设置蓝牙状态监听器。

        mBluetoothUtil.setOnBluetoothUtilStatusChangeLinsener(new BluetoothUtils.OnBluetoothUtilStatusChangeListener() {
            @Override
            public void onFindDevice(BluetoothDevice device) {
                Toast.makeText(MainActivity.this,"FindDevice",Toast.LENGTH_SHORT).show();
                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeServiceInitFailed() {
                Toast.makeText(MainActivity.this, "LeServiceInitFailed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFindGattServices(List<BluetoothGattService> supportedGattServices) {
                Toast.makeText(MainActivity.this, "FindGattServices(", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFindData(String uuid, String data) {
                Toast.makeText(MainActivity.this, "FindData", Toast.LENGTH_SHORT).show();
                dataValue.setText(data);
            }

            @Override
            public void onConnected() {
                Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                mListView.setVisibility(View.GONE);
                gattView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onDisconnected() {
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFindGattService(BluetoothGattService supportedGattService) {
                Toast.makeText(MainActivity.this, "FindGattService", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFindGattCharacteristic(BluetoothGattCharacteristic characteristic) {
                Toast.makeText(MainActivity.this, "FindGattCharacteristic", Toast.LENGTH_SHORT).show();
                
                mNotifyCharacteristic = characteristic;
                mBluetoothUtil.setCharacteristicNotification(
                        characteristic, true);
            }

            @Override
            public void onSendData(String UUID, String data) {
                dataValue.append("发送：" + data + "\r\n");
            }
        });
		
四、搜索蓝牙设备。

	开始搜索：
	    mBluetoothUtil.scanLeDevice(this, true);

	停止搜索：
	    mBluetoothUtil.scanLeDevice(this, false);
	
五、连接设备。

	连接设备：
	    mBluetoothUtil.connectDevice(MainActivity.this, device.getAddress(), sId, cId);

	断开设备：
	    mBluetoothUtil.disconnectionDevice();
		
	
六、传递数据。

	发送数据
	    mBluetoothUtil.sendData(mNotifyCharacteristic, edtSend.getText().toString());

	接受数据
	    在OnBluetoothUtilStatusChangeListener的onFindData(String uuid, String data)方法中处理
	
七、关闭资源。

	mBluetoothUtil.onDestroy(this);

/*
 * Copyright (C) 2016 Freescale Semiconductor, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.freescale.bleclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.freescale.bleclient.global.GlobalContacts;
import com.freescale.bleclient.global.IMXUuid;
import com.freescale.bleclient.global.SampleGattAttributes;
import com.freescale.bleclient.service.BluetoothLeInterface;
import com.freescale.bleclient.service.BluetoothLeService;
import com.freescale.bleclient.utils.ListViewUtil;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceControlActivity extends Activity implements OnClickListener{

	private static final String TAG = "BleClient";
	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";
	private final String LIST_VALUE = "VALUE";
	private String mDeviceName;
	private String mDeviceAddress;

	private TextView mTvDevName;
	private TextView mTvAddress;
	private TextView mTvState;
	private ExpandableListView mLvService;
	private TextView mTvConnect;
	private EditText mEtMessage;
	private Button mBtnSend;
	private ScrollView mSvController;

	private DeviceInfoAdapter mDevAdapter;   

	private BluetoothLeInterface mBleController;
	private BleServiceConn mConn;
		
	private boolean mIsConnected = false;

	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private ArrayList<HashMap<String, String>> mGattServiceData;
	private ArrayList<ArrayList<HashMap<String, String>>> mGattCharacteristicData;
	private HashMap<String, BluetoothGattCharacteristic> mCharToRead;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initViews();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mBleController.disconnect(mDeviceAddress);
		if(mConn != null){
			unbindService(mConn);
		}
		mBleController = null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBleController != null) {
			final boolean result = mBleController.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	private void initViews() {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_controller);
		
		final Intent intent = getIntent();

		mDeviceName = intent.getStringExtra(GlobalContacts.EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(GlobalContacts.EXTRAS_DEVICE_ADDRESS);

		mTvDevName = (TextView) findViewById(R.id.tv_device_name);
		mTvAddress = (TextView) findViewById(R.id.tv_device_address);
		mTvState = (TextView) findViewById(R.id.tv_connection_state);
		mTvConnect = (TextView) findViewById(R.id.tv_connect_state);
		mLvService = (ExpandableListView) findViewById(R.id.lv_gatt_services_list);
		mEtMessage = (EditText)findViewById(R.id.et_message);
		mBtnSend = (Button)findViewById(R.id.btn_send);
		mSvController = (ScrollView) findViewById(R.id.sv_controller);
		
		mTvDevName.setText(mDeviceName);
		mTvAddress.setText(mDeviceAddress);
		mEtMessage.setVisibility(View.GONE);
		mBtnSend.setVisibility(View.GONE);

		mLvService.setOnGroupExpandListener(new OnGroupExpandListener() {
			
			@Override
			public void onGroupExpand(int groupPosition) {
				ListViewUtil.setListViewHeight(mLvService);
			}
		});
		
		
		if(mDeviceName.equals("i.MX Device")){
			mTvConnect.setOnClickListener(this);
			mConn = new BleServiceConn();
			Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
			startService(gattServiceIntent);
			bindService(gattServiceIntent, mConn, BIND_AUTO_CREATE);
		}
	}

	class BleServiceConn implements ServiceConnection{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "DeviceControlActivity onServiceConnected");
			
			mBleController = (BluetoothLeInterface) service;
			if (!mBleController.initialize(DeviceControlActivity.this)) {
				Log.d(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			
			mBleController.connect(mDeviceAddress);
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "DeviceControlActivity onServiceDisconnected");
		}
	}

	//Initial the IntentFilter of the BroadCastReceiver
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
	//                        or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				Log.d(TAG, "BluetoothLeService connect");
				mIsConnected = true;
				updateConnectionState(R.string.connected);
				mTvConnect.setText(R.string.disconnected);
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				Log.d(TAG, "BluetoothLeService disconnected");
				mIsConnected = false;
				updateConnectionState(R.string.disconnected);
				mTvConnect.setText(R.string.connected);
				clearUI();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the user interface.
				Log.d(TAG, "BluetoothLeService discovered");
				displayGattServices(mBleController.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				String value = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
				String charName = intent.getStringExtra(BluetoothLeService.EXTRA_NAME);
				if(charName.equals(GlobalContacts.MANFACTURER_NAME)){
					mGattCharacteristicData.get(3).get(0).put(LIST_VALUE, value);
				}else if(charName.equals(GlobalContacts.MODEL_NAME)){
					mGattCharacteristicData.get(3).get(1).put(LIST_VALUE, value);
				}else if(charName.equals(GlobalContacts.SERIAL_NUMBER)){
					mGattCharacteristicData.get(3).get(2).put(LIST_VALUE, value);
				}else if(charName.equals(GlobalContacts.BATTERY_LEVEL)){
					mGattCharacteristicData.get(4).get(0).put(LIST_VALUE, value);
				}else if(charName.equals(GlobalContacts.HEART_RATE)){
					mGattCharacteristicData.get(5).get(0).put(LIST_VALUE, value);
				}else if(charName.equals(GlobalContacts.CPU_TEMPERATURE)){
					mGattCharacteristicData.get(6).get(0).put(LIST_VALUE, value);
				}else if(charName.equals(GlobalContacts.REMOTE_DATE)){
					mGattCharacteristicData.get(7).get(0).put(LIST_VALUE, value);
				}
				mDevAdapter.notifyDataSetChanged();
			}
		}
	};
	

	// Demonstrates how to iterate through the supported GATT Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the ExpandableListView
	// on the UI.
	protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null) return;
		String uuid = null;
		String unknownServiceString = getResources().getString(R.string.unknown_service);
		String unknownCharaString = getResources().getString(R.string.unknown_characteristic);

		mGattServiceData = new ArrayList<HashMap<String, String>>();
		mGattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
		mCharToRead = new HashMap<String, BluetoothGattCharacteristic>();

		for (BluetoothGattService gattService : gattServices) {
			//Get GattService
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
			currentServiceData.put(LIST_UUID, uuid);
			mGattServiceData.add(currentServiceData);

			//To get the listview Data
			ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				Log.e(TAG, gattCharacteristic.getUuid().toString());
				if(gattCharacteristic.getUuid().toString().equals(IMXUuid.CHAR_MANUFACTURER_NAME_STRING)){
					mCharToRead.put(GlobalContacts.MANFACTURER_NAME, gattCharacteristic);
					mBleController.setCharacteristicNotification(gattCharacteristic, true);
				}else if(gattCharacteristic.getUuid().toString().equals(IMXUuid.CHAR_MODEL_NUMBER_STRING)){
					mCharToRead.put(GlobalContacts.MODEL_NAME, gattCharacteristic);
					mBleController.setCharacteristicNotification(gattCharacteristic, true);
				}else if(gattCharacteristic.getUuid().toString().equals(IMXUuid.CHAR_SERIAL_NUMBER_STRING)){
					mCharToRead.put(GlobalContacts.SERIAL_NUMBER, gattCharacteristic);
					mBleController.setCharacteristicNotification(gattCharacteristic, true);
				}else if(gattCharacteristic.getUuid().toString().equals(IMXUuid.CHAR_ALERT_LEVEL)){
					mCharToRead.put(GlobalContacts.ALERT_LEVEL, gattCharacteristic);
					mBleController.setCharacteristicNotification(gattCharacteristic, true);
				}else if(gattCharacteristic.getUuid().toString().equals(IMXUuid.CHAR_BATTERY_LEVEL)){
					mCharToRead.put(GlobalContacts.BATTERY_LEVEL, gattCharacteristic);
					mBleController.setCharacteristicNotification(gattCharacteristic, true);
				}else if(gattCharacteristic.getUuid().toString().equals(IMXUuid.CHAR_BATTER)){
					mCharToRead.put(GlobalContacts.HEART_RATE, gattCharacteristic);
					mBleController.setCharacteristicNotification(gattCharacteristic, true);
				}else if(gattCharacteristic.getUuid().toString().equals(IMXUuid.CHAR_CPU_TEMP)){
					mCharToRead.put(GlobalContacts.CPU_TEMPERATURE, gattCharacteristic);
					mBleController.setCharacteristicNotification(gattCharacteristic, true);
				}else if(gattCharacteristic.getUuid().toString().equals(IMXUuid.CHAR_DATE)){
					mCharToRead.put(GlobalContacts.REMOTE_DATE, gattCharacteristic);
					mBleController.setCharacteristicNotification(gattCharacteristic, true);
				}else if(gattCharacteristic.getUuid().toString().equals(IMXUuid.CHAR_MESSAGE)){
					mCharToRead.put(GlobalContacts.CUSTOM_MESSAGE, gattCharacteristic);
				}else{
				}

				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();
				currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
				currentCharaData.put(LIST_UUID, uuid);

				if(gattCharacteristic.getValue() != null){
					currentCharaData.put(LIST_VALUE, gattCharacteristic.getValue().toString());
				}else{
					currentCharaData.put(LIST_VALUE, "");
				}
				gattCharacteristicGroupData.add(currentCharaData);
			}
			mGattCharacteristics.add(charas);
			mGattCharacteristicData.add(gattCharacteristicGroupData);
		}
		mDevAdapter = new DeviceInfoAdapter();
		mLvService.setAdapter(mDevAdapter);
		ListViewUtil.setListViewHeight(mLvService);
		mEtMessage.setVisibility(View.VISIBLE);
		mBtnSend.setVisibility(View.VISIBLE);
	}

	private void updateConnectionState(final int resourceId) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mTvState.setText(resourceId);
			}
		});
	}

	private void clearUI() {
		if(mGattServiceData != null){
			mGattServiceData.clear();
		}
		if(mGattCharacteristicData != null){
			mGattCharacteristicData.clear();
		}
		if(mDevAdapter != null){
			mDevAdapter.notifyDataSetChanged();
		}
	}

	class DeviceInfoAdapter extends BaseExpandableListAdapter{

		@Override
		public int getGroupCount() {
			return mGattServiceData.size();
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return mGattCharacteristicData.get(groupPosition).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return 0;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return 0;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			ViewHolderParent holder;
			if(convertView == null){
				convertView = View.inflate(DeviceControlActivity.this, R.layout.list_parent_expand, null);
				holder = new ViewHolderParent();
				holder.tvService = (TextView) convertView.findViewById(R.id.tv_info_service);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolderParent) convertView.getTag();
			}
			HashMap<String, String> gattServiceData = new HashMap<String, String>();
			gattServiceData = mGattServiceData.get(groupPosition);
			String gattServiceName = gattServiceData.get(LIST_NAME);
			String uuid = gattServiceData.get(LIST_UUID);
			holder.tvService.setText(gattServiceName);
			return convertView;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			ViewHolderChild holder;
			if(convertView == null){
				convertView = View.inflate(DeviceControlActivity.this, R.layout.list_child_expand, null);
				holder = new ViewHolderChild();
				holder.tvChar = (TextView) convertView.findViewById(R.id.tv_info_char);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolderChild) convertView.getTag();
			}
			ArrayList<HashMap<String, String>> gattCharGroup = new ArrayList<HashMap<String,String>>();
			gattCharGroup = mGattCharacteristicData.get(groupPosition);
			HashMap<String, String> gattChar = gattCharGroup.get(childPosition);
			String charName = gattChar.get(LIST_NAME);
			String uuid = gattChar.get(LIST_UUID);
			String value = gattChar.get(LIST_VALUE);
			holder.tvChar.setText(charName + ":" + value);
			return convertView;
		}
		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}
	}

	class ViewHolderParent{
		TextView tvService;
	}
	class ViewHolderChild{
		TextView tvChar;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tv_connect_state:
			if(!mIsConnected){
				mBleController.connect(mDeviceAddress);
			}else{
				mBleController.disconnect(mDeviceAddress);
				clearUI();
			}
			break;
		default:
			break;
		}
	}
	
	public void AlarmTheServer(View view){
		BluetoothGattCharacteristic charMessage = mCharToRead.get(GlobalContacts.CUSTOM_MESSAGE);
		if(charMessage == null){
			return;
		}
		String message = mEtMessage.getText().toString();
		if(message.length() > 20){
			Toast.makeText(this, "no more than 20 letters", Toast.LENGTH_SHORT).show();
		}else{
			if(mBleController.writeCharacteristic(charMessage, message) == true){
				Toast.makeText(this, "Send Message Successfully", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(this, "Send Message Unsuccessfully", Toast.LENGTH_SHORT).show();
			}
		}
	}
}

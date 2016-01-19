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
package com.freescale.bleclient.service;

import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.freescale.bleclient.ble.BleManager;
import com.freescale.bleclient.global.GlobalContacts;
import com.freescale.bleclient.global.IMXUuid;
import com.freescale.bleclient.global.SampleGattAttributes;

public class BluetoothLeService extends Service {
	private final static String TAG = "BleClient";

	private BleManager mBleManager;

	private String mBluetoothDeviceAddress;
	private int mConnectionState = STATE_DISCONNECTED;

	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;

	public final static String ACTION_GATT_CONNECTED           = "com.freescale.bleclient.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED        = "com.freescale.bleclient.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.freescale.bleclient.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE           = "com.freescale.bleclient.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA                      = "com.freescale.bleclient.EXTRA_DATA";

	public final static UUID UUID_HEART_RATE_MEASUREMENT       = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

	public static final String EXTRA_NAME = "extra_name";

	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				intentAction = ACTION_GATT_CONNECTED;
				mConnectionState = STATE_CONNECTED;
				broadcastUpdate(intentAction);
				Log.d(TAG, "Connected to GATT server.");
				// Attempts to discover services after successful connection.
				mBleManager.discoveryService();
				Log.d(TAG, "Attempting to start service discovery:");

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				intentAction = ACTION_GATT_DISCONNECTED;
				mConnectionState = STATE_DISCONNECTED;
				Log.d(TAG, "Disconnected from GATT server.");
				broadcastUpdate(intentAction);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
			} else {
				Log.d(TAG, "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic,
				int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			Log.d(TAG, "onCharacteristicChanged");
			broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
		}
	};

	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action,
			final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);
		final byte[] data = characteristic.getValue();
		String charUuid = characteristic.getUuid().toString();
		String charId = null;
		if(charUuid.equals(IMXUuid.CHAR_ALERT_LEVEL)){
			charId = GlobalContacts.ALERT_LEVEL;
		}else if(charUuid.equals(IMXUuid.CHAR_MANUFACTURER_NAME_STRING)){
			charId = GlobalContacts.MANFACTURER_NAME;
		}else if(charUuid.equals(IMXUuid.CHAR_MODEL_NUMBER_STRING)){
			charId = GlobalContacts.MODEL_NAME;
		}else if(charUuid.equals(IMXUuid.CHAR_SERIAL_NUMBER_STRING)){
			charId = GlobalContacts.SERIAL_NUMBER;
		}else if(charUuid.equals(IMXUuid.CHAR_BATTERY_LEVEL)){
			charId = GlobalContacts.BATTERY_LEVEL;
		}else if(charUuid.equals(IMXUuid.CHAR_BATTER)){
			charId = GlobalContacts.HEART_RATE;
		}else if(charUuid.equals(IMXUuid.CHAR_DATE)){
			charId = GlobalContacts.REMOTE_DATE;
		}else if(charUuid.equals(IMXUuid.CHAR_MESSAGE)){
			charId = GlobalContacts.CUSTOM_MESSAGE;
		}else if(charUuid.equals(IMXUuid.CHAR_CPU_TEMP)){
			charId = GlobalContacts.CPU_TEMPERATURE;
		}
		intent.putExtra(EXTRA_NAME, charId);
		intent.putExtra(EXTRA_DATA, new String(data));
		sendBroadcast(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new BluetoothLeController();
	}

	class BluetoothLeController extends	Binder implements BluetoothLeInterface{

		@Override
		public boolean connect(String deviceAddress) {
			return BluetoothLeService.this.connect(deviceAddress);
		}

		@Override
		public boolean initialize(Activity activity) {
			return BluetoothLeService.this.initialize(activity);
		}

		@Override
		public List<BluetoothGattService> getSupportedGattServices() {
			return BluetoothLeService.this.getSupportedGattServices();
		}

		@Override
		public void disconnect(String mDeviceAddress) {
			BluetoothLeService.this.disconnect();
		}

		@Override
		public BluetoothGatt getGatt() {
			return BluetoothLeService.this.getGatt();
		}

		@Override
		public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
			return BluetoothLeService.this.setCharacteristicNotification(characteristic, enabled);
		}

		@Override
		public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String value) {
			return BluetoothLeService.this.writeCharacteristic(characteristic, value);
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that BluetoothGatt.close() is called
		// such that resources are cleaned up properly.  In this particular example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		return super.onUnbind(intent);
	}

	public boolean initialize(Activity activity) {
		boolean ret = true;
		mBleManager = BleManager.getInstance(activity);
		if(mBleManager == null)
			ret = false;
		return ret;
	
	}

	public boolean connect(final String address) {
		Log.d(TAG, "BluetoothLeService connect");
		if (mBleManager.isInit(address)) {
			Log.d(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		if (mBleManager.getRemoteDevices(address) == null) {
			Log.d(TAG, "Device not found.  Unable to connect.");
			return false;
		}

		mBleManager.connectGatt(this, false, mGattCallback);
		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		return true;
	}

	public void disconnect() {
		mBleManager.disconnect();
	}

	public void close() {
		mBleManager.closeGatt();
	}

	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		mBleManager.readCharacteristic(characteristic);
	}
	
	public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String value) {
		return mBleManager.writeCharacteristic(characteristic, value);
	}

	public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
			boolean enabled) {
		return mBleManager.setCharacteristicNotification(characteristic, enabled);
	}

	public List<BluetoothGattService> getSupportedGattServices() {
		return mBleManager.getSupportServices();
	}

	public BluetoothGatt getGatt(){
		return  mBleManager.getBluetoothGatt();
	}
	
}

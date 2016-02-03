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
package com.freescale.bleserver.ble;

import com.freescale.bleserver.MockServerCallBack;
import com.freescale.bleserver.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

public class BleServerManager {

	private static BleServerManager mBleServerManager;
	private static Activity mActivity;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothLeAdvertiser mBluetoothAdvertiser;
	private BluetoothGattServer mGattServer;

	private static final int REQUEST_ENABLE_BT = 1;
	private static final String TAG = "BleServer";
	private MockServerCallBack mMockServerCallBack;

	private BleServerManager(){
		initBle();
	}

	private void initBle() {
		
		if(mBluetoothManager == null)
			mBluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);

		if (mBluetoothManager != null && mBluetoothAdapter == null) {
			mBluetoothAdapter = mBluetoothManager.getAdapter();
		}

		//if the bluetooth has not been enabled, enable it!
		if ((mBluetoothAdapter == null) || (!mBluetoothAdapter.isEnabled())) {
			Toast.makeText(mActivity, R.string.bt_unavailable, Toast.LENGTH_SHORT).show();
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	public static BleServerManager getBleServerManager(Activity activity){
		mActivity = activity;
		if( mBleServerManager == null){
			mBleServerManager = new BleServerManager();
		}
		return mBleServerManager;
	}

	public boolean isBLESupported(Context context) {
		return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
	}

	public BluetoothManager getManager() {
		return mBluetoothManager; 
	}

	public static AdvertiseSettings createAdvSettings(boolean connectable, int timeoutMillis) {
		AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
		builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
		builder.setConnectable(connectable);
		builder.setTimeout(timeoutMillis);
		builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
		return builder.build();
	}


	public static AdvertiseData createFMPAdvertiseData() {
		AdvertiseData.Builder builder = new AdvertiseData.Builder();
		builder.setIncludeDeviceName(true);
		AdvertiseData adv = builder.build();
		return adv;
	}

	public boolean setName(String deviceName){
		if(mBluetoothAdapter != null)
			return mBluetoothAdapter.setName(deviceName);
		else 
			return false;
	}

	//start and stop Advertise as Immediate Alert Service
	public void startIASAdvertise(){
		
		initBle();
		if (mBluetoothAdapter == null) {
			return;
		}
		if (mBluetoothAdvertiser == null) {
			mBluetoothAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
		}
		if (mBluetoothAdvertiser != null) {
			mMockServerCallBack = new MockServerCallBack(mActivity);
			mGattServer = mBluetoothManager.openGattServer(mActivity, mMockServerCallBack);
			if(mGattServer == null){
				Log.d(TAG , "BleServerManger:gatt is null");
			}
			try{
				mMockServerCallBack.setupServices(mGattServer);
				mBluetoothAdvertiser.startAdvertising(createAdvSettings(true, 0), createFMPAdvertiseData(),mAdvCallback);
			}catch(InterruptedException e){
				Log.v(TAG, "Fail to setup BleService");
			}
		}
	}

	public void stopAdvertise() {

		if (mBluetoothAdvertiser != null) {
			mBluetoothAdvertiser.stopAdvertising(mAdvCallback);
			mBluetoothAdvertiser = null;
		}
		
		if(mBluetoothAdapter != null){
			mBluetoothAdapter = null;
		}
		
		if (mGattServer != null) {
			mGattServer.clearServices();
			mGattServer.close();
			mGattServer = null;
		}
	}

	private AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
		public void onStartSuccess(android.bluetooth.le.AdvertiseSettings settingsInEffect) {
			Log.d(TAG, "AdvertiseCallBack");
			if (settingsInEffect != null) {
				Log.d(TAG, "onStartSuccess TxPowerLv="+ settingsInEffect.getTxPowerLevel()+ " mode=" + settingsInEffect.getMode()+ " timeout=" + settingsInEffect.getTimeout());
			} else {
				Log.d(TAG, "onStartSuccess, settingInEffect is null");
			}
		}

		public void onStartFailure(int errorCode) {
			Log.d(TAG, "onStartFailure errorCode=" + errorCode);
		};
	};


	//Getters and Setters
	public BluetoothManager getBluetoothManager(){
		return mBluetoothManager;
	}

	public BluetoothAdapter getBluetoothAdapter(){
		return mBluetoothAdapter;
	}
}

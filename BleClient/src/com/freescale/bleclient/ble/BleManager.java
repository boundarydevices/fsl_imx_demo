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
package com.freescale.bleclient.ble;

import java.util.List;
import java.util.UUID;

import com.freescale.bleclient.R;
import com.freescale.bleclient.global.SampleGattAttributes;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

public class BleManager {

	private static BleManager bleManager;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mRemoteBluetoothDevices;
	private BluetoothGatt mBluetoothGatt;

	private static Activity mActivity;

	private static final int REQUEST_ENABLE_BT = 1;
	private static final String TAG = "BleManager";

	private BleManager(){
		initBle();
	}

	private boolean initBle() {
		//1.check whether the system support the ble
		if (!mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(mActivity, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			Log.e(TAG, "system cannot support the ble");
			mActivity.finish();
			return false;
		}

		//2.initial the bluetooth adapter, get a bluetooth adapter from bluetooth management
		Log.e(TAG, "initial the bluetooth adapter, get a bluetooth adapter from bluetooth managemen");
		mBluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
		if (mBluetoothManager == null) {
			Log.e(TAG, "Unable to initialize BluetoothManager.");
			return false;
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		//3.check the device if can support the bluetooth
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "device cannot support the bluetooth");
			Toast.makeText(mActivity, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			mActivity.finish();
			return false;
		}

		return true;
	}

	public static BleManager getInstance(Activity activity){
		mActivity = activity;
		if(bleManager == null){
			bleManager = new BleManager();
		}
		return bleManager;
	}

	public void enSureBleOpen(){
		if(mBluetoothAdapter == null){
			Log.d(TAG, "BluetoothAdapter is null");
			return;
		}			
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	// Device scan callback.
	public void setOnScanListener(OnScanListener onScanListener){
		this.onScanListener = onScanListener;
	}

	private OnScanListener onScanListener;

	public interface OnScanListener{
		void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord);
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			onScanListener.onLeScan(device, rssi, scanRecord);
		}
	};

	//Start and stop Scan.
	public void startScan(){
		if(mBluetoothAdapter == null){
			Log.d(TAG, "BluetoothAdapter is null");
			return;
		}			
		mBluetoothAdapter.startLeScan(mLeScanCallback);
	}

	public void stopScan(){
		if(mBluetoothAdapter == null){
			Log.d(TAG, "BluetoothAdapter is null");
			return;
		}			
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
	}

	public boolean isInit(String address){
		return mBluetoothAdapter == null || address == null;
	}

	public BluetoothDevice getRemoteDevices(String address){
		mRemoteBluetoothDevices = mBluetoothAdapter.getRemoteDevice(address);
		if (mRemoteBluetoothDevices == null) {
			Log.d(TAG, "Device not found.  Unable to connect.");
		}
		return mRemoteBluetoothDevices;
	}

	public boolean connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback){

		mBluetoothGatt = mRemoteBluetoothDevices.connectGatt(context, autoConnect, callback);
		if(mBluetoothGatt == null){
			Log.d(TAG, "bluetoothGatt is null");
			return false;
		}else{
			return true;
		}
	}

	public void disconnect() {

		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.d(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
	}

	public void closeGatt() {

		if (mBluetoothGatt == null) {
			Log.d(TAG, "BluetoothGatt not initialized");
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;		
	}

	public boolean readCharacteristic(BluetoothGattCharacteristic characteristic){

		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.d(TAG, "BluetoothAdapter not initialized");
			return false;
		}
		return mBluetoothGatt.readCharacteristic(characteristic);
	}
	
	public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String value){
		characteristic.setValue(value);
		if(mBluetoothGatt.writeCharacteristic(characteristic) == true){
			return true;
		}else{
			return false;
		}
	}

	public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled){
		
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.d(TAG, "BluetoothAdapter not initialized");
			return false;
		}
		
		if(mBluetoothGatt.setCharacteristicNotification(characteristic, enabled) == false){
			Log.e(TAG, "setCharacteristicNotification failed");
			return false;
		}
		
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
				UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
		if(descriptor == null){
			Log.e(TAG, "descriptor is null");
			return false;
		}
		
		if(descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == false){
			Log.e(TAG, "set Descriptor value fail");
			return false;
		}
		
		return mBluetoothGatt.writeDescriptor(descriptor);
	}
	
	public List<BluetoothGattService> getSupportServices(){
		if (mBluetoothGatt == null) 
			return null;
		return mBluetoothGatt.getServices();
	}
	
	//Getters and Settters
	public BluetoothAdapter getBtAdapter(){
		return mBluetoothAdapter;
	}

	public BluetoothManager getBluetoothManager(){
		return mBluetoothManager;
	}

	public BluetoothGatt getBluetoothGatt(){
		return mBluetoothGatt;
	}

	public boolean discoveryService(){
		return mBluetoothGatt.discoverServices();
	}
}

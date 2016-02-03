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
package com.freescale.bleserver;

import java.util.UUID;

import com.freescale.bleserver.HomeActivity.onBatteryChangedListener;
import com.freescale.bleserver.global.Attributes;
import com.freescale.bleserver.global.IMXUuid;
import com.freescale.bleserver.pager.HomePager;
import com.freescale.bleserver.pager.HomePager.onDataChangedListener;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

public class MockServerCallBack extends BluetoothGattServerCallback {
	private static final String TAG = "BleServer";
	private byte[] mAlertLevel = new byte[] {(byte) 0x00};
	private Activity mActivity;
	private HomePager mHomepager;
	private boolean mIsPushStatic = false;

	public MockServerCallBack(Activity activity) {
		this.mActivity = activity;
		this.mHomepager = ((HomeActivity)mActivity).mHomePager;
		mHomepager.setOnDataChangedListener(new onDataChangedListener() {

			@Override
			public void onDateChanged() {
				if(mDateChar !=null && btClient != null){
					mDateChar.setValue(Attributes.Date+"");
					if(btClient != null){
						mGattServer.notifyCharacteristicChanged(btClient, mDateChar, false);
					}
					if(!mIsPushStatic){
						try {
							mGattServer.notifyCharacteristicChanged(btClient, mManufacturerNameChar, false);
							Thread.sleep(10);
							mGattServer.notifyCharacteristicChanged(btClient, mModuleNumberChar, false);
							Thread.sleep(10);
							mGattServer.notifyCharacteristicChanged(btClient, mSerialNumberChar, false);
							Thread.sleep(10);
							mGattServer.notifyCharacteristicChanged(btClient, mBatteryChar, false);
							mIsPushStatic = true;
						} catch (InterruptedException e) {
						}
					}
				}
			}

			@Override
			public void onTemperatureChanged() {
				if(mTemperatureChar !=null && btClient != null){
					mTemperatureChar.setValue(Attributes.cpuTemp+"");
					if(btClient != null)
						mGattServer.notifyCharacteristicChanged(btClient, mTemperatureChar, false);
				}
			}

			@Override
			public void onHeartRateChanged() {
				if(mHeartRateChar !=null && btClient != null){
					mHeartRateChar.setValue(Attributes.heartRate+"");
					if(btClient != null)
						mGattServer.notifyCharacteristicChanged(btClient, mHeartRateChar, false);
				}
			}

		});

		HomeActivity homeUI = (HomeActivity) mActivity;
		homeUI.setonBatteryChangedListener(new onBatteryChangedListener() {

			@Override
			public void onBatteryChanged() {
				if(mBatteryChar !=null && btClient != null){
					mBatteryChar.setValue(Attributes.battery+"");
					if(btClient != null)
						mGattServer.notifyCharacteristicChanged(btClient, mBatteryChar, false);
				}
			}
		});
	}

	private BluetoothGattServer mGattServer;
	private BluetoothGattCharacteristic mDateChar;
	private BluetoothDevice btClient;
	private BluetoothGattCharacteristic mHeartRateChar;
	private BluetoothGattCharacteristic mTemperatureChar;
	private BluetoothGattCharacteristic mBatteryChar;
	private BluetoothGattCharacteristic mManufacturerNameChar;
	private BluetoothGattCharacteristic mModuleNumberChar;
	private BluetoothGattCharacteristic mSerialNumberChar;

	public void setupServices(BluetoothGattServer gattServer) throws InterruptedException{
		if (gattServer == null) {
			throw new IllegalArgumentException("gattServer is null");
		}
		mGattServer = gattServer;
		// setup services
		{ 
			//immediate alert
			BluetoothGattService ias = new BluetoothGattService( UUID.fromString(IMXUuid.SERVICE_IMMEDIATE_ALERT),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			//alert level char.
			BluetoothGattCharacteristic alc = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_ALERT_LEVEL),
					BluetoothGattCharacteristic.PROPERTY_READ |BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY ,
					BluetoothGattCharacteristic.PERMISSION_READ |BluetoothGattCharacteristic.PERMISSION_WRITE);
			alc.setValue("");
			ias.addCharacteristic(alc);
			if(mGattServer!=null && ias!=null)
				mGattServer.addService(ias);
		}
		Thread.sleep(100);
		{ 
			BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
					UUID.fromString(IMXUuid.CLIENT_CHARACTERISTIC_CONFIG), 
					BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
			//device information
			BluetoothGattService dis = new BluetoothGattService(
					UUID.fromString(IMXUuid.SERVICE_DEVICE_INFORMATION),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			mManufacturerNameChar = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_MANUFACTURER_NAME_STRING),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);
			mManufacturerNameChar.addDescriptor(descriptor);
			mManufacturerNameChar.setValue("Freescale");
			mModuleNumberChar = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_MODEL_NUMBER_STRING),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);
			mModuleNumberChar.addDescriptor(descriptor);
			mModuleNumberChar.setValue("i.MX");
			mSerialNumberChar = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_SERIAL_NUMBER_STRING),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);
			mSerialNumberChar.addDescriptor(descriptor);
			mSerialNumberChar.setValue("000-000-000");

			dis.addCharacteristic(mManufacturerNameChar);
			dis.addCharacteristic(mModuleNumberChar);
			dis.addCharacteristic(mSerialNumberChar);
			if(mGattServer!=null && dis!=null)
				mGattServer.addService(dis);
		}
		Thread.sleep(100);
		{
			//batery information
			BluetoothGattService bis = new BluetoothGattService(
					UUID.fromString(IMXUuid.SERVICE_BATTERY_SERVICE),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			mBatteryChar = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_BATTERY_LEVEL),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);

			BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
					UUID.fromString(IMXUuid.CLIENT_CHARACTERISTIC_CONFIG), 
					BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
			mBatteryChar.addDescriptor(descriptor);

			mBatteryChar.setValue(Attributes.battery+"");
			bis.addCharacteristic(mBatteryChar);
			if(mGattServer!=null && bis!=null)
				mGattServer.addService(bis);
		}
		Thread.sleep(100);
		{
			//heart rate information
			BluetoothGattService hris = new BluetoothGattService(
					UUID.fromString(IMXUuid.SERVICE_HEART_RATE),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			mHeartRateChar = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_BATTER),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);

			BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
					UUID.fromString(IMXUuid.CLIENT_CHARACTERISTIC_CONFIG), 
					BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
			mHeartRateChar.addDescriptor(descriptor);

			mHeartRateChar.setValue(Attributes.heartRate+"");
			hris.addCharacteristic(mHeartRateChar);
			if(mGattServer!=null && hris!=null)
				mGattServer.addService(hris);
		}
		Thread.sleep(100);
		{
			//cpu temperature
			BluetoothGattService cts = new BluetoothGattService(
					UUID.fromString(IMXUuid.SERVICE_CPU_TEMP),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			mTemperatureChar = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_CPU_TEMP),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY ,
					BluetoothGattCharacteristic.PERMISSION_READ);

			BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
					UUID.fromString(IMXUuid.CLIENT_CHARACTERISTIC_CONFIG), 
					BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
			mTemperatureChar.addDescriptor(descriptor);

			mTemperatureChar.setValue(Attributes.cpuTemp+"");
			cts.addCharacteristic(mTemperatureChar);
			if(mGattServer!=null && cts!=null)
				mGattServer.addService(cts);
		}
		Thread.sleep(100);
		{
			//Date Information
			BluetoothGattService dates = new BluetoothGattService(
					UUID.fromString(IMXUuid.SERVICE_DATE),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			mDateChar = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_DATE),
					BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);
			BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
					UUID.fromString(IMXUuid.CLIENT_CHARACTERISTIC_CONFIG), 
					BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
			mDateChar.addDescriptor(descriptor);

			if(Attributes.Date != null){
				mDateChar.setValue(Attributes.Date);
			}
			dates.addCharacteristic(mDateChar);
			if(mGattServer!=null && dates!=null)
				mGattServer.addService(dates);
		}
		Thread.sleep(100);
		{
			//Message Information
			BluetoothGattService mis = new BluetoothGattService(
					UUID.fromString(IMXUuid.SERVICE_MESSAGE),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			//Message char
			BluetoothGattCharacteristic messageChar = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_MESSAGE),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
					BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
			if(Attributes.Message != null){
				messageChar.setValue(Attributes.Message);
			}
			mis.addCharacteristic(messageChar);
			if(mGattServer!=null && mis!=null)
				mGattServer.addService(mis);
		}
	}

	public void onServiceAdded(int status, BluetoothGattService service) {
		if (status == BluetoothGatt.GATT_SUCCESS) {
			Log.d(TAG, "onServiceAdded status=GATT_SUCCESS service=" + service.getUuid().toString());
		} else {
			Log.d(TAG, "onServiceAdded status!=GATT_SUCCESS");
		}
	}

	public void onConnectionStateChange(android.bluetooth.BluetoothDevice device, int status,
			int newState) {
		Log.d(TAG, "onConnectionStateChange status=" + status + "->" + newState);
		if(status ==0 && newState == 0){
			btClient = null;
		}

	}

	public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device,
			int requestId, int offset, BluetoothGattCharacteristic characteristic) {
		Log.d(TAG, "onCharacteristicReadRequest requestId=" + requestId + " offset=" + offset);
		if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_MANUFACTURER_NAME_STRING))) {
			characteristic.setValue("Freescale");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_MODEL_NUMBER_STRING))) {
			characteristic.setValue("i.Mx");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_SERIAL_NUMBER_STRING))) {
			characteristic.setValue("000-000-000");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_ALERT_LEVEL))) {
			characteristic.setValue(mAlertLevel);
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_BATTERY_LEVEL))) {
			characteristic.setValue(Attributes.battery+"");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_BATTER))){
			characteristic.setValue(Attributes.heartRate+"");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_CPU_TEMP))){
			characteristic.setValue(Attributes.cpuTemp+"");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_DATE))){
			characteristic.setValue(Attributes.Date+"");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_MESSAGE))){
			characteristic.setValue(Attributes.Message+"");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		}
	}

	public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice device,
			int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite,
			boolean responseNeeded, int offset, byte[] value) {
		if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_ALERT_LEVEL))) {
			if (value != null && value.length > 0) {
				mAlertLevel[0] = value[0];
			} else {
			}
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
		}if(characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_MESSAGE))){
			String messageValue = new String(value);
			Attributes.Message = messageValue;
			if(MessageAcitivity.mMessageHandler != null)
				MessageAcitivity.mMessageHandler.sendEmptyMessage(0);
		}
	}

	@Override
	public void onDescriptorWriteRequest (BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

		btClient = device;
		Log.d(TAG, "onDescriptorWriteRequest");
		// now tell the connected device that this was all successfull
		mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
	}
}

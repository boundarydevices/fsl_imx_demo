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

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.util.Log;
import android.widget.Toast;

import com.freescale.bleserver.global.Attributes;
import com.freescale.bleserver.global.IMXUuid;

public class ImmediateAlertService extends BluetoothGattServerCallback {
	private static final String TAG = "BLE";
	private byte[] mAlertLevel = new byte[] {(byte) 0x00};
	private Activity mActivity;
	
	public ImmediateAlertService(Activity activity) {
		this.mActivity = activity;
	}
	
	private BluetoothGattServer mGattServer;
	
	public void setupServices(BluetoothGattServer gattServer) {
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

		{ 
			//device information
			BluetoothGattService dis = new BluetoothGattService(
					UUID.fromString(IMXUuid.SERVICE_DEVICE_INFORMATION),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			//manufacturer name string char.
			BluetoothGattCharacteristic mansc = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_MANUFACTURER_NAME_STRING),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);
			mansc.setValue("Freescale");
			//model number string char.
			BluetoothGattCharacteristic monsc = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_MODEL_NUMBER_STRING),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);
			monsc.setValue("i.MX");
			//serial number string char.
			BluetoothGattCharacteristic snsc = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_SERIAL_NUMBER_STRING),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);
			snsc.setValue("000-000-000");
			dis.addCharacteristic(mansc);
			dis.addCharacteristic(monsc);
			dis.addCharacteristic(snsc);
			if(mGattServer!=null && dis!=null)
				mGattServer.addService(dis);
		}

		{
			//batery information
			BluetoothGattService bis = new BluetoothGattService(
					UUID.fromString(IMXUuid.SERVICE_BATTERY_SERVICE),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			//batery level
			BluetoothGattCharacteristic blChar = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_BATTERY_LEVEL),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);
			blChar.setValue(Attributes.battery+"");
			bis.addCharacteristic(blChar);
			if(mGattServer!=null && bis!=null)
				mGattServer.addService(bis);
		}

		{
			//heart rate information
			BluetoothGattService hris = new BluetoothGattService(
					UUID.fromString(IMXUuid.SERVICE_HEART_RATE),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			//heart rate level
			BluetoothGattCharacteristic batc = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_BATTER),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);
			batc.setValue(Attributes.heartRate+"");
			hris.addCharacteristic(batc);
			if(mGattServer!=null && hris!=null)
				mGattServer.addService(hris);
		}

		{
			//cpu temperature
			BluetoothGattService cts = new BluetoothGattService(
					UUID.fromString(IMXUuid.SERVICE_CPU_TEMP),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			BluetoothGattCharacteristic TemperatureChar = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_CPU_TEMP),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY ,
					BluetoothGattCharacteristic.PERMISSION_READ);
			TemperatureChar.setValue(Attributes.cpuTemp+"");
			cts.addCharacteristic(TemperatureChar);
			if(mGattServer!=null && cts!=null)
				mGattServer.addService(cts);
		}

		{
			//Date Information
			BluetoothGattService dates = new BluetoothGattService(
					UUID.fromString(IMXUuid.SERVICE_DATE),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			//Date char
			BluetoothGattCharacteristic dateChar = new BluetoothGattCharacteristic(
					UUID.fromString(IMXUuid.CHAR_DATE),
					BluetoothGattCharacteristic.PROPERTY_READ,
					BluetoothGattCharacteristic.PERMISSION_READ);
			if(Attributes.Date != null){
				dateChar.setValue(Attributes.Date);
			}
			dates.addCharacteristic(dateChar);
			if(mGattServer!=null && dates!=null)
				mGattServer.addService(dates);
		}

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
	}

	public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device,
			int requestId, int offset, BluetoothGattCharacteristic characteristic) {
		Log.d(TAG, "onCharacteristicReadRequest requestId=" + requestId + " offset=" + offset);
		if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_MANUFACTURER_NAME_STRING))) {
			Log.d(TAG, "CHAR_MANUFACTURER_NAME_STRING");
			characteristic.setValue("Freescale");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_MODEL_NUMBER_STRING))) {
			Log.d(TAG, "CHAR_MODEL_NUMBER_STRING");
			characteristic.setValue("i.Mx");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_SERIAL_NUMBER_STRING))) {
			Log.d(TAG, "CHAR_SERIAL_NUMBER_STRING");
			characteristic.setValue("000-000-000");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_ALERT_LEVEL))) {
			Log.d(TAG, "CHAR_ALERT_LEVEL");
			characteristic.setValue(mAlertLevel);
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_BATTERY_LEVEL))) {
			Log.d(TAG, "CHAR_BATTERY_LEVEL");
			characteristic.setValue(Attributes.battery+"");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_BATTER))){
			Log.d(TAG, "CHAR_BATTER");
			characteristic.setValue(Attributes.heartRate+"");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_CPU_TEMP))){
			Log.d(TAG, "CHAR_CPU_TEMP");
			characteristic.setValue(Attributes.cpuTemp+"");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_DATE))){
			Log.d(TAG, "CHAR_DATE");
			characteristic.setValue(Attributes.Date+"");
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,characteristic.getValue());
		} else if (characteristic.getUuid().equals(UUID.fromString(IMXUuid.CHAR_MESSAGE))){
			Log.d(TAG, "CHAR_MESSAGE");
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
			MessageAcitivity.mMessageHandler.sendEmptyMessage(0);
		}
	}
}

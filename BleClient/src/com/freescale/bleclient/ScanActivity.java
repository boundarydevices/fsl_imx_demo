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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.freescale.bleclient.global.GlobalContacts;
import com.freescale.bleclient.R;

public class ScanActivity extends Activity implements OnClickListener{

	private static final String TAG = "BLE";
	private static final int REQUEST_ENABLE_BT = 1;

	private ProgressBar mPbScan;
	private TextView mTvScan;
	private ScanAdapter mScanAdapter;
	private ListView mLvScan;
	private ArrayList<BluetoothDevice> mLeDevices;

	private BluetoothAdapter mBluetoothAdapter;
	private boolean mIsScanning;
	private Handler mHandler;
	private static final long SCAN_PERIOD = 10000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initBle();
		initView();
		ensureBleDevice();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mScanAdapter.notifyDataSetChanged();
		scanLeDevice(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
		mLeDevices.clear();
	}

	private void ensureBleDevice(){
		// Ensure that the bluetooth device can be used
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
		mScanAdapter = new ScanAdapter();
		mLvScan.setAdapter(mScanAdapter);
		scanLeDevice(true);
	}

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			// To scan for 10 seconds and stop scanning
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mIsScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					mPbScan.setVisibility(View.INVISIBLE);
					mTvScan.setText("SCAN");
				}
			}, SCAN_PERIOD);
			mIsScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mIsScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
		mPbScan.setVisibility(View.VISIBLE);
		mTvScan.setText("STOP");
	}

	private void initBle() {
		//1.check whether the system support the ble
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}
		//2.initial the bluetooth adapter, get a bluetooth adapter from bluetooth management
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		//3.check the device if can support the bluetooth
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		mHandler = new Handler();
		mLeDevices = new ArrayList<BluetoothDevice>();
	}

	private void initView() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		mPbScan = (ProgressBar) findViewById(R.id.pb_scan);
		mTvScan = (TextView) findViewById(R.id.tv_scan_flag);
		mLvScan = (ListView) findViewById(R.id.lv_sacnner);

		mTvScan.setOnClickListener(this);
		mLvScan.setOnItemClickListener(new ScanListItemClick());
	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if(!mLeDevices.contains(device)){
						mLeDevices.add(device);
					}
					mScanAdapter.notifyDataSetChanged();
				}
			});
		}
	};

	//ListView Adapter
	
	class ScanAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			return mLeDevices.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder	holder;
			if(convertView == null){
				convertView = View.inflate(ScanActivity.this, R.layout.item_list_scan, null);
				holder = new ViewHolder();
				holder.tvDevice = (TextView) convertView.findViewById(R.id.tv_scan_device);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			BluetoothDevice device = mLeDevices.get(position);
			String deviceName = device.getName();
			if (deviceName != null && deviceName.length() > 0)
				holder.tvDevice.setText(deviceName);
			else
				holder.tvDevice.setText(R.string.unknown_device);
			return convertView;
		}
	}

	class ViewHolder{
		TextView tvDevice;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tv_scan_flag:
			if(mIsScanning){
				scanLeDevice(false);
			}else{
				mLeDevices.clear();
				mScanAdapter.notifyDataSetChanged();
				scanLeDevice(true);
			}
			break;
		default:
			break;
		}
	}
	
	class ScanListItemClick implements OnItemClickListener{
		
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			//get the device from listview
	        final BluetoothDevice device = mLeDevices.get(position);
	        if (device == null)
	        	return;
	        //start deviceControlActivity
	        final Intent intent = new Intent(ScanActivity.this, DeviceControlActivity.class);
	        intent.putExtra(GlobalContacts.EXTRAS_DEVICE_NAME, device.getName());
	        intent.putExtra(GlobalContacts.EXTRAS_DEVICE_ADDRESS, device.getAddress());
	        if (mIsScanning) {
	            mBluetoothAdapter.stopLeScan(mLeScanCallback);
	            mIsScanning = false;
	        }
	        startActivity(intent);
		}
	}

		
	
}

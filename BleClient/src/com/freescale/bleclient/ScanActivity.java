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

import com.freescale.bleclient.ble.BleManager;
import com.freescale.bleclient.ble.BleManager.OnScanListener;
import com.freescale.bleclient.global.GlobalContacts;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
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
import android.util.Log;

public class ScanActivity extends Activity implements OnClickListener, OnScanListener{

	private static final String TAG = "BleClient";

	private ProgressBar mPbScan;
	private TextView mTvScan;
	private ScanAdapter mScanAdapter;
	private ListView mLvScan;
	private ArrayList<BluetoothDevice> mLeDevices;

	private boolean mIsScanning;
	private Handler mHandler;
	private static final long SCAN_PERIOD = 10000;

	private BleManager mBleManager;

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
		mBleManager.enSureBleOpen();
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
					mBleManager.startScan();
					mPbScan.setVisibility(View.INVISIBLE);
					mTvScan.setText("SCAN");
				}
			}, SCAN_PERIOD);
			mIsScanning = true;
			mBleManager.startScan();
		} else {
			mIsScanning = false;
			mBleManager.stopScan();
		}
		mPbScan.setVisibility(View.VISIBLE);
		mTvScan.setText("STOP");
	}

	private void initBle() {
		mBleManager = BleManager.getInstance(this);
		mBleManager.setOnScanListener(this);
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
			final BluetoothDevice device = mLeDevices.get(position);
			if (device == null)
				return;
			Log.d(TAG, "device name is " + device.getName() + ", device address is " + device.getAddress() );
			final Intent intent = new Intent(ScanActivity.this, DeviceControlActivity.class);
			intent.putExtra(GlobalContacts.EXTRAS_DEVICE_NAME, device.getName());
			intent.putExtra(GlobalContacts.EXTRAS_DEVICE_ADDRESS, device.getAddress());
			if (mIsScanning) {
				mBleManager.stopScan();
				mIsScanning = false;
			}
			startActivity(intent);
		}
	}

	@Override
	public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {	
		Log.d(TAG, "onLeScane in ScanActivity");
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

}

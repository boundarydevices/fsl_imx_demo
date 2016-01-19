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
import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.freescale.bleserver.R;
import com.freescale.bleserver.global.Attributes;
import com.freescale.bleserver.pager.BasePager;
import com.freescale.bleserver.pager.HomePager;
import com.freescale.bleserver.pager.SettingPager;
import com.freescale.bleserver.utils.BleUtil;
import com.freescale.bleserver.utils.PrefUtils;

public class HomeActivity extends FragmentActivity{

	//Fragment Tag
	private static final String FRAG_HOME = "homeFragment";
	private static final String FRAG_EDIT = "editFragment";
	private static final String TAG = "BLE";
	//UI Compoments
	private RadioGroup mRgHome;
	private BatteryReceiver mBatReceiver = null;
	private TextView mTitle;
	public ViewPager mVpHome;
	private HomePagerAdapter mHomePagerAdapter;
	public ArrayList<BasePager> mPagerList;
	//2 child pager of ViewPager
	private HomePager mHomePager;
	private SettingPager mSettingPager;

	private static final int REQUEST_ENABLE_BT = 1;
	// BT
	private BluetoothAdapter mBTAdapter;
	private BluetoothLeAdvertiser mBTAdvertiser;
	public BluetoothGattServer mGattServer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initView();
		initBle();
		initData();

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mHomePager.refreshView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mHomePager.stopThread();
		unregisterReceiver(mBatReceiver);
		stopAdvertise();
		PrefUtils.setBoolean(this, PrefUtils.BLE_STATE, false);
	}

	private void initView() {
		//find view
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_home);
		mRgHome = (RadioGroup) findViewById(R.id.rg_group);
		mTitle = (TextView)findViewById(R.id.tv_home_title);
		mVpHome = (ViewPager) findViewById(R.id.vp_home);
		//set listener
		mRgHome.setOnCheckedChangeListener(new MyRgCheckListener());
	}

	private void initBle() {
		//1.BLE check
		if (!BleUtil.isBLESupported(this)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		//2. BT check, get BTAdapter
		BluetoothManager manager = BleUtil.getManager(this);
		if (manager != null) {
			mBTAdapter = manager.getAdapter();
		}
		//3.if the bluetooth has not been enabled, enable it!
		if ((mBTAdapter == null) || (!mBTAdapter.isEnabled())) {
			Toast.makeText(this, R.string.bt_unavailable, Toast.LENGTH_SHORT).show();
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		//4.set device name
		mBTAdapter.setName("i.MX Device");
		//5.set the related UI listener, set it in the HomePager
	}

	private void initData() {
		//view pager
		mPagerList = new ArrayList<BasePager>();
		mHomePager = new HomePager(this);
		mSettingPager = new SettingPager(this);
		mPagerList.add(mHomePager);
		mPagerList.add(mSettingPager);
		mHomePagerAdapter = new HomePagerAdapter();
		mVpHome.setAdapter(mHomePagerAdapter);	

		//batteryInfo Receiver
		mBatReceiver = new BatteryReceiver();
		IntentFilter filter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(mBatReceiver, filter);
	}

	//RadioGrupCheckListener
	class MyRgCheckListener implements OnCheckedChangeListener{

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			switch (checkedId) {
			case R.id.rb_home:
				mVpHome.setCurrentItem(0,false);
				mHomePager.setEnable();
				break;
			case R.id.rb_setting:
				mVpHome.setCurrentItem(1,false);
				mSettingPager.initData();
				break;
			default:
				break;
			}			
		}
	}

	private class BatteryReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			int rawlevel = intent.getIntExtra("level", -1);
			int scale = intent.getIntExtra("scale", -1); 
			int level = -1;  
			if (rawlevel >= 0 && scale > 0) {  
				level = (rawlevel * 100) / scale;  
			}  
			Attributes.battery = level;
		}
	}

	//ViewPagerAdapter
	class HomePagerAdapter extends PagerAdapter{

		@Override
		public int getCount() {
			return mPagerList.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			BasePager pager = mPagerList.get(position);
			pager.initData(); 
			container.addView(pager.mRootView);		
			return pager.mRootView;
		}
	}

	//start and stop Advertise as Immediate Alert Service
	public void startIASAdvertise() {
		if (mBTAdapter == null) {
			return;
		}
		if (mBTAdvertiser == null) {
			mBTAdvertiser = mBTAdapter.getBluetoothLeAdvertiser();
		}
		if (mBTAdvertiser != null) {
			ImmediateAlertService ias = new ImmediateAlertService(this);
			mGattServer = BleUtil.getManager(this).openGattServer(this, ias);
			if(mGattServer == null){
				Log.e("aa" , "gatt is null");
			}
			ias.setupServices(mGattServer);
			mBTAdvertiser.startAdvertising(BleUtil.createAdvSettings(true, 0),BleUtil.createFMPAdvertiseData(),mAdvCallback);
		}
	}

	public void stopAdvertise() {
		//close the gatt server
		if (mGattServer != null) {
			mGattServer.clearServices();
			mGattServer.close();
			mGattServer = null;
		}
		//close the btadvertiser
		if (mBTAdvertiser != null) {
			mBTAdvertiser.stopAdvertising(mAdvCallback);
			mBTAdvertiser = null;
		}
	}

	private AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
		public void onStartSuccess(android.bluetooth.le.AdvertiseSettings settingsInEffect) {
			Log.i(TAG, "callback 0");
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
	
}

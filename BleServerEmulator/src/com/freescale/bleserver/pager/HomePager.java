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
package com.freescale.bleserver.pager;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freescale.bleserver.R;
import com.freescale.bleserver.EditActivity;
import com.freescale.bleserver.HomeActivity;
import com.freescale.bleserver.global.Attributes;
import com.freescale.bleserver.utils.PrefUtils;
import com.freescale.bleserver.utils.StreamUtil;

public class HomePager extends BasePager implements OnClickListener{ 

	//Varialbe related to some thread
	private boolean mIsRunTemp = true;
	private boolean mIsRunHeart = true;
	private boolean mIsRunDate = true;
	private boolean mIsBleOn;

	//UI Compoments
	private TextView mTvTemp;
	private TextView mTvBleState;
	private RelativeLayout mRlCpuTempl;
	private RelativeLayout mRlMessage;
	private RelativeLayout mRlHeart;
	private RelativeLayout mRlSetting;
	private TextView mTvHeartRate;


	//handler of refreshing the temperature
	private Handler mTempHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			mTvTemp.setText(Attributes.cpuTemp + "'C");
		};
	};

	private Handler mHeartRateHandler  = new Handler(){
		public void handleMessage(android.os.Message msg) {
			mTvHeartRate.setText(Attributes.heartRate+ "");
		};
	};

	private HomeActivity homeUI;

	public HomePager(Activity activity) {
		super(activity);	
		homeUI = (HomeActivity) mActivity;
	}

	@Override	
	public void initViews() {

		mRootView = View.inflate(mActivity, R.layout.pager_home, null);
		mTvTemp   = (TextView) mRootView.findViewById(R.id.tv_home_temperature_value);
		mTvBleState = (TextView) mRootView.findViewById(R.id.btn_on_off);
		mTvHeartRate = (TextView) mRootView.findViewById(R.id.tv_home_heart_rate);
		mRlCpuTempl = (RelativeLayout) mRootView.findViewById(R.id.rl_cpu_temp);
		mRlMessage = (RelativeLayout) mRootView.findViewById(R.id.rl_cpu_message);
		mRlHeart = (RelativeLayout) mRootView.findViewById(R.id.rl_cpu_heart);
		mRlSetting = (RelativeLayout) mRootView.findViewById(R.id.rl_cpu_setting);

		//setListener
		mTvBleState.setOnClickListener(this);
		mRlCpuTempl.setOnClickListener(this);
		mRlMessage.setOnClickListener(this);
		mRlHeart.setOnClickListener(this);
		mRlSetting.setOnClickListener(this);

		setEnable();
	}

	@Override
	public void initData() {
		new ScanTimeThread().start();
		new HeartRateThread().start();
		new DateThread().start();

	}

	public void setEnable(){
		mIsBleOn = PrefUtils.getBoolean(mActivity, PrefUtils.BLE_STATE, false);
		refreshState(mIsBleOn);
	}

	public void refreshState(boolean isOn){
		if(isOn){
			mTvBleState.setText(R.string.ble_on);
			mTvBleState.setBackgroundResource(R.drawable.btn_ok_shape);
		}else{
			mTvBleState.setText(R.string.ble_off);
			mTvBleState.setBackgroundResource(R.drawable.btn_cancel_shape);
		}
	}

	class ScanTimeThread extends Thread{
		@Override
		public void run() {
			while(mIsRunTemp){
				String command = "cat /sys/class/thermal/thermal_zone0/temp";
				try {
					Process process = Runtime.getRuntime().exec (command);
					InputStream is = process.getInputStream();
					String cpuTemp = StreamUtil.getStreamString(is);
					Attributes.cpuTemp = Integer.parseInt(cpuTemp)/1000;
					mTempHandler.sendEmptyMessage(0);
					sleep(5000);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class HeartRateThread extends Thread{
		@Override
		public void run() {
			while(mIsRunHeart){
				Attributes.heartRate = (int) Math.floor(Math.random()*30 + 50);
				mHeartRateHandler.sendEmptyMessage(0);
				try {
					sleep(60000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class DateThread extends Thread{
		@Override
		public void run() {
			while(mIsRunDate){
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Attributes.Date = df.format(new Date());
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void stopThread(){
		mIsRunTemp = false;
		mIsRunHeart = false;
		mIsRunDate = false;
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_on_off:
			if(mIsBleOn){
				mIsBleOn = false;
				homeUI.stopAdvertise();
			}else{
				mIsBleOn = true;
				homeUI.startIASAdvertise();
			}
			refreshState(mIsBleOn);
			PrefUtils.setBoolean(mActivity, PrefUtils.BLE_STATE, mIsBleOn);
			break;
		case R.id.rl_cpu_temp:
		case R.id.rl_cpu_setting:
		case R.id.rl_cpu_heart:
		case R.id.rl_cpu_message:
			Intent intent = new Intent(mActivity, EditActivity.class);
			mActivity.startActivity(intent);
			mActivity.overridePendingTransition(R.anim.tran_in, R.anim.tran_out);
			break;
		default:
			break;
		}
	}
}


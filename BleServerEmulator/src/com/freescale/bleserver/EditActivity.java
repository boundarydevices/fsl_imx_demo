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

import com.freescale.bleserver.global.Attributes;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.TextView;

public class EditActivity extends Activity{

	private TextView mTvCpuTemp;
	private TextView mTvBattery;
	private TextView mTvHeartRate;
	private TextView mTvDate;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initView();
		initData();
	}

	private void initData() {
		new FillTextThread().start();
	}

	private void initView() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_edit);
		mTvCpuTemp = (TextView) findViewById(R.id.tv_edit_cpu);
		mTvBattery = (TextView) findViewById(R.id.tv_battery);
		mTvHeartRate = (TextView) findViewById(R.id.tv_edit_heart);
		mTvDate = (TextView) findViewById(R.id.tv_edit_date);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.pre_tran_in, R.anim.pre_tran_out);
	}
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			mTvCpuTemp.setText("CPU Temperature:" + Attributes.cpuTemp + "'C");
			mTvBattery.setText("Battery:" + Attributes.battery + "%");
			mTvHeartRate.setText("HeartRate:" + Attributes.heartRate );
			mTvDate.setText("Date:" + Attributes.Date);
		};
	};
	
	private boolean isRunning = true;
	class FillTextThread extends Thread{
		@Override
		public void run() {
			while(isRunning){
				try {
					mHandler.sendEmptyMessage(0);
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

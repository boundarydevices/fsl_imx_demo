/*
 * Copyright (C) 2015 Freescale Semiconductor, Inc.
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
package com.freescale.sleepawake;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

public class MyActivity extends Activity {

	private EditText mEt_awake;
	private EditText mEt_sleep;
	private Button   mBtn_start;
	private CheckBox mCb_random;
	private TextView mTv_times;
	private static final int SLEEP_TIME_MIN = 5000;
	private SharedPreferences mSp;
	private SharedPreferences.Editor mEd;
	private DevicePolicyManager mDpm;
	private Button mBtn_stop;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);		
		initParam();
	}

	public void initParam(){
		mEt_awake  = (EditText) findViewById(R.id.et_awake);
		mEt_sleep  = (EditText) findViewById(R.id.et_sleep);
		mBtn_start = (Button) findViewById(R.id.btn_start) ;
		mBtn_stop = (Button) findViewById(R.id.btn_stop);
		mCb_random = (CheckBox) findViewById(R.id.cb_random);
		mTv_times  = (TextView) findViewById(R.id.tv_times);
		mSp = getSharedPreferences("AwakeSleepAutoTest", MODE_PRIVATE);
		mEd = mSp.edit();
		mDpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		int times = mSp.getInt("times", 0);

		mTv_times.setText("Test times:"+times);

	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		ComponentName who = new ComponentName(this,MyAdmin.class);
		if(mDpm.isAdminActive(who)){
			mBtn_start.setEnabled(true);
			mBtn_stop.setEnabled(true);
		}else{
			mBtn_start.setEnabled(false);
			mBtn_stop.setEnabled(false);
		}
	}

	public void startTest(View v){		

		int awakeTime = Integer.parseInt(mEt_awake.getText().toString()) * 1000;
		int sleepTime = Integer.parseInt(mEt_sleep.getText().toString()) * 1000;

		mTv_times.setText("Test times:0");
		mEd.putBoolean("isRandom", mCb_random.isChecked());
		mEd.commit();
		//ensure that the sleep time should not below 5s
		if(sleepTime < SLEEP_TIME_MIN){
			Toast.makeText(getApplicationContext(), "SleepTime should be set above 5s", Toast.LENGTH_LONG).show();
		}else{
			Intent intent = new Intent(MyActivity.this, MyService.class);
			intent.putExtra("awaketime", awakeTime);
			intent.putExtra("sleeptime", sleepTime);
			startService(intent);	
			Toast.makeText(getApplicationContext(), "SleepAwake Service has been started!", Toast.LENGTH_LONG).show();
		}
	}

	public void stopTest(View v){
		Intent intent = new Intent("android.intent.action.CANCEL_MY_SLEEP_AWAKE");
		sendBroadcast(intent);
	}

	public void setAdmin(View v){
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		ComponentName   mDeviceAdminSample = new ComponentName(this,MyAdmin.class);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdminSample);
		intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
				"Active the device administrators before start the test");
		startActivity(intent);
	}
	
	public void uninstall(View view ){
		
		ComponentName   mDeviceAdminSample = new ComponentName(this,MyAdmin.class);
		mDpm.removeActiveAdmin(mDeviceAdminSample);
		mBtn_start.setEnabled(false);
		mBtn_stop.setEnabled(false);
	}
	
}

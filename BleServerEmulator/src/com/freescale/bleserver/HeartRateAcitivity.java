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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class HeartRateAcitivity extends Activity implements OnClickListener{

	private EditText mEtHeartRate;
	private TextView mTvHeartRate;
	private boolean mIsRunHeart = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initView();
	}

	private void initView() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_heartrate);
		mEtHeartRate = (EditText) findViewById(R.id.et_heart_rate);
		mTvHeartRate = (TextView) findViewById(R.id.tv_heart_rate);
		Button mBtnModify = (Button) findViewById(R.id.btn_modify);
		
		mBtnModify.setOnClickListener(this);
		new HeartRateThread().start();
	}

	private void modifyHeartRate(){
		String heartRate = mEtHeartRate.getText().toString();
		Log.e("aa", heartRate);
		Attributes.heartRate = Integer.parseInt(heartRate);
		mTvHeartRate.setText("Heart Rate:"+ Attributes.heartRate);
	}
	
	
	class HeartRateThread extends Thread{
		
		@Override
		public void run() {
			super.run();
			while(mIsRunHeart){
				runOnUiThread(new Runnable() {
					public void run() {
						mTvHeartRate.setText("Heart Rate:"+ Attributes.heartRate);
					}
				});
				try {
					sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.pre_tran_in, R.anim.pre_tran_out);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mIsRunHeart = false;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_modify:
			modifyHeartRate();
			break;

		default:
			break;
		}
	}
	
}

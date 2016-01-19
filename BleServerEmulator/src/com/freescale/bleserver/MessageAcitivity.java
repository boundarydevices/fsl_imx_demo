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
import android.view.WindowManager;
import android.widget.TextView;

public class MessageAcitivity extends Activity {

	private static TextView mTvMesCon;
	public static Handler mMessageHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_message);
		
		mTvMesCon = (TextView) findViewById(R.id.tv_message_content);
		mTvMesCon.setText(Attributes.Message);
		mMessageHandler = new Handler(){
			
			public void handleMessage(android.os.Message msg) {
				if(mTvMesCon != null){
					mTvMesCon.setText(Attributes.Message);
				}
			};
		};
	}
	
	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.pre_tran_in, R.anim.pre_tran_out);
	}
}

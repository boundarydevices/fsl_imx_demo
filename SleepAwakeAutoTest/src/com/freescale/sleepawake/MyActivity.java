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
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MyActivity extends Activity {
	
	private EditText mEt_awake;
	private EditText mEt_sleep;
	private Button   mBtn_start;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findId();
	}
	
	public void findId(){
		mEt_awake  = (EditText) findViewById(R.id.et_awake);
		mEt_sleep  = (EditText) findViewById(R.id.et_sleep);
		mBtn_start = (Button) findViewById(R.id.btn_start) ;
	}
	
	public void startTest(View v){
		int awakeTime = Integer.parseInt(mEt_awake.getText().toString());
		int sleepTime = Integer.parseInt(mEt_sleep.getText().toString());
		Intent intent = new Intent(MyActivity.this, MyService.class);
		intent.putExtra("awaketime", awakeTime);
		intent.putExtra("sleeptime", sleepTime);
		startService(intent);
		Toast.makeText(getApplicationContext(), "SleepAwake Service has been started!", Toast.LENGTH_LONG).show();
		finish();
	}
}

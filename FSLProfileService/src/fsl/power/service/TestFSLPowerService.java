/*
 * Copyright (C) 2012 Freescale Semiconductor, Inc.
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


package fsl.power.service;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TestFSLPowerService extends Activity{

    private OnClickListener mStartService = new OnClickListener() {
	public void onClick(View v) {
		Intent stintent = new Intent();
		stintent.setClass(TestFSLPowerService.this, FSLPowerOptionService.class);
		TestFSLPowerService.this.startService(stintent);
	}
    };

    private OnClickListener mStopService = new OnClickListener() {
	public void onClick(View v) {
		Intent stintent = new Intent();
		stintent.setClass(TestFSLPowerService.this, FSLPowerOptionService.class);
		TestFSLPowerService.this.stopService(stintent);
	}
    };


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Button start = (Button)findViewById(R.id.start);
		start.setOnClickListener(mStartService);
		Button stop = (Button)findViewById(R.id.stop);
		stop.setOnClickListener(mStopService);
	    }

}

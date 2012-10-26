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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class PowerServiceReceiver extends BroadcastReceiver{

	private static final String TAG = "PowerServiceReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		Intent startservice;
		startservice = new Intent(context, FSLPowerOptionService.class);
		int id = intent.getIntExtra("profile", 3);
		ContentResolver cr = context.getContentResolver();
		Log.i(TAG, action + "----"+"change to profile" + id);
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
			context.startService(startservice);
			//FSLPowerOptionService.activeProfile(cr,id);
		}
		else if (intent.getAction().equals("fsl.power.service.action.START_SERVICE" )){
			FSLPowerOptionService.activeProfile(cr,id);
			context.startService(startservice);

		}



	}

}

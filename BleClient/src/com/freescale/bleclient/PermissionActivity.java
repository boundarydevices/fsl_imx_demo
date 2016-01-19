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

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import android.util.Log;
import com.freescale.bleclient.R;

public class PermissionActivity extends Activity {

	private int mNumPermissionsToRequest = 0;
	private boolean mShouldRequestLocationPermission = false;
	private boolean mFlagHasLocationPermission = true;
	private int mIndexPermissionRequestLocation = 0;
	private static final int PERMISSION_REQUEST_CODE = 0;
	private static final String TAG = "BleClient PermissionActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.permissions);
		checkPermission();
	}

	private void checkPermission(){

		if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED){
			mNumPermissionsToRequest++;
			mShouldRequestLocationPermission  = true;
		}else{
			mFlagHasLocationPermission  = true;
		}

		String[] permissionToRequest = new String[mNumPermissionsToRequest];
		int permissionRequestIndex = 0;

		if(mShouldRequestLocationPermission){
			permissionToRequest[permissionRequestIndex] = Manifest.permission.ACCESS_COARSE_LOCATION;
			mIndexPermissionRequestLocation= permissionRequestIndex;
			permissionRequestIndex++;
		}

		if(permissionToRequest.length > 0){
			requestPermissions(permissionToRequest, PERMISSION_REQUEST_CODE);
		}else{
			enterScanMenu();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
			String permissions[], int[] grantResults) {
		switch (requestCode) {
		case PERMISSION_REQUEST_CODE:
			if (grantResults.length > 0
					&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Log.v(TAG, "Grant permission successfully");
			} else {
				Log.v(TAG, "Grant Permission unsuccessfully");
			}
			break;
		default:
			break;
		}
		enterScanMenu();
	}

	private void enterScanMenu(){
		Intent enterScan = new Intent(this, ScanActivity.class);
		startActivity(enterScan);
		finish();
	}

}



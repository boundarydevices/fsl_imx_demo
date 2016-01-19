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

import com.freescale.bleserver.R;
import com.freescale.bleserver.utils.PrefUtils;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SettingPager extends BasePager implements OnClickListener{

	private TextView mTvState;
	private ToggleButton mBtnState;

	public SettingPager(Activity activity) {
		super(activity);
	}

	@Override
	public void initViews() {
		mRootView = View.inflate(mActivity, R.layout.pager_setting, null);
		mTvState = (TextView) mRootView.findViewById(R.id.tv_setting_state);
		mBtnState = (ToggleButton) mRootView.findViewById(R.id.mTogBtn);
		mBtnState.setOnClickListener(this);
		
	}

	private void refreshState(boolean isEnable) {
		if(isEnable){
			mTvState.setText(R.string.ble_on);
		}else{
			mTvState.setText(R.string.ble_off);
		}
		mBtnState.setChecked(isEnable);
	}

	@Override
	public void initData() {
		boolean isBleEnable = PrefUtils.getBoolean(mActivity, PrefUtils.BLE_STATE, false);
		refreshState(isBleEnable);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.mTogBtn:
			if(mBtnState.isChecked()){
				PrefUtils.setBoolean(mActivity, PrefUtils.BLE_STATE, true);
			}else{
				PrefUtils.setBoolean(mActivity, PrefUtils.BLE_STATE, false);
			}
			refreshState(mBtnState.isChecked());
			break;

		default:
			break;
		}
	}

}

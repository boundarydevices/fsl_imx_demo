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
package com.imx.cts;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import com.imx.AssetsActivity;
import com.imx.cts.utils.CmdUtil;
import android.test.ActivityInstrumentationTestCase2;

public class IMxCtsAssetsTestCase  extends ActivityInstrumentationTestCase2<AssetsActivity> {
    private static final String TAG = "IMxCtsAssetsTestCase";
    protected AssetsActivity mActivity;
    protected String mBoard;
    protected String mCurGovernorVal;

    public IMxCtsAssetsTestCase() {
        super(AssetsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CmdUtil.CmdResult curScalingGovernor = CmdUtil.execCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
        mCurGovernorVal = curScalingGovernor.successMsg;
        CmdUtil.execCommand("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
        mActivity = getActivity();
        getInstrumentation().waitForIdleSync();
        mBoard = whichBoard();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mActivity.finish();
        CmdUtil.execCommand("echo " + mCurGovernorVal +" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
    }

    private String whichBoard() {
        CmdUtil.CmdResult boardNameMsg = CmdUtil.execCommand("getprop ro.product.board");
        CmdUtil.CmdResult socMsg = CmdUtil.execCommand("getprop ro.soc");
        return socMsg.successMsg + "_" + boardNameMsg.successMsg;
   }
}

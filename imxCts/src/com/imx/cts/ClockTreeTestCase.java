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

import android.util.Log;
import com.imx.cts.IMxCtsAssetsTestCase;
import com.imx.cts.utils.CmdUtil;
import com.imx.cts.utils.CmdUtil.CmdResult;
import com.imx.AssetsActivity;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ClockTreeTestCase extends IMxCtsAssetsTestCase {

    private static final String TAG = "ClockTreeTestCase";

    public void testClockTree() {

        List<ClockInfo> clockInfos = parseParseClockTree();
        List<ClockInfo> standardInfos = getStandardClockTree();
        for (int i = 0; i < clockInfos.size(); i++) {
            assertEquals(clockInfos.get(i).clockName + ":rate is not equal", standardInfos.get(i).rate, clockInfos.get(i).rate);
        }
    }

    private List<ClockInfo> parseParseClockTree() {
        CmdResult clockTreeStr = CmdUtil.execCommand("cat /sys/kernel/debug/clk/clk_summary");
        return getClockInfo(clockTreeStr.successMsg);
    }

    private List<ClockInfo> getStandardClockTree() {

        String fileName = new String();

        boolean isDualDisplay = true;
        //Is it dual display mode?
        String cmd = "cat /sys/class/graphics/fb2/mode";
        CmdUtil.CmdResult actualResult = CmdUtil.execCommand(cmd);
        if (actualResult.errorMsg.contains("No such file or directory")) {
            isDualDisplay = false;
        }

        fileName = generateFile(isDualDisplay);

        List<ClockInfo> ret = new ArrayList();
        try {
            InputStream inputStream = mActivity.getClockTree(fileName);
            String clockInfoStr = inputStream2String(inputStream);
            String clockInfoStrReal = clockInfoStr.replace("\n", "");
            ret = getClockInfo(clockInfoStrReal);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return ret;
        }
    }

    private List<ClockInfo> getClockInfo(String clockTreeStr) {
        String[] temp = clockTreeStr.split(" ");
        List<String> clockTrees = new ArrayList();
        for (String s : temp) {
            if (!s.equals("")) {
                clockTrees.add(s);
            }
        }

        List<ClockInfo> clockInfos = new ArrayList();
        for (int i = 6; i < clockTrees.size(); i += 6) {
            ClockInfo clockInfo = new ClockInfo();
            clockInfo.clockName = clockTrees.get(i);
            clockInfo.enableCnt = clockTrees.get(i + 1);
            clockInfo.prepareCnt = clockTrees.get(i + 2);
            clockInfo.rate = clockTrees.get(i + 3);
            clockInfo.accuracy = clockTrees.get(i + 4);
            clockInfo.phase = clockTrees.get(i + 5);
            clockInfos.add(clockInfo);
        }
        return clockInfos;
    }

    private String inputStream2String(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }

    class ClockInfo {
        public String clockName;
        public String enableCnt;
        public String prepareCnt;
        public String rate;
        public String accuracy;
        public String phase;

        @Override
        public String toString() {
            return "clockName:" + clockName + ",enable_cnt:" + enableCnt + ", prepare_cnt:" +
                    prepareCnt + ",rate:" + rate + ",accuray:" + accuracy + ",phase:" + phase;
        }
    }

    private String generateFile(boolean isDualDisplay) {
        String fileName = "";

        if (isDualDisplay) {
            if (mBoard.equals("i.MX6QP_SABRESD")) {
                fileName = "6qpSdClockTreeDual";
            } else if (mBoard.equals("i.MX6DL_SABRESD")) {
                fileName = "6dlSdClockTreeDual";
            } else if (mBoard.equals("i.MX6Q_SABRESD")) {
                fileName = "6qSdClockTreeDual";
            } else if (mBoard.equals("i.MX6Q_SABREAUTO")) {
                fileName = "6qArdClockTreeDual";
            } else if (mBoard.equals("i.MX6QP_SABREAUTO")) {
                fileName = "6qpArdClockTreeDual";
            } else if (mBoard.equals("i.MX6DL_SABREAUTO")) {
                fileName = "6dlArdClockTreeDual";
            }
        } else {
            if (mBoard.equals("i.MX6QP_SABRESD")) {
                fileName = "6qpSdClockTree";
            } else if (mBoard.equals("i.MX6DL_SABRESD")) {
                fileName = "6dlSdClockTree";
            } else if (mBoard.equals("i.MX6Q_SABRESD")) {
                fileName = "6qSdClockTree";
            } else if (mBoard.equals("i.MX6Q_SABREAUTO")) {
                fileName = "6qArdClockTree";
            } else if (mBoard.equals("i.MX6QP_SABREAUTO")) {
                fileName = "6qpArdClockTree";
            } else if (mBoard.equals("i.MX6SL_EVK")) {
                fileName = "evk6slClockTree";
            } else if (mBoard.equals("i.MX6SX_SABREAUTO")) {
                fileName = "6sxArdClockTree";
            } else  if (mBoard.equals("i.MX7D_SABRESD") ) {
                fileName = "7dSdClockTree";
            } else if (mBoard.equals("i.MX6DL_SABREAUTO")) {
                fileName = "6dlArdClockTree";
            } else if (mBoard.equals("i.MX6SX_SABRESD")) {
                fileName = "6sxSdClockTree";
            }
        }
        return fileName;
    }
}

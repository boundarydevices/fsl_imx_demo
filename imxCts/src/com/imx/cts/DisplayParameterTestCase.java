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

import com.imx.cts.IMxCtsAssetsTestCase;
import com.imx.cts.utils.CmdUtil;
import com.imx.AssetsActivity;

public class DisplayParameterTestCase extends IMxCtsAssetsTestCase {

    public void testFb0BitsPerPixel() {

        String expected = "";

        String cmd = "cat /sys/class/graphics/fb0/bits_per_pixel";
        CmdUtil.CmdResult actualResult = CmdUtil.execCommand(cmd);

        if (actualResult.errorMsg.contains("No such file or directory")) {
            fail("Does not support Fb0");
        }

        String actual = actualResult.successMsg;
        expected = mActivity.getFb0BitPerPiexl(mBoard);
        assertEquals("Fb0 bits_per_pixel is wrong", expected, actual);
    }

    public void testFb0Mode() {
        String expected = "#";

        String cmd = getFb0ModeCommand();
        CmdUtil.CmdResult actualResult = CmdUtil.execCommand(cmd);

        if (actualResult.errorMsg.contains("No such file or directory")) {
            fail("Does not support Fb0");
        }

        String actual = actualResult.successMsg;
        expected = mActivity.getFb0Mode(mBoard);
        /* String "actual": U:1024x768p-60 or S:1024x768p-60
           String "expected": 1024x768p-60 or   1024x768p-60,
           we don't care the first char(U/S), so cut the string
           from third character*/
        actual = actual.substring(2, actual.length());
        assertEquals("Fb0 Mode is wrong", expected, actual);
    }

    public void testFb2BitsPerPixel() {

        String expected = "";

        String cmd = "cat /sys/class/graphics/fb2/bits_per_pixel";
        CmdUtil.CmdResult actualResult = CmdUtil.execCommand(cmd);

        if (actualResult.errorMsg.contains("No such file or directory")) {
            return;
        }

        String actual = actualResult.successMsg;
        expected = mActivity.getFb2BitPerPiexl(mBoard);
        assertEquals("Fb2 bits_per_pixel is wrong", expected, actual);
    }

    public void testFb2Mode() {
        String expected = "#";

        String cmd = "cat /sys/class/graphics/fb2/mode";
        CmdUtil.CmdResult actualResult = CmdUtil.execCommand(cmd);

        if (actualResult.errorMsg.contains("No such file or directory")) {
            return;
        }

        String actual = actualResult.successMsg;
        expected = mActivity.getFb2Mode(mBoard);
        /* String "actual": U:1024x768p-60 or S:1024x768p-60
           String "expected": 1024x768p-60 or   1024x768p-60,
           we don't care the first char(U/S), so cut the string
           from third character*/
        actual = actual.substring(2, actual.length());
        assertEquals("Fb2 Mode is wrong", expected, actual);
    }

    private String getFb0ModeCommand() {
        return mBoard.equals("i.MX7D_SABRESD") ||
               mBoard.equals("i.MX6SX_SABREAUTO") ||
               mBoard.equals("i.MX6SX_SABRESD") ||
               mBoard.equals("i.MX6SL_EVK") ? "cat /sys/class/graphics/fb0/modes" : "cat /sys/class/graphics/fb0/mode";
    }
}

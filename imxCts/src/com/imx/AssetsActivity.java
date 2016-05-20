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
package com.imx;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by acer-pc on 2016/5/15.
 */
public class AssetsActivity extends Activity {

    private static final String TAG = "AssetsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public String getFb0BitPerPiexl(String board) {
        return getValueFromLine(board, "fb0_bits_per_pixel");
    }

    public String getFb0Mode(String board) {
        return getValueFromLine(board, "fb0_mode");
    }

    public String getFb2BitPerPiexl(String board) {
        return getValueFromLine(board, "fb2_bits_per_pixel");
    }

    public String getFb2Mode(String board) {
        return getValueFromLine(board, "fb2_mode");
    }

    public InputStream getClockTree(String path) {
        try {
            return getAssets().open(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getValueFromLine(String board, String path) {
        String expected = "";
        try {
            InputStreamReader inputReader = new InputStreamReader(getResources().
                    getAssets().open(path));
            expected = getValueFromInput(inputReader, board);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return expected;
        }
    }

    public static String getValueFromInput(InputStreamReader inputReader, String board) throws IOException {
        BufferedReader bufReader = new BufferedReader(inputReader);
        String line = "";
        String ret = "";
        while ((line = bufReader.readLine()) != null) {
            String[] lineArr = line.split("#");
            if (lineArr[0].equals(board)) {
                ret = lineArr[1];
            }
        }
        return ret;
    }
}

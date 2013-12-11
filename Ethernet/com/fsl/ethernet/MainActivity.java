/*
 * Copyright (C) 2013 Freescale Semiconductor, Inc.
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

package com.fsl.ethernet;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.DialogInterface;
import com.fsl.ethernet.EthernetDevInfo;
import android.view.View.OnClickListener;
import android.text.method.ScrollingMovementMethod;

public class MainActivity extends Activity {
    private EthernetEnabler mEthEnabler;
    private EthernetConfigDialog mEthConfigDialog;
    private Button mBtnConfig;
    private Button mBtnCheck;
    private EthernetDevInfo  mSaveConfig;
    private String TAG = "MainActivity";
    private static String Mode_dhcp = "dhcp";
    private boolean shareprefences_flag = false;
    private boolean first_run = true;
    public static final String FIRST_RUN = "ethernet";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ethernet_configure);
        SharedPreferences sp = getSharedPreferences("ethernet",
                Context.MODE_WORLD_WRITEABLE);
        first_run = sp.getBoolean(FIRST_RUN, false);
        if (!first_run){
        Editor editor = sp.edit();
        try {
            editor.putBoolean(FIRST_RUN, true);
            editor.putString("conn_mode",EthernetDevInfo.ETHERNET_CONN_MODE_DHCP);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            editor.commit();
        }
        mEthEnabler = new EthernetEnabler(this);
        mEthConfigDialog = new EthernetConfigDialog(this, mEthEnabler);
        mEthEnabler.setConfigDialog(mEthConfigDialog);
        addListenerOnBtnConfig();
        addListenerOnBtnCheck();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void addListenerOnBtnConfig() {
        mBtnConfig = (Button) findViewById(R.id.btnConfig);

        mBtnConfig.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEthConfigDialog.show();
            }
        });
    }

    public void addListenerOnBtnCheck() {
        mBtnConfig = (Button) findViewById(R.id.btnCheck);

        mBtnConfig.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView text = (TextView) findViewById(R.id.tvConfig);
                text.setMovementMethod(ScrollingMovementMethod.getInstance());
                mSaveConfig = mEthEnabler.getManager().getSavedConfig();
                if (mSaveConfig != null) {
                    final String config_detail = "IP address : " + mEthEnabler.getManager().getSharedPreIpAddress() + "\n"
                            + "DNS address: " + mEthEnabler.getManager().getSharedPreDnsAddress() + "\n"
                            + "IP mode    : " + mEthEnabler.getManager().getSharedPreMode() + "\n";
                    text.setText(config_detail);
                }
            }
        });
    }
}

/*
 * Copyright (C) 2014 Freescale Semiconductor, Inc.
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

import android.net.ConnectivityManager;
import android.net.Proxy;
import android.net.LinkProperties;
import android.net.ProxyProperties;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class EthernetAdvDialog extends AlertDialog implements DialogInterface.OnClickListener,View.OnClickListener{

    private final String TAG = "EthernetAdvDialog";
    private static String Mode_dhcp = "dhcp";
    private Context mContext;
    private EthernetEnabler mEthEnabler;
    private View mView;
    private EditText mProxyIp;
    private EditText mProxyPort;
    private EditText mProxyExclusionList;
    private LinkProperties mLinkProperties;
    private ProxyProperties mHttpProxy;
    private static ConnectivityManager sConnectivityManager = null;

    protected EthernetAdvDialog(Context context,EthernetEnabler Enabler) {
        super(context);
        mContext = context;
        mEthEnabler = Enabler;
        LinkProperties mLinkProperties = new LinkProperties();
        buildDialogContent(context);
    }

    public int buildDialogContent(Context context) {
        this.setTitle(R.string.eth_advanced_title);
        this.setView(mView = getLayoutInflater().inflate(R.layout.advanced_setting, null));

        mProxyIp = (EditText)mView.findViewById(R.id.proxy_address_edit);
        mProxyPort = (EditText)mView.findViewById(R.id.proxy_port_edit);
        mProxyExclusionList = (EditText)mView.findViewById(R.id.proxy_exclusionlist);

        this.setInverseBackgroundForced(true);
        this.setButton(BUTTON_POSITIVE, context.getText(R.string.menu_save), this);
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), this);
        mProxyIp.setText(mEthEnabler.getManager().getSharedPreProxyAddress(),TextView.BufferType.EDITABLE);
        mProxyPort.setText(mEthEnabler.getManager().getSharedPreProxyPort(),TextView.BufferType.EDITABLE);
        mProxyExclusionList.setText(mEthEnabler.getManager().getSharedPreProxyExclusionList(),TextView.BufferType.EDITABLE);
        this.setInverseBackgroundForced(true);
        this.setButton(BUTTON_POSITIVE, context.getText(R.string.menu_save), this);
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), this);
        return 0;
    }

    public void handle_saveconf() {
        EthernetDevInfo info = new EthernetDevInfo();
        String [] DevName = mEthEnabler.getManager().getDeviceNameList();
        info.setIfName(DevName[0]);
        info.setConnectMode(mEthEnabler.getManager().getSharedPreMode());
        info.setIpAddress(mEthEnabler.getManager().getSharedPreIpAddress());
        info.setDnsAddr(mEthEnabler.getManager().getSharedPreDnsAddress());
        info.setProxyAddr(mProxyIp.getText().toString());
        info.setProxyPort(mProxyPort.getText().toString());
        info.setProxyExclusionList(mProxyExclusionList.getText().toString());
        mEthEnabler.getManager().updateDevInfo(info);
        mEthEnabler.getManager().setProxy();
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                handle_saveconf();
                break;
            case BUTTON_NEGATIVE:
                //Don't need to do anything
                break;
            default:
                Log.e(TAG,"Unknow button");
        }
    }

    @Override
    public void onClick(View arg0) {
    // TODO Auto-generated method stub

    }


  @Override
    public void show() {
          super.show();
          mProxyIp.setText(mEthEnabler.getManager().getSharedPreProxyAddress(),TextView.BufferType.EDITABLE);
          mProxyPort.setText(mEthEnabler.getManager().getSharedPreProxyPort(),TextView.BufferType.EDITABLE);
          mProxyExclusionList.setText(mEthEnabler.getManager().getSharedPreProxyExclusionList(),TextView.BufferType.EDITABLE);
    }
}

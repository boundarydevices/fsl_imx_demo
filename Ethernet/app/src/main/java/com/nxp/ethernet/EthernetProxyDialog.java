/*
 * Copyright 2021 NXP
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
package com.nxp.ethernet;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class EthernetProxyDialog extends AlertDialog implements DialogInterface.OnClickListener, View.OnClickListener {

    private final String TAG = "EthernetProxyDialog";
    private final EthernetEnabler mEthEnabler;
    private EditText mProxyIp;
    private EditText mProxyPort;
    private EditText mProxyExclusionList;
    private LinearLayout mConfigWindow;
    private CheckBox mProxyEnableCheckBox;

    protected EthernetProxyDialog(Context context, EthernetEnabler Enabler) {
        super(context);
        mEthEnabler = Enabler;
        buildDialogContent(context);
    }

    public void buildDialogContent(Context context) {
        this.setTitle(R.string.eth_advanced_title);
        View mView;
        this.setView(mView = getLayoutInflater().inflate(R.layout.proxy_setting, null));

        mProxyIp = (EditText) mView.findViewById(R.id.proxy_address_edit);
        mProxyPort = (EditText) mView.findViewById(R.id.proxy_port_edit);
        mProxyExclusionList = (EditText) mView.findViewById(R.id.proxy_exclusionlist);
        mProxyEnableCheckBox = (CheckBox) mView.findViewById(R.id.proxy_enable_checkbox);
        mConfigWindow = (LinearLayout) mView.findViewById(R.id.enterprise_wrapper);
        mProxyEnableCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mConfigWindow.setVisibility(View.VISIBLE);
            } else {
                mConfigWindow.setVisibility(View.GONE);
            }
            mProxyIp.setEnabled(isChecked);
            mProxyPort.setEnabled(isChecked);
            mProxyExclusionList.setEnabled(isChecked);
        });


        this.setInverseBackgroundForced(true);
        this.setButton(BUTTON_POSITIVE, context.getText(R.string.menu_save), this);
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), this);
        mProxyIp.setText(mEthEnabler.getManager().getSharedPreProxyAddress(), TextView.BufferType.EDITABLE);
        mProxyPort.setText(mEthEnabler.getManager().getSharedPreProxyPort(), TextView.BufferType.EDITABLE);
        mProxyExclusionList.setText(mEthEnabler.getManager().getSharedPreProxyExclusionList(), TextView.BufferType.EDITABLE);
        if (mEthEnabler.getManager().getSharedPreProxyAddress() == null) {
            mProxyEnableCheckBox.setChecked(false);
            mConfigWindow.setVisibility(View.GONE);
        } else {
            mProxyEnableCheckBox.setChecked(true);
            mConfigWindow.setVisibility(View.VISIBLE);
        }
        this.setInverseBackgroundForced(true);
        this.setButton(BUTTON_POSITIVE, context.getText(R.string.menu_save), this);
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), this);
    }

    public void saveconf() {
        if (mEthEnabler.getManager().isEthernetConnect()) {
            EthernetDevInfo info = new EthernetDevInfo();
            String[] DevName = mEthEnabler.getManager().getDeviceNameList();
            info.setIfName(DevName[0]);
            info.setConnectMode(mEthEnabler.getManager().getSharedPreMode());
            info.setIpAddress(mEthEnabler.getManager().getSharedPreIpAddress());
            info.setDnsAddr(mEthEnabler.getManager().getSharedPreDnsAddress());

            if (mProxyEnableCheckBox.isChecked()) {
                info.setProxyAddr(mProxyIp.getText().toString());
                info.setProxyPort(mProxyPort.getText().toString());
                info.setProxyExclusionList(mProxyExclusionList.getText().toString());
            } else {
                info.setProxyAddr("");
                info.setProxyPort("");
                info.setProxyExclusionList("");
            }

            mEthEnabler.getManager().updateDevInfo(info);
            mEthEnabler.getManager().setProxy();
        } else {
            Toast.makeText(this.getContext(), R.string.show_connect_ethernet, Toast.LENGTH_SHORT).show();
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                saveconf();
                break;
            case BUTTON_NEGATIVE:
                dialog.cancel();
                break;
            default:
                Log.e(TAG, "Unknown button");
        }
    }

    @Override
    public void onClick(View arg0) {
        // TODO Auto-generated method stub

    }
}

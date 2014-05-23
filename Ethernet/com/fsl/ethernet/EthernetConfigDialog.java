/*
 * Copyright (C) 2013-2014 Freescale Semiconductor, Inc.
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

import android.view.View;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import com.fsl.ethernet.EthernetDevInfo;
import android.widget.LinearLayout;
import android.widget.CompoundButton;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
/**
 * Created by B38613 on 13-8-5.
 */
public class EthernetConfigDialog extends AlertDialog implements
        DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener, View.OnClickListener {
    private final String TAG = "EtherenetSettings";
    private static final boolean localLOGV = true;
    private Context mContext;
    private EthernetEnabler mEthEnabler;
    private View mView;
    private Spinner mDevList;
    private TextView mDevs;
    private RadioButton mConTypeDhcp;
    private RadioButton mConTypeManual;
    private EditText mIpaddr;
    private EditText mDns;
    private LinearLayout ip_dns_setting;
    private static String Mode_dhcp = "dhcp";

    public EthernetConfigDialog(Context context, EthernetEnabler Enabler) {
        super(context);
        mContext = context;
        mEthEnabler = Enabler;
        buildDialogContent(context);
    }
    public int buildDialogContent(Context context) {
        this.setTitle(R.string.eth_config_title);
        this.setView(mView = getLayoutInflater().inflate(R.layout.eth_configure, null));
        mDevs = (TextView) mView.findViewById(R.id.eth_dev_list_text);
        mDevList = (Spinner) mView.findViewById(R.id.eth_dev_spinner);
        mConTypeDhcp = (RadioButton) mView.findViewById(R.id.dhcp_radio);
        mConTypeManual = (RadioButton) mView.findViewById(R.id.manual_radio);
        mIpaddr = (EditText)mView.findViewById(R.id.ipaddr_edit);
        mDns = (EditText)mView.findViewById(R.id.eth_dns_edit);
        ip_dns_setting = (LinearLayout)mView.findViewById(R.id.ip_dns_setting);

        if (mEthEnabler.getManager().isConfigured()) {
            EthernetDevInfo info = mEthEnabler.getManager().getSavedConfig();
            if (mEthEnabler.getManager().getSharedPreMode().equals(Mode_dhcp)) {
                mConTypeDhcp.setChecked(true);
                mConTypeManual.setChecked(false);
                ip_dns_setting.setVisibility(View.GONE);
            } else {
                mConTypeDhcp.setChecked(false);
                mConTypeManual.setChecked(true);
                ip_dns_setting.setVisibility(View.VISIBLE);
                mIpaddr.setText(mEthEnabler.getManager().getSharedPreIpAddress(),TextView.BufferType.EDITABLE);
                mDns.setText(mEthEnabler.getManager().getSharedPreDnsAddress(),TextView.BufferType.EDITABLE);
            }
        } else {
            mConTypeDhcp.setChecked(true);
            mConTypeManual.setChecked(false);
            ip_dns_setting.setVisibility(View.GONE);
        }
        mConTypeManual.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                ip_dns_setting.setVisibility(View.VISIBLE);
            }
        });
        mConTypeDhcp.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                ip_dns_setting.setVisibility(View.GONE);
            }
        });

        this.setInverseBackgroundForced(true);
        this.setButton(BUTTON_POSITIVE, context.getText(R.string.menu_save), this);
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), this);
        String[] Devs = mEthEnabler.getManager().getDeviceNameList();
        if (Devs != null) {
            if (localLOGV)
                Log.d(TAG, "found device: " + Devs[0]);
            updateDevNameList(Devs);
        }
        return 0;
    }

    public void handle_saveconf() {
        EthernetDevInfo info = new EthernetDevInfo();
        info.setIfName(mDevList.getSelectedItem().toString());
        if (localLOGV)
            Log.d(TAG, "Config device for " + mDevList.getSelectedItem().toString());
        if (mConTypeManual.isChecked()) {
            if ((mIpaddr.getText().toString().equals(""))&&(mDns.getText().toString().equals("")))
            {
                Toast.makeText(this.getContext(), R.string.show_need_setting,Toast.LENGTH_SHORT).show();
            }else{
                info.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_MANUAL);
                info.setIpAddress(mIpaddr.getText().toString());
                info.setDnsAddr(mDns.getText().toString());
            }
        } else {
            info.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_DHCP);
            info.setIpAddress(null);
            info.setDnsAddr(null);
        }

        info.setProxyAddr(mEthEnabler.getManager().getSharedPreProxyAddress());
        info.setProxyPort(mEthEnabler.getManager().getSharedPreProxyPort());
        info.setProxyExclusionList(mEthEnabler.getManager().getSharedPreProxyExclusionList());
        mEthEnabler.getManager().updateDevInfo(info);
        mEthEnabler.setEthEnabled();
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

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void onClick(View v) {

    }
    public void updateDevNameList(String[] DevList) {
        if (DevList != null) {
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                    getContext(), android.R.layout.simple_spinner_item, DevList);
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            mDevList.setAdapter(adapter);
        }

    }

}


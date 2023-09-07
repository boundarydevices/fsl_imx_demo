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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class EthernetIpDialog extends AlertDialog
        implements DialogInterface.OnClickListener,
                AdapterView.OnItemSelectedListener,
                View.OnClickListener {
    private final String TAG = "EthernetIpSetting";
    private static final boolean DBG = true;
    private final EthernetEnabler mEthEnabler;
    private Spinner mDevList;
    private RadioButton mConTypeManual;
    private EditText mIpaddr;
    private EditText mDns;
    private EditText mPrefixLength;
    private EditText mGateway;
    private LinearLayout ip_dns_setting;

    public EthernetIpDialog(Context context, EthernetEnabler Enabler) {
        super(context);
        mEthEnabler = Enabler;
        buildDialogContent(context);
    }

    public void buildDialogContent(Context context) {
        this.setTitle(R.string.eth_config_title);
        View mView;
        this.setView(mView = getLayoutInflater().inflate(R.layout.eth_configure, null));
        mDevList = (Spinner) mView.findViewById(R.id.eth_dev_spinner);
        RadioButton mConTypeDhcp = (RadioButton) mView.findViewById(R.id.dhcp_radio);
        mConTypeManual = (RadioButton) mView.findViewById(R.id.manual_radio);
        mIpaddr = (EditText) mView.findViewById(R.id.ipaddr_edit);
        mDns = (EditText) mView.findViewById(R.id.eth_dns_edit);
        mPrefixLength = (EditText) mView.findViewById(R.id.network_prefix_length_edit);
        mGateway = (EditText) mView.findViewById(R.id.eth_gateway_edit);

        ip_dns_setting = (LinearLayout) mView.findViewById(R.id.ip_dns_setting);

        if (mEthEnabler.getManager().isConfigured()) {
            String mode_dhcp = "dhcp";
            if (mEthEnabler.getManager().getSharedPreMode().equals(mode_dhcp)) {
                mConTypeDhcp.setChecked(true);
                mConTypeManual.setChecked(false);
                ip_dns_setting.setVisibility(View.GONE);
            } else {
                mConTypeDhcp.setChecked(false);
                mConTypeManual.setChecked(true);
                ip_dns_setting.setVisibility(View.VISIBLE);
                mIpaddr.setText(
                        mEthEnabler.getManager().getSharedPreIpAddress(),
                        TextView.BufferType.EDITABLE);
                mDns.setText(
                        mEthEnabler.getManager().getSharedPreDnsAddress(),
                        TextView.BufferType.EDITABLE);
                mPrefixLength.setText(
                        mEthEnabler.getManager().getSharedPrefixLength(),
                        TextView.BufferType.EDITABLE);
                mGateway.setText(
                        mEthEnabler.getManager().getSharedPreGateway(),
                        TextView.BufferType.EDITABLE);
            }
        } else {
            mConTypeDhcp.setChecked(true);
            mConTypeManual.setChecked(false);
            ip_dns_setting.setVisibility(View.GONE);
        }

        mConTypeManual.setOnClickListener(v -> ip_dns_setting.setVisibility(View.VISIBLE));
        mConTypeDhcp.setOnClickListener(v -> ip_dns_setting.setVisibility(View.GONE));

        this.setInverseBackgroundForced(true);
        this.setButton(BUTTON_POSITIVE, context.getText(R.string.menu_save), this);
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), this);
        String[] Devs = mEthEnabler.getManager().getDeviceNameList();
        if (Devs != null) {
            if (DBG) Log.d(TAG, "found device: " + Devs[0]);
            updateDevNameList(Devs);
        }
    }

    public void saveconf() {
        EthernetDevInfo info = new EthernetDevInfo();
        info.setIfName(mDevList.getSelectedItem().toString());
        if (DBG) Log.d(TAG, "Config device for " + mDevList.getSelectedItem().toString());
        if (mConTypeManual.isChecked()) {
            if ((mIpaddr.getText().toString().equals(""))
                    || (mPrefixLength.getText().toString().equals(""))
                    || mGateway.getText().toString().equals("")) {
                Toast.makeText(this.getContext(), R.string.show_need_setting, Toast.LENGTH_SHORT)
                        .show();
                return;
            } else {
                info.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_MANUAL);
                info.setIpAddress(mIpaddr.getText().toString());
                info.setDnsAddr(mDns.getText().toString());
                info.setPrefixLength(mPrefixLength.getText().toString());
                info.setGateway(mGateway.getText().toString());
            }
        } else {
            info.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_DHCP);
            info.setIpAddress(null);
            info.setDnsAddr(null);
        }

        //        info.setProxyAddr(mEthEnabler.getManager().getSharedPreProxyAddress());
        //        info.setProxyPort(mEthEnabler.getManager().getSharedPreProxyPort());
        //
        // info.setProxyExclusionList(mEthEnabler.getManager().getSharedPreProxyExclusionList());

        mEthEnabler.getManager().updateDevInfo(info);
        mEthEnabler.setEthEnabled();
        // configHandler.post(new ConfigHandler(info));
    }

    //    class ConfigHandler implements Runnable {
    //        EthernetDevInfo info;
    //
    //        public ConfigHandler(EthernetDevInfo info) {
    //            this.info = info;
    //        }
    //
    //        public void run() {
    //            mEthEnabler.getManager().updateDevInfo(info);
    //            mEthEnabler.setEthEnabled();
    //        }
    //    }

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

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}

    public void onNothingSelected(AdapterView<?> parent) {}

    public void onClick(View v) {}

    public void updateDevNameList(String[] DevList) {
        if (DevList != null) {
            ArrayAdapter<CharSequence> adapter =
                    new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, DevList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mDevList.setAdapter(adapter);
        }
    }
}

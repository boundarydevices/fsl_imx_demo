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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.InetAddresses;
import android.net.IpConfiguration;
import android.net.IpConfiguration.*;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.net.module.util.ProxyUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LocalEthernetManager {
    public static final String TAG = "LocalEthernetManager";

    public EthernetManager mEthernetManager;
    private final Context mContext;
    private final String[] DevName;

    private final ConnectivityManager mConnMgr;

    // Allows underscore char to supports proxies that do not
    // follow the spec
    private static final String HC = "a-zA-Z0-9\\_";

    // Matches blank input, ips, and domain names
    private static final String HOSTNAME_REGEXP =
            "^$|^[" + HC + "]+(\\-[" + HC + "]+)*(\\.[" + HC + "]+(\\-[" + HC + "]+)*)*$";
    private static final Pattern HOSTNAME_PATTERN;
    private static final String EXCLUSION_REGEXP =
            "$|^(\\*)?\\.?[" + HC + "]+(\\-[" + HC + "]+)*(\\.[" + HC + "]+(\\-[" + HC + "]+)*)*$";
    private static final Pattern EXCLUSION_PATTERN;

    static {
        HOSTNAME_PATTERN = Pattern.compile(HOSTNAME_REGEXP);
        EXCLUSION_PATTERN = Pattern.compile(EXCLUSION_REGEXP);
    }

    public LocalEthernetManager(Context context) {
        mContext = context;

        DevName = new String[1];
        DevName[0] = "eth0";

        mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mEthernetManager = (EthernetManager) context.getSystemService(EthernetManager.class);

    }

    /**
     * check if the ethernet service has been configured.
     * @return {@code true} if configured {@code false} otherwise
     */
    public boolean isConfigured() {
        return (getSharedPreMode() != null);
    }

    /**
     * Return the saved ethernet configuration
     * @return ethernet interface configuration on success, {@code null} on failure
     */
    public synchronized EthernetDevInfo getSavedConfig() {
        if (!isConfigured())
            return null;
        EthernetDevInfo info = new EthernetDevInfo();
        info.setConnectMode(getSharedPreMode());
        info.setIfName(DevName[0]);
        if (info.getConnectMode().equals(EthernetDevInfo.ETHERNET_CONN_MODE_DHCP)) {
            updateDevInfo(getDhcpInfo());
        }
        info.setIpAddress(getSharedPreIpAddress());
        info.setDnsAddr(getSharedPreDnsAddress());
        info.setGateway(getSharedPreGateway());
        info.setPrefixLength(getSharedPrefixLength());
        info.setProxyAddr(getSharedPreProxyAddress());
        info.setProxyPort(getSharedPreProxyPort());
        info.setProxyExclusionList(getSharedPreProxyExclusionList());

        Log.d(TAG, "---------Static IP address =" + info.getIpAddress());
        Log.d(TAG, "---------Static IP dns =" + info.getDnsAddr());
        Log.d(TAG, "---------Static IP gateway =" + info.getGateway());
        Log.d(TAG, "---------Static IP prefixlength =" + info.getPrefixLength());

        return info;
    }

    public String[] getDeviceNameList() {
        return mEthernetManager.getAvailableInterfaces();
    }

    void configureInterface(EthernetDevInfo info) {
        if (info.getConnectMode().equals(EthernetDevInfo.ETHERNET_CONN_MODE_DHCP)) {
            IpConfiguration mIpConfiguration = new IpConfiguration();
            mIpConfiguration.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
            mEthernetManager.setConfiguration(DevName[0], mIpConfiguration);
        } else {
            try {
                IpConfiguration mIpConfiguration = new IpConfiguration();
                mIpConfiguration.setIpAssignment(IpConfiguration.IpAssignment.STATIC);

                final StaticIpConfiguration.Builder staticIpBuilder =
                        new StaticIpConfiguration.Builder();

                String ipAddr = info.getIpAddress();
                if (TextUtils.isEmpty(ipAddr)) {
                    Log.d(TAG,"Static IP configuration failed with empty IP address");
                }

                Log.d(TAG, "---------Static IP address =" + ipAddr);

                Inet4Address inetAddr = null;
                // InetAddress inetAddress = null;
                try {
                    inetAddr = (Inet4Address) InetAddresses.parseNumericAddress(ipAddr);
                    // inetAddress = InetAddresses.parseNumericAddress(ipAddr);
                } catch (IllegalArgumentException | ClassCastException e) {
                    Log.d(TAG,"Static IP configuration failed with address parse error");
                }

                if (inetAddr == null || inetAddr.equals(Inet4Address.ANY)) {
                    Log.d(TAG,"Static IP configuration failed with inetAddr error");
                }

                try {
                    int networkPrefixLength = Integer.parseInt(info.getPrefixLength());
                    Log.d(TAG,"--------networkPrefixLength =" + networkPrefixLength);

                    if (networkPrefixLength < 0 || networkPrefixLength > 32) {
                        Log.d(TAG,"Static IP configuration failed with PrefixLength parse error");
                    }
                    staticIpBuilder.setIpAddress(new LinkAddress(inetAddr, networkPrefixLength));
                    // staticIpBuilder.setIpAddress(new LinkAddress(inetAddress, networkPrefixLength));
                } catch (NumberFormatException e) {
                    Log.d(TAG,"Static IP configuration failed with ipaddress set error");
                }

                String gateway = info.getGateway();
                if (!TextUtils.isEmpty(gateway)) {
                    try {
                        staticIpBuilder.setGateway(InetAddresses.parseNumericAddress(gateway));
                    } catch (IllegalArgumentException | ClassCastException e) {
                        Log.d(TAG,"Static IP configuration failed with gateway set error");
                    }
                }

                final ArrayList<InetAddress> dnsServers = new ArrayList<>();
                String dns1 = info.getDnsAddr();
                if (!TextUtils.isEmpty(dns1)) {
                    try {
                        dnsServers.add(InetAddresses.parseNumericAddress(dns1));
                    } catch (IllegalArgumentException | ClassCastException e) {
                        Log.d(TAG,"Static IP configuration failed with dns error");
                    }
                }
                staticIpBuilder.setDnsServers(dnsServers);

                mIpConfiguration.setStaticIpConfiguration(staticIpBuilder.build());
                mEthernetManager.setConfiguration(DevName[0], mIpConfiguration);
                Log.d(TAG,"Static IP configuration succeeded");
            } catch (IllegalStateException e) {
                Log.e(TAG,"Static IP configuration failed with IllegalStateException: " + e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG,"Wrong Static IP: " + e);
                Toast.makeText(mContext, "Illegal address inputted. You can not access the Internet.",Toast.LENGTH_SHORT).show();
            } catch (Exception err) {
                Log.e(TAG, "Exception in setting Static IP");
                Toast.makeText(mContext, "We got exception when set the static IP.",Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "set ip manually " + info.toString());
            updateDevInfo(info);
        }
    }

    public EthernetDevInfo getDhcpInfo() {
        EthernetDevInfo ethinfo = new EthernetDevInfo();
        String [] DevName = getDeviceNameList();
        ethinfo.setIfName(DevName[0]);
        ethinfo.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_DHCP);
        String ip;
        try {
            Network network = mConnMgr.getActiveNetwork();
            ip = mConnMgr.getLinkProperties(network).getAddresses().toString();
        } catch (Exception err) {
            ip = "[]";
            Log.w(TAG, "get Dhcp Info error:" + err.toString());
        }
        if (!ip.equals("[]"))
            ethinfo.setIpAddress(ip.substring(2, ip.length()-1));
        String dns = " ";
        try {
            Network network = mConnMgr.getActiveNetwork();
            for( InetAddress d : mConnMgr.getLinkProperties(network).getDnsServers()) {
                String temp = d.toString();
                dns = temp.substring(1, temp.length()-1);
                break;
            }
        } catch (Exception err) {
            Log.w(TAG, "get Dhcp Info error:" + err.toString());
        }
        ethinfo.setDnsAddr(dns);// now only use dns1, need optimization later here.
        String proxyAddress = getSharedPreProxyAddress();
        String proxyPort = getSharedPreProxyPort();
        String proxyExclusionList=getSharedPreProxyExclusionList();
        ethinfo.setProxyAddr(proxyAddress);
        ethinfo.setProxyPort(proxyPort);
        ethinfo.setProxyExclusionList(proxyExclusionList);
        return ethinfo;
    }

    public void resetInterface() {
        /*
         * This will guide us to enabled the enabled device
         */
        EthernetDevInfo info = getSavedConfig();
        if (info != null && isConfigured()) {
            configureInterface(info);
        } else {
            //First boot using AOSP dhcp
            updateDevInfo(getDhcpInfo());
        }
    }

    /**
     * update a ethernet interface information
     * @param info  the interface infomation
     */
    public synchronized void updateDevInfo(EthernetDevInfo info) {
        sharedPreferencesStore(info);
    }

    public SharedPreferences sharedPreferences(){
        return this.mContext.getSharedPreferences("ethernet",
                Context.MODE_PRIVATE);
    }

    public void sharedPreferencesStore(EthernetDevInfo info){
        Editor editor = sharedPreferences().edit();
        try {
            editor.putString("conn_mode",info.getConnectMode());
            editor.putString("mIpaddr",info.getIpAddress());
            editor.putString("mDns",info.getDnsAddr());
            editor.putString("mGateway", info.getGateway());
            editor.putString("mPrefixLength", info.getPrefixLength());
            editor.putString("mProxyIp",info.getProxyAddr());
            editor.putString("mProxyPort", info.getProxyPort());
            editor.putString("mProxyExclusionList", info.getProxyExclusionList());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        editor.apply();

    }

    public String getSharedPreMode(){
        String temp = null;
        try {
            temp = sharedPreferences().getString("conn_mode",null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    public String getSharedPreIpAddress(){
        String temp = null;
        try {
            temp = sharedPreferences().getString("mIpaddr",null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    public String getSharedPreDnsAddress(){
        String temp = null;
        try {
            temp = sharedPreferences().getString("mDns",null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    public String getSharedPreGateway(){
        String temp = null;
        try {
            temp = sharedPreferences().getString("mGateway",null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    public String getSharedPrefixLength() {
        String temp = null;
        try {
            temp = sharedPreferences().getString("mPrefixLength",null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    public String getSharedPreProxyAddress(){
        String temp = null;
        try {
            temp = sharedPreferences().getString("mProxyIp",null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    public String getSharedPreProxyPort(){
        String temp = null;
        try {
            temp = sharedPreferences().getString("mProxyPort",null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    public String getSharedPreProxyExclusionList(){
        String temp = null;
        try {
            temp = sharedPreferences().getString("mProxyExclusionList",null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * validate syntax of hostname and port entries
     *
     * @return 0 on success, string resource ID on failure
     */
    public static int validate(String hostname, String port, String exclList) {
        Matcher match = HOSTNAME_PATTERN.matcher(hostname);
        String[] exclListArray = exclList.split(",");

        if (!match.matches()) return -1;

        for (String excl : exclListArray) {
            Matcher m = EXCLUSION_PATTERN.matcher(excl);
            if (!m.matches()) return -1;
        }

        if (hostname.length() > 0 && port.length() == 0) {
            return -1;
        }

        if (port.length() > 0) {
            if (hostname.length() == 0) {
                return -1;
            }
            int portVal = -1;
            try {
                portVal = Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                return -1;
            }
            if (portVal <= 0 || portVal > 0xFFFF) {
                return -1;
            }
        }
        return 0;
    }

    public void setProxy(){
        IpConfiguration mIpConfiguration = new IpConfiguration();
        boolean hasProxySettings = true;

        if (getSharedPreProxyAddress() == null || getSharedPreProxyPort() == null) {
            hasProxySettings = false;
        }

        mIpConfiguration.setProxySettings(hasProxySettings
                ? IpConfiguration.ProxySettings.STATIC : IpConfiguration.ProxySettings.NONE);

        if (hasProxySettings) {
            Network network = mConnMgr.getActiveNetwork();
            LinkProperties lp = mConnMgr.getLinkProperties(network);
            if (lp == null)
                return;

            int port = 0;
            String exclusionList = null;
            String host = null;
            int result;
            try {
                host = getSharedPreProxyAddress();
                port = Integer.parseInt(getSharedPreProxyPort());
                exclusionList = getSharedPreProxyExclusionList();
                result = validate(host, getSharedPreProxyPort(), exclusionList);
            } catch (NumberFormatException e) {
                result = -1;
            }
            if (result == 0) {
                mIpConfiguration.setHttpProxy(ProxyInfo.buildDirectProxy(host, port,
                        ProxyUtils.exclusionStringAsList(exclusionList)));
            }
        } else {
            mIpConfiguration.setHttpProxy(null);
        }
    }

    public boolean isEthernetConnect(){
        NetworkInfo networkInfo = mConnMgr.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        } else {
            return networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET;
        }
    }
}

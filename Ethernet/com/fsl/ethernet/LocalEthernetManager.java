/*
 * Copyright (C) 2013-2015 Freescale Semiconductor, Inc.
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Iterator;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.content.Context;
import android.provider.Settings;
import android.os.ServiceManager;
import android.os.IBinder;
import android.content.ContentResolver;
import android.os.INetworkManagementService;
import android.net.ConnectivityManager;
import android.net.NetworkUtils;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.LinkProperties;
import android.net.InterfaceConfiguration;
import android.net.ProxyInfo;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;
import android.net.StaticIpConfiguration;
import android.net.IpConfiguration;
import android.net.IpConfiguration.*;
import java.util.ArrayList;
import java.net.Inet4Address;
import java.lang.Integer;
/**
 * Created by B38613 on 9/27/13.
 */
public class LocalEthernetManager {
    public static final String TAG = "LocalEthernetManager";

    public  android.net.EthernetManager mEthManager;
    private Context mContext;
    private String[] DevName;

    private ConnectivityManager mConnMgr;

    public LocalEthernetManager(Context context) {
        mContext = context;

        DevName = new String[1];
        DevName[0] = "eth0";

        mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mEthManager = (android.net.EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
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
        info.setNetMask(getSharedPreNetMask());
        info.setProxyAddr(getSharedPreProxyAddress());
        info.setProxyPort(getSharedPreProxyPort());
        info.setProxyExclusionList(getSharedPreProxyExclusionList());
        return info;
    }

    public String[] getDeviceNameList() {
        return mEthManager.getAvailableInterfaces();
    }

    void configureInterface(EthernetDevInfo info) {
        if (info.getConnectMode().equals(EthernetDevInfo.ETHERNET_CONN_MODE_DHCP)) {
            IpConfiguration ipcfg = new IpConfiguration();
            ipcfg.ipAssignment = IpAssignment.DHCP;
            mEthManager.setConfiguration(DevName[0], ipcfg);
        } else {
            InterfaceConfiguration ifcg = null;
            Log.d(TAG, "Static IP =" + info.getIpAddress());
            try {
                IpConfiguration ipcfg = mEthManager.getConfiguration(DevName[0]);
                ipcfg.ipAssignment = IpAssignment.STATIC;

                Inet4Address iNetmask = (Inet4Address)InetAddress.getByName(info.getNetMask());
                int netmask = NetworkUtils.inetAddressToInt(iNetmask);
                int prefixLength = NetworkUtils.netmaskIntToPrefixLength(netmask);
                LinkAddress ipAddr = new LinkAddress(info.getIpAddress()+"/"+ Integer.toString(prefixLength));
                InetAddress gwAddr = InetAddress.getByName(info.getGateway());
                StaticIpConfiguration config = new StaticIpConfiguration();
                config.ipAddress = ipAddr;
                config.gateway = gwAddr;
                if (info.getDnsAddr() != null)
                    config.dnsServers.add(InetAddress.getByName(info.getDnsAddr()));

                ipcfg.staticIpConfiguration = config;
                mEthManager.setConfiguration(DevName[0] ,ipcfg);
                Log.d(TAG,"Static IP configuration succeeded");
            } catch (UnknownHostException e){
                Log.e(TAG,"Static IP configuration failed: " + e);
            } catch (IllegalStateException e) {
                Log.e(TAG,"Static IP configuration fialed: " + e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG,"Wrong Static IP: " + e);
                Toast.makeText(mContext, "Illegal address inputed. You can not access the Internet.",Toast.LENGTH_SHORT).show();
            } catch (Exception err) {
                Log.e(TAG, "Exception in setting Static IP");
                Toast.makeText(mContext, "We got exception when set the static IP.",Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "set ip manually " + info.toString());
            updateDevInfo(info);
        }
    }

    public String interMask2String(int prefixLength) {
        String netMask = null;
        int inetMask = prefixLength;

        int part = inetMask / 8;
        int remainder = inetMask % 8;
        int sum = 0;

        for (int i = 8; i > 8 - remainder; i--) {
            sum = sum + (int) Math.pow(2, i - 1);
        }

        if (part == 0) {
            netMask = sum + ".0.0.0";
        } else if (part == 1) {
            netMask = "255." + sum + ".0.0";
        } else if (part == 2) {
            netMask = "255.255." + sum + ".0";
        } else if (part == 3) {
            netMask = "255.255.255." + sum;
        } else if (part == 4) {
            netMask = "255.255.255.255";
        }
        return netMask;
    }

    public EthernetDevInfo getStaticInfo() {
        EthernetDevInfo infotemp = new EthernetDevInfo();
        String [] DevName = getDeviceNameList();
        infotemp.setIfName(DevName[0]);
        infotemp.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_MANUAL);
        String ip;

        StaticIpConfiguration staticIpConfiguration = mEthManager.getConfiguration(DevName[0]).getStaticIpConfiguration();

        LinkAddress ipAddr = staticIpConfiguration.ipAddress;
        InetAddress gwAddr = staticIpConfiguration.gateway;
        ArrayList<InetAddress> dnsServers = staticIpConfiguration.dnsServers;

        if( ipAddr !=null ) {
            infotemp.setIpAddress(ipAddr.getAddress().getHostAddress());
            infotemp.setNetMask(interMask2String(ipAddr.getPrefixLength()));
        }

        if(gwAddr !=null) {
            infotemp.setGateway(gwAddr.getHostAddress());
        }

        infotemp.setDnsAddr(dnsServers.get(0).getHostAddress());
        //now only use dns1, need optimization later here.
        //if(dnsServers.size() > 1) {
        //    infotemp.setDnsAddr(dnsServers.get(1).getHostAddress());
        //}

        String proxyAddress = getSharedPreProxyAddress();
        String proxyPort = getSharedPreProxyPort();
        String proxyExclusionList=getSharedPreProxyExclusionList();
        infotemp.setProxyAddr(proxyAddress);
        infotemp.setProxyPort(proxyPort);
        infotemp.setProxyExclusionList(proxyExclusionList);
        return infotemp;
    }

    public EthernetDevInfo getDhcpInfo() {
        EthernetDevInfo infotemp = new EthernetDevInfo();
        String [] DevName = getDeviceNameList();
        infotemp.setIfName(DevName[0]);
        infotemp.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_DHCP);
        String ip;
        try {
            ip = mConnMgr.getLinkProperties(ConnectivityManager.TYPE_ETHERNET).getAddresses().toString();
        } catch (Exception err) {
            ip = "[]";
            Log.w(TAG, "getDhcpInfo error:" + err.toString());
        }
        if (ip != "[]" )
            infotemp.setIpAddress(ip.substring(2, ip.length()-1));
        String dns = " ";
        try {
        int i = 0;
            for( InetAddress d : mConnMgr.getLinkProperties(ConnectivityManager.TYPE_ETHERNET).getDnsServers()) {
                String temp = d.toString();
                if (temp != null)
                    dns = temp.substring(1, temp.length()-1);
                break;
            }
        } catch (Exception err) {
            Log.w(TAG, "getDhcpInfo error:" + err.toString());
        }
        infotemp.setDnsAddr(dns);// now only use dns1, need optimization later here.
        String proxyAddress = getSharedPreProxyAddress();
        String proxyPort = getSharedPreProxyPort();
        String proxyExclusionList=getSharedPreProxyExclusionList();
        infotemp.setProxyAddr(proxyAddress);
        infotemp.setProxyPort(proxyPort);
        infotemp.setProxyExclusionList(proxyExclusionList);
        return infotemp;
    }

    /**
     * reset ethernet interface
     * @return true
     * @throws UnknownHostException
     */
    public void resetInterface() {
        /*
         * This will guide us to enabled the enabled device
         */
        String mInterfaceName ;
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
        SharedPreferences sp = this.mContext.getSharedPreferences("ethernet",
                Context.MODE_PRIVATE);
        return sp;
    }

    public void sharedPreferencesStore(EthernetDevInfo info){
        Editor editor = sharedPreferences().edit();
        try {
            editor.putString("conn_mode",info.getConnectMode());
            editor.putString("mIpaddr",info.getIpAddress());
            editor.putString("mDns",info.getDnsAddr());
            editor.putString("mGateway", info.getGateway());
            editor.putString("mNetMask", info.getNetMask());
            editor.putString("mProxyIp",info.getProxyAddr());
            editor.putString("mProxyPort", info.getProxyPort());
            editor.putString("mProxyExclusionList", info.getProxyExclusionList());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        editor.commit();

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

    public String getSharedPreNetMask(){
        String temp = null;
        try {
            temp = sharedPreferences().getString("mNetMask",null);
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

    public void setProxy(){
        String exclusionList = null;
        exclusionList=getSharedPreProxyExclusionList();
        if (getSharedPreProxyAddress() == null || getSharedPreProxyPort() == null) {
            mConnMgr.setGlobalProxy(null);
            return;
        }
        LinkProperties lp = mConnMgr.getLinkProperties(ConnectivityManager.TYPE_ETHERNET);
        if (lp == null)
            return;
        int port = 0;
        try {
            port = Integer.parseInt(getSharedPreProxyPort());
        } catch(NumberFormatException e){
        }
        ProxyInfo proxyProperties =
            new ProxyInfo(getSharedPreProxyAddress(), port, exclusionList);
        mConnMgr.setGlobalProxy(null);
        mConnMgr.setGlobalProxy(proxyProperties);
    }

    public void initProxy(){
        EthernetDevInfo info = getSavedConfig();
        if (info != null) {
            updateDevInfo(info);
            setProxy();
        }
    }

    public boolean isEthernetConnect(){
        return mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET).isConnected();
    }
}

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
import android.net.DhcpInfo;
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
import android.os.SystemProperties;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;
import android.net.LinkAddress;
import android.net.StaticIpConfiguration;
import android.net.IpConfiguration;
import android.net.IpConfiguration.*;
import java.util.ArrayList;
import java.net.Inet4Address;
import java.lang.Integer;

/**
 * Created by B38613 on 9/27/13.
 */
public class EthernetManager {
    public static final String TAG = "EthernetManager";

    public static final int ETHERNET_DEVICE_SCAN_RESULT_READY = 0;
    public static final String ETHERNET_STATE_CHANGED_ACTION =
            "android.net.ethernet.ETHERNET_STATE_CHANGED";
    public static final String NETWORK_STATE_CHANGED_ACTION =
            "android.net.ethernet.STATE_CHANGE";

    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    public static final String EXTRA_ETHERNET_STATE = "ETHERNET_state";
    public static final String EXTRA_PREVIOUS_ETHERNET_STATE = "previous_ETHERNET_state";
    /**
     * The lookup key for a {@link android.net.LinkProperties} object associated with the
     * Ethernet network. Retrieve with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     * @hide
     */
    public static final String EXTRA_LINK_PROPERTIES = "linkProperties";

    /**
     * The lookup key for a {@link android.net.LinkCapabilities} object associated with the
     * Ethernet network. Retrieve with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     * @hide
     */
    public static final String EXTRA_LINK_CAPABILITIES = "linkCapabilities";

    public static final int ETHERNET_STATE_UNKNOWN = 0;
    public static final int ETHERNET_STATE_DISABLED = 1;
    public static final int ETHERNET_STATE_ENABLED = 2;
    private static final int ETHERNET_HAS_CONFIG = 1;


    /** @hide */
    public static final int DATA_ACTIVITY_NONE         = 0x00;
    /** @hide */
    public static final int DATA_ACTIVITY_IN           = 0x01;
    /** @hide */
    public static final int DATA_ACTIVITY_OUT          = 0x02;
    /** @hide */
    public static final int DATA_ACTIVITY_INOUT        = 0x03;

    private android.net.EthernetManager ethernetService;
    private Context mContext;
    private String[] DevName;
    private int mEthState= ETHERNET_STATE_UNKNOWN;
    private INetworkManagementService mNMService;
    private DhcpInfo mDhcpInfo;
    private String mode;
    private String ip_address;
    private String dns_address;
    private ConnectivityManager mConnMgr;

    public EthernetManager(Context context) {
        mContext = context;

        DevName = new String[1];
        String sIfaceMatch = context.getResources().getString(
                        com.android.internal.R.string.config_ethernet_iface_regex);


        DevName[0] = "eth0";//mTracker.getLinkProperties().getInterfaceName();

        mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        ethernetService = (android.net.EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
        mNMService = INetworkManagementService.Stub.asInterface(b);

        try {
            final String[] ifaces = mNMService.listInterfaces();
            for (String iface : ifaces) {
                if (iface.matches(sIfaceMatch)) {
                    DevName[0] = iface;
                    Log.d(TAG, "We will operate iface:" + DevName[0]);
                    break;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Could not get list of interfaces " + e);
        }


        HandlerThread dhcpThread = new HandlerThread("DHCP Handler Thread");
        dhcpThread.start();
        mDhcpInfo = new DhcpInfo();
    }

    /**
     * check if the ethernet service has been configured.
     * @return {@code true} if configured {@code false} otherwise
     */
    public boolean isConfigured() {
        //return "1".equals(SystemProperties.get("net."+ DevName[0] + ".config", "0"));
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

    /**
     * update a ethernet interface information
     * @param info  the interface infomation
     */
    private int scanDevice() {
        return 1;
    }

    /**
     * get all the ethernet device names
     * @return interface name list on success, {@code null} on failure
     */
    public String[] getDeviceNameList() {
        return (scanDevice() > 0) ? DevName : null;
    }

    private void setInterfaceUp(String InterfaceName)
    {
        try {
            mNMService.setInterfaceUp(InterfaceName);
        } catch (RemoteException re){
            Log.e(TAG,"Set interface up failed: " + re);
        } catch (IllegalStateException e) {
            Log.e(TAG,"Set interface up fialed: " + e);
        }

    }

    void configureInterface(EthernetDevInfo info) {
        if (info.getConnectMode().equals(EthernetDevInfo.ETHERNET_CONN_MODE_DHCP)) {
            IpConfiguration ipcfg = new IpConfiguration();
            ipcfg.ipAssignment = IpAssignment.DHCP;
            ethernetService.setConfiguration(ipcfg);
        } else {
            InterfaceConfiguration ifcg = null;
            Log.d(TAG, "Static IP =" + info.getIpAddress());
            try {
                IpConfiguration ipcfg = ethernetService.getConfiguration();
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
                  ethernetService.setConfiguration(ipcfg);

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
            SystemProperties.set("net.dns1", info.getDnsAddr());
            SystemProperties.set("net." + info.getIfName() + ".dns1",info.getDnsAddr());
            updateDevInfo(info);
        }
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
        SystemProperties.set("net.dns1", info.getDnsAddr());
        SystemProperties.set("net." + info.getIfName() + ".dns1",info.getDnsAddr());
        SystemProperties.set("net." + info.getIfName() + ".dns2", "0.0.0.0");
    }

    public SharedPreferences sharedPreferences(){
        SharedPreferences sp = this.mContext.getSharedPreferences("ethernet",
                Context.MODE_WORLD_WRITEABLE);
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
            SystemProperties.set("rw.HTTP_PROXY", "");
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
        SystemProperties.set("rw.HTTP_PROXY", proxyProperties.getHost() + ":" + proxyProperties.getPort());
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

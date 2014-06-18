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
import android.net.EthernetDataTracker;
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
import android.net.ProxyProperties;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemProperties;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;
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

    private Context mContext;
    private String[] DevName;
    private int mEthState= ETHERNET_STATE_UNKNOWN;
    private EthernetDataTracker mTracker;
    private INetworkManagementService mNMService;
    private DhcpInfo mDhcpInfo;
    private Handler mTrackerTarget;
    private String mode;
    private String ip_address;
    private String dns_address;
    private ConnectivityManager mConnMgr;

    public EthernetManager(Context context) {
        mContext = context;
        mTracker = EthernetDataTracker.getInstance();

        DevName = new String[1];

        DevName[0] = "eth0";//mTracker.getLinkProperties().getInterfaceName();

        mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNMService = INetworkManagementService.Stub.asInterface(b);
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

            try {
                mNMService.setInterfaceDown(info.getIfName());
                mNMService.clearInterfaceAddresses(info.getIfName());
                mNMService.setInterfaceUp(info.getIfName());
            } catch (RemoteException re){
                Log.e(TAG,"DHCP configuration failed: " + re);
            } catch (IllegalStateException e) {
                Log.e(TAG,"DHCP configuration fialed: " + e);
            }
        } else {
            InterfaceConfiguration ifcg = null;
            Log.d(TAG, "Static IP =" + info.getIpAddress());
            try {
                ifcg = mNMService.getInterfaceConfig(info.getIfName());
                ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(
                        info.getIpAddress()), 24));
                ifcg.setInterfaceUp();
                mNMService.setInterfaceConfig(info.getIfName(), ifcg);

                Log.d(TAG,"Static IP configuration succeeded");
            } catch (RemoteException re){
                Log.e(TAG,"Static IP configuration failed: " + re);
            } catch (IllegalStateException e) {
                Log.e(TAG,"Static IP configuration fialed: " + e);
            }catch (IllegalArgumentException e) {
                Log.e(TAG,"Wrong Static IP: " + e);
                Toast.makeText(mContext, "Illegal address inputed. You can not access the Internet.",Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "set ip manually " + info.toString());
            SystemProperties.set("net.dns1", info.getDnsAddr());
            SystemProperties.set("net." + info.getIfName() + ".dns1",info.getDnsAddr());
            SystemProperties.set("net." + info.getIfName() + ".dns2", "0.0.0.0");
            updateDevInfo(info);
        }
    }

    public EthernetDevInfo getDhcpInfo() {
        EthernetDevInfo infotemp = new EthernetDevInfo();
        String [] DevName = getDeviceNameList();
        infotemp.setIfName(DevName[0]);
        infotemp.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_DHCP);
        String ip;
        ip = mConnMgr.getLinkProperties(ConnectivityManager.TYPE_ETHERNET).getAddresses().toString();
        Log.d(TAG, "===========IP=" + ip);
        if (ip != "[]" )
            infotemp.setIpAddress(ip.substring(2, ip.length()-1));
        String dns = " ";
        int i = 0;
        for( InetAddress d : mConnMgr.getLinkProperties(ConnectivityManager.TYPE_ETHERNET).getDnses()) {
            String temp = d.toString();
            if (temp != null)
                dns = temp.substring(1, temp.length()-1);
            break;
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
            synchronized (this) {
                mInterfaceName = info.getIfName();
                Log.d(TAG, "reset device " + mInterfaceName);
                NetworkUtils.resetConnections(mInterfaceName, NetworkUtils.RESET_ALL_ADDRESSES);
            }
            if (!NetworkUtils.stopDhcp(mInterfaceName)) {
                Log.d(TAG, "Could not stop DHCP");
            }
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
        SystemProperties.set("net." + info.getIfName() + ".config", "1");
        SystemProperties.set("net." + info.getIfName() + ".mode", info.getConnectMode());
        SystemProperties.set("net." + info.getIfName() + ".ip", info.getIpAddress());
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
        LinkProperties lp = mTracker.getLinkProperties();
        if (lp == null)
            return;
        int port = 0;
        try {
            port = Integer.parseInt(getSharedPreProxyPort());
        } catch(NumberFormatException e){
        }
        ProxyProperties proxyProperties =
            new ProxyProperties(getSharedPreProxyAddress(), port, exclusionList);
        mConnMgr.setGlobalProxy(null);
        mConnMgr.setGlobalProxy(proxyProperties);
        Log.i(TAG,"=============getHttpProxy==============" + proxyProperties);
    }

    public void initProxy(){
        EthernetDevInfo info = getSavedConfig();
        if (info != null) {
            updateDevInfo(info);
            setProxy();
	}
    }
}

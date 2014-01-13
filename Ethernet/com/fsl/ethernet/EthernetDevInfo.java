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
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
/**
 * Created by B38613 on 9/27/13.
 */
public class EthernetDevInfo {
    /**
     * The ethernet interface is configured by dhcp
     */
    public static final String ETHERNET_CONN_MODE_DHCP= "dhcp";
    /**
     * The ethernet interface is configured manually
     */
    public static final String ETHERNET_CONN_MODE_MANUAL = "manual";

    private String dev_name;
    private String ipaddr;
    private String netmask;
    private String route;
    private String dns;
    private String mode;
    private String proxyIp;
    private String proxyPort;
    private String proxyExclusionList;

    public EthernetDevInfo () {
        dev_name = null;
        ipaddr = null;
        dns = null;
        route = null;
        netmask = null;
        mode = ETHERNET_CONN_MODE_DHCP;
        proxyIp = null;
        proxyPort = null;
    }

    /**
     * save interface name into the configuration
     */
    public void setIfName(String ifname) {
        this.dev_name = ifname;
    }

    /**
     * Returns the interface name from the saved configuration
     * @return interface name
     */
    public String getIfName() {
        return this.dev_name;
    }

    public void setIpAddress(String ip) {
        this.ipaddr = ip;
    }

    public String getIpAddress() {
        return this.ipaddr;
    }

    public void setNetMask(String ip) {
        this.netmask = ip;
    }

    public String getNetMask( ) {
        return this.netmask;
    }

    public void setRouteAddr(String route) {
        this.route = route;
    }

    public String getRouteAddr() {
        return this.route;
    }

    public void setDnsAddr(String dns) {
        this.dns = dns;
    }

    public String getDnsAddr() {
        return this.dns;
    }

    public void setProxyAddr(String ip) {
        this.proxyIp = ip;
    }

    public String getProxyAddr() {
        return this.proxyIp;
    }

    public void setProxyPort(String port) {
        this.proxyPort = port;
    }

    public String getProxyPort() {
        return this.proxyPort;
    }

    public String getProxyExclusionList() {
        return proxyExclusionList;
    }

    public void setProxyExclusionList(String proxyExclusionList) {
        this.proxyExclusionList = proxyExclusionList;
    }
    /**
     * Set ethernet configuration mode
     * @param mode {@code ETHERNET_CONN_MODE_DHCP} for dhcp {@code ETHERNET_CONN_MODE_MANUAL} for manual configure
     * @return
     */
    public boolean setConnectMode(String mode) {
        if (mode.equals(ETHERNET_CONN_MODE_DHCP) || mode.equals(ETHERNET_CONN_MODE_MANUAL)) {
            this.mode = mode;
            return true;
        }
        return false;
    }

    public String getConnectMode() {
        return this.mode;
    }

}

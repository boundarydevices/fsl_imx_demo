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

public class EthernetDevInfo {
    /**
     * The ethernet interface is configured by dhcp
     */
    public static final String ETHERNET_CONN_MODE_DHCP= "dhcp";
    /**
     * The ethernet interface is configured manually
     */
    public static final String ETHERNET_CONN_MODE_MANUAL = "static";

    private String dev_name;
    private String ipaddr;
    private String prefixlength;
    private String dns;
    private String mode;
    private String proxyIp;
    private String proxyPort;
    private String proxyExclusionList;
    private String gateway;

    public EthernetDevInfo () {
        dev_name = null;
        ipaddr = null;
        dns = null;
        prefixlength = null;
        mode = ETHERNET_CONN_MODE_DHCP;
        proxyIp = null;
        proxyPort = null;
        gateway = null;
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

    public void setGateway(String gw) {
        this.gateway = gw;
    }

    public String getGateway() {
        return this.gateway;
    }

    public void setPrefixLength(String ip) {
        this.prefixlength = ip;
    }

    public String getPrefixLength( ) {
        return this.prefixlength;
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
     */
    public void setConnectMode(String mode) {
        if (mode.equals(ETHERNET_CONN_MODE_DHCP) || mode.equals(ETHERNET_CONN_MODE_MANUAL)) {
            this.mode = mode;
        }
    }

    public String getConnectMode() {
        return this.mode;
    }

}

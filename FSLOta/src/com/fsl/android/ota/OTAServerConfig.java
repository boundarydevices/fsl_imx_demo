/*
/* Copyright 2012-2015 Freescale Semiconductor, Inc.
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

package com.fsl.android.ota;

import android.os.SystemProperties;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import android.util.Log;
import android.os.Build;

// TODO: get the configure from a configure file.
public class OTAServerConfig {
	
	final String default_serveraddr = "10.192.224.88";
	final String default_protocol = "http";
	final int default_port = 10888;
	URL updatePackageURL;
	URL buildpropURL;
	URL payloadPropertiesURL;
	URL payloadURL;
	URL diffPayloadPropertiesURL;
	URL diffPayloadURL;
	boolean ab_slot = false;
	boolean is_diff_upgrade = false;
	String product;
	final String TAG = "OTA";
	final String configFile = "/vendor/etc/ota.conf";
	final String machineFile = "/sys/devices/soc0/machine";
	final String server_ip_config = "server";
	final String port_config_str = "port";
	final String android_nickname = "ota_folder_suffix";
	String machineString = null;
	public OTAServerConfig (String productname) throws MalformedURLException {
		if (loadConfigureFromFile(configFile, productname) == false)
			defaultConfigure(productname);
	}
	void readMachine() {
		File file = new File(machineFile);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			machineString = reader.readLine();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	boolean loadConfigureFromFile (String configFile, String product) {
		try {
			BuildPropParser parser = new BuildPropParser(new File(configFile), null);
			String server = parser.getProp(server_ip_config);
			String port_str = parser.getProp(port_config_str);
			String android_name = parser.getProp(android_nickname);
			String version_incremental = Build.VERSION.INCREMENTAL;
			int port = new Long(port_str).intValue();
			String fileaddr;
			String buildconfigAddr;
			String payloadPropertiesAddr;
			String payloadAddr;
			String diffPayloadPropertiesAddr;
			String diffPayloadAddr;

			readMachine();
			String version = SystemProperties.get("ro.build.version.release");
			fileaddr = new String(product + "_" + android_name + "_" + version + "/" + product + "-ota-" + version_incremental);
			buildconfigAddr = new String(product + "_" + android_name + "_" + version + "/" + "build.prop");
			payloadPropertiesAddr = new String(product + "_" + android_name + "_" + version + "/payload_properties-");
			payloadAddr = new String(product + "_" + android_name + "_" + version + "/payload-");
			diffPayloadPropertiesAddr = payloadPropertiesAddr;
			diffPayloadAddr = payloadAddr;
			String boottype = SystemProperties.get("ro.boot.storage_type");
			if (machineString.indexOf("i.MX6") != -1) {
			if (machineString.indexOf("DualLite") != -1) {
                              if (boottype.equals("nand"))
                                  {fileaddr = fileaddr + "-imx6dl_nand.zip";}
                              else
                              fileaddr = fileaddr + "-imx6dl.zip";
			} else if (machineString.indexOf("Quad") != -1) {
				if(machineString.indexOf("Plus") != -1){
                              		if (boottype.equals("nand"))
                                  		{fileaddr = fileaddr + "-imx6qp_nand.zip";}
                              		else
                              	  		fileaddr = fileaddr + "-imx6qp.zip";
				} else {
                                	if (boottype.equals("nand"))
                                  		{fileaddr = fileaddr + "-imx6q_nand.zip";}
                                	else
                                  		fileaddr = fileaddr + "-imx6q.zip";
				}
			} else if (machineString.indexOf("SoloLite") != -1) {
				fileaddr = fileaddr + "-imx6sl.zip";
			} else if (machineString.indexOf("SoloX") != -1) {
                               if (boottype.equals("nand"))
                                  {fileaddr = fileaddr + "-imx6sx_nand.zip";
                        }
                              else
			      fileaddr = fileaddr + "-imx6sx.zip";
			}
			} else if (machineString.indexOf("i.MX7ULP") != -1) {
			      fileaddr = fileaddr + "-imx7ulp.zip";
			} else if (machineString.indexOf("i.MX7D") != -1) {
			      fileaddr = fileaddr + "-imx7d.zip";
			} else if (machineString.indexOf("i.MX8MQ") != -1) {
			      ab_slot = true;
			      payloadPropertiesAddr = payloadPropertiesAddr + "imx8mq.txt";
			      payloadAddr = payloadAddr + "imx8mq.bin";
			      diffPayloadPropertiesAddr = diffPayloadPropertiesAddr + "imx8mq_diff.txt";
			      diffPayloadAddr = diffPayloadAddr + "imx8mq_diff.bin";
			} else if (machineString.indexOf("i.MX8QXP") != -1) {
			      ab_slot = true;
			      payloadPropertiesAddr = payloadPropertiesAddr + "imx8qxp.txt";
			      payloadAddr = payloadAddr + "imx8qxp.bin";
			      diffPayloadPropertiesAddr = diffPayloadPropertiesAddr + "imx8qxp_diff.txt";
			      diffPayloadAddr = diffPayloadAddr + "imx8qxp_diff.bin";
			} else if (machineString.indexOf("i.MX8QM") != -1) {
			      ab_slot = true;
			      payloadPropertiesAddr = payloadPropertiesAddr + "imx8qm.txt";
			      payloadAddr = payloadAddr + "imx8qm.bin";
			      diffPayloadPropertiesAddr = diffPayloadPropertiesAddr + "imx8qm_diff.txt";
			      diffPayloadAddr = diffPayloadAddr + "imx8qm_diff.bin";
			} else if (machineString.indexOf("i.MX8MM") != -1) {
			      ab_slot = true;
			      payloadPropertiesAddr = payloadPropertiesAddr + "imx8mm.txt";
			      payloadAddr = payloadAddr + "imx8mm.bin";
			      diffPayloadPropertiesAddr = diffPayloadPropertiesAddr + "imx8mm_diff.txt";
			      diffPayloadAddr = diffPayloadAddr + "imx8mm_diff.bin";
			}
			buildpropURL = new URL(default_protocol, server, port, buildconfigAddr);
			if (!ab_slot) {
				updatePackageURL = new URL(default_protocol, server, port, fileaddr);
			} else {
				payloadPropertiesURL = new URL(default_protocol, server, port, payloadPropertiesAddr);
				payloadURL = new URL(default_protocol, server, port, payloadAddr);
				diffPayloadPropertiesURL = new URL(default_protocol, server, port, diffPayloadPropertiesAddr);
				diffPayloadURL = new URL(default_protocol, server, port, diffPayloadAddr);
			}
		} catch (Exception e) {
			Log.e(TAG, "wrong format/error of OTA configure file.");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	void defaultConfigure(String productname) throws MalformedURLException
	{
		product = productname;
		String fileaddr = new String(product + "/" + product + ".ota.zip");
		String buildconfigAddr = new String(product + "/" + "build.prop"); 
		updatePackageURL = new URL(default_protocol, default_serveraddr, default_port, fileaddr );
		buildpropURL = new URL(default_protocol, default_serveraddr, default_port, buildconfigAddr);
		Log.d(TAG, "create a new server config: package url " + updatePackageURL.toString() + "port:" + updatePackageURL.getPort());
		Log.d(TAG, "build.prop URL:" + buildpropURL.toString());
	}
	
	public boolean ab_slot()
	{
		return ab_slot;
	}

	public boolean getDiffUpgrade()
	{
		return is_diff_upgrade;
	}

	public void setDiffUpgrade()
	{
		is_diff_upgrade = true;
	}

	public URL getPackageURL ()
	{
		if (ab_slot) {
			if (getDiffUpgrade())
				return diffPayloadURL;
			else
				return payloadURL;
		}
		else
			return updatePackageURL;
	}

	public URL getPayloadPropertiesURL ()
	{
		if (getDiffUpgrade())
			return diffPayloadPropertiesURL;
		else
			return payloadPropertiesURL;
	}

	public URL getBuildPropURL() { return buildpropURL; }

}

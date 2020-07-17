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

// TODO: get the configure from a configure file.
public class OTAServerConfig {
	
	final String default_serveraddr = "10.192.224.88";
	final String default_protocol = "http";
	final int default_port = 0;
	URL updatePackageURL;
	URL buildpropURL;
	String product;
	final String TAG = "OTA";
	final String configFile = "/system/etc/ota.conf";
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
			int port = new Long(port_str).intValue();
			String fileaddr;
			String buildconfigAddr;

			readMachine();
			String version = SystemProperties.get("ro.build.version.release");
			fileaddr = new String(product + "_" + android_name + "_" + version + "/" + product + ".ota.zip");
			buildconfigAddr = new String(product + "_" + android_name + "_" + version + "/" + "build.prop");
                        String boottype = SystemProperties.get("ro.boot.storage_type");
			if (machineString.indexOf("i.MX6") != -1) {
			if (machineString.indexOf("DualLite") != -1) {
                              if (boottype.equals("nand"))
                                  {fileaddr = fileaddr + ".imx6dl_nand";}
                              else
                              fileaddr = fileaddr + ".imx6dl";
			} else if (machineString.indexOf("Quad") != -1) {
				if(machineString.indexOf("Plus") != -1){
                              		if (boottype.equals("nand"))
                                  		{fileaddr = fileaddr + ".imx6qp_nand";}
                              		else
                              	  		fileaddr = fileaddr + ".imx6qp";
				} else {
                                	if (boottype.equals("nand"))
                                  		{fileaddr = fileaddr + ".imx6q_nand";}
                                	else
                                  		fileaddr = fileaddr + ".imx6q";
				}
			} else if (machineString.indexOf("SoloLite") != -1) {
				fileaddr = fileaddr + ".imx6sl";
			} else if (machineString.indexOf("SoloX") != -1) {
                               if (boottype.equals("nand"))
                                  {fileaddr = fileaddr + ".imx6sx_nand";
                        }
                              else
			      fileaddr = fileaddr + ".imx6sx";
			}
			} else if (machineString.indexOf("i.MX7ULP") != -1) {
			      fileaddr = fileaddr + ".imx7ulp";
			} else if (machineString.indexOf("i.MX7D") != -1) {
			      fileaddr = fileaddr + ".imx7d";
			}
			updatePackageURL = new URL(default_protocol, server, port, fileaddr);
			buildpropURL = new URL(default_protocol, server, port, buildconfigAddr);
		} catch (Exception e) {
			Log.e(TAG, "wrong format/error of OTA configure file.");
			return false;
		}
		
		return true;
	}
	
	public void setUpdatePackageURL(String url)
	{
		try {
			updatePackageURL = new URL(url);
		} catch (Exception e) {
			Log.e(TAG, "Wrong URL format " + url);
		}
	}

	void defaultConfigure(String productname) throws MalformedURLException
	{
		product = productname;
		String fileaddr = new String(product + "/" + product + ".ota.zip");
		String buildconfigAddr = new String(product + "/" + "build.prop");
		if (default_port > 0) {
			updatePackageURL = new URL(default_protocol, default_serveraddr, default_port, fileaddr);
			buildpropURL = new URL(default_protocol, default_serveraddr, default_port, buildconfigAddr);
		} else {
			updatePackageURL = new URL(default_protocol, default_serveraddr, fileaddr);
			buildpropURL = new URL(default_protocol, default_serveraddr, buildconfigAddr);
		}
		Log.d(TAG, "create a new server config: package url " + updatePackageURL.toString() + " port: " + updatePackageURL.getPort());
		Log.d(TAG, "build.prop URL: " + buildpropURL.toString());
	}
	
	public URL getPackageURL () { return updatePackageURL; }
	public URL getBuildPropURL() { return buildpropURL; }
	
}

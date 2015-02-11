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
	final int default_port = 10888;
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
			String fileaddr = new String(product + "_" + android_name + "/" + product + ".ota.zip");
			String buildconfigAddr = new String(product + "_" + android_name + "/" + "build.prop");

			readMachine();

                        String boottype = SystemProperties.get("ro.boot.storage_type");
			if (machineString.indexOf("DualLite") != -1) {
                               if (boottype.equals("sd"))
                                  {fileaddr = fileaddr + ".imx6dl_sd";}
                              else if (boottype.equals("nand"))
                                  {fileaddr = fileaddr + ".imx6dl_nand";}
                              else
                              fileaddr = fileaddr + ".imx6dl";
			} else if (machineString.indexOf("Quad") != -1) {
                               if (boottype.equals("sd"))
                                  {fileaddr = fileaddr + ".imx6q_sd";}
                              else if (boottype.equals("nand"))
                                  {fileaddr = fileaddr + ".imx6q_nand";}
                              else
                              fileaddr = fileaddr + ".imx6q";
			} else if (machineString.indexOf("SoloLite") != -1) {
				fileaddr = fileaddr + ".imx6sl";
			} else if (machineString.indexOf("SoloX") != -1) {
                               if (boottype.equals("nand"))
                                  {fileaddr = fileaddr + ".imx6sx_nand";
                        }
                              else
			      fileaddr = fileaddr + ".imx6sx";
			}

			updatePackageURL = new URL(default_protocol, server, port, fileaddr);
			buildpropURL = new URL(default_protocol, server, port, buildconfigAddr);
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
	
	public URL getPackageURL () { return updatePackageURL; }
	public URL getBuildPropURL() { return buildpropURL; }
	
}

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
	
	final String default_serveraddr = "10.193.108.180";
	final String default_protocol = "http";
	final int default_port = 10888;
	URL updatePackageURL;
	URL buildpropURL;
	URL diffbuildpropURL;
	URL payloadPropertiesURL;
	URL payloadURL;
	URL diffPayloadPropertiesURL;
	URL diffPayloadURL;
	boolean ab_slot = false;
	boolean is_diff_upgrade = false;
	String product;
	final String TAG = "OTA";
	final String configFile = "/vendor/etc/ota.conf";
	final String server_ip_config = "server";
	final String port_config_str = "port";
	final String android_nickname = "ota_folder_suffix";
	public OTAServerConfig (String productname) throws MalformedURLException {
		if (loadConfigureFromFile(configFile, productname) == false)
			defaultConfigure(productname);
	}

	boolean loadConfigureFromFile (String configFile, String product) {
		try {
			BuildPropParser parser = new BuildPropParser(new File(configFile), null);
			String server = parser.getProp(server_ip_config);
			String port_str = parser.getProp(port_config_str);
			String android_name = parser.getProp(android_nickname);
			String version_incremental = Build.VERSION.INCREMENTAL;
			int port = new Long(port_str).intValue();
			String ota_folder;
			String fileaddr;
			String buildconfigAddr;
			String diffbuildconfigAddr;
			String payloadPropertiesAddr;
			String payloadAddr;
			String diffPayloadPropertiesAddr;
			String diffPayloadAddr;

			String version = SystemProperties.get("ro.build.version.release");
			String platform = SystemProperties.get("ro.board.platform");
			ota_folder = new String(product + "_" + android_name + "_" + version + "/");
			fileaddr = new String(ota_folder + product + "-ota-" + version_incremental + ".zip");
			buildconfigAddr = new String(ota_folder + "build.prop");
			diffbuildconfigAddr = new String(ota_folder + "build_diff.prop");
			payloadPropertiesAddr = new String(ota_folder + "payload_properties.txt");
			diffPayloadPropertiesAddr = new String(ota_folder + "payload_properties_diff.txt");
			payloadAddr = new String(ota_folder + "payload.bin");
			diffPayloadAddr = new String(ota_folder + "payload_diff.bin");
			if (platform.indexOf("imx8") != -1) {
				ab_slot = true;
			}

			buildpropURL = new URL(default_protocol, server, port, buildconfigAddr);
			if (!ab_slot) {
				updatePackageURL = new URL(default_protocol, server, port, fileaddr);
				Log.d(TAG, "ota package: " + updatePackageURL.toString());
			} else {
				payloadPropertiesURL = new URL(default_protocol, server, port, payloadPropertiesAddr);
				payloadURL = new URL(default_protocol, server, port, payloadAddr);
				diffbuildpropURL = new URL(default_protocol, server, port, diffbuildconfigAddr);
				diffPayloadPropertiesURL = new URL(default_protocol, server, port, diffPayloadPropertiesAddr);
				diffPayloadURL = new URL(default_protocol, server, port, diffPayloadAddr);
				Log.d(TAG, "build.prop: " + buildpropURL.toString());
				Log.d(TAG, "payload.bin: " + payloadURL.toString());
				Log.d(TAG, "payload_properties.txt" + payloadPropertiesURL.toString());
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

	public URL getBuildPropDiffURL() { return diffbuildpropURL; }

}

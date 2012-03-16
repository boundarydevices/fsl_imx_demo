/*
/* Copyright 2012 Freescale Semiconductor, Inc.
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

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
	final String server_ip_config = "server";
	final String port_config_str = "port";
	
	public OTAServerConfig (String productname) throws MalformedURLException {
		
		if (loadConfigureFromFile(configFile, productname) == false)
			defaultConfigure(productname);
	}
	
	boolean loadConfigureFromFile (String configFile, String product) {
		try {
			BuildPropParser parser = new BuildPropParser(new File(configFile), null);
			String server = parser.getProp(server_ip_config);
			String port_str = parser.getProp(port_config_str);
			int port = new Long(port_str).intValue();
			String fileaddr = new String(product + "/" + product + ".ota.zip");
			String buildconfigAddr = new String(product + "/" + "build.prop"); 
			updatePackageURL = new URL(default_protocol, server, port, fileaddr);
			buildpropURL = new URL(default_protocol, server, port, buildconfigAddr);
		} catch (Exception e) {
			Log.e(TAG, "wrong format/error of OTA configure file.");
			e.printStackTrace();
			return false;
		}
		
		return false;
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

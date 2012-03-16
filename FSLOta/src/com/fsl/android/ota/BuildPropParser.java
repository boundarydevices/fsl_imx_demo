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

import java.io.*;
import java.util.*;

import android.content.Context;
import android.util.Log;

public class BuildPropParser {
    private HashMap<String, String> propHM = null;
    File tmpFile;
    Context mContext;
    
    final String TAG = "OTA";

    BuildPropParser(ByteArrayOutputStream out, Context context) {
    	mContext = context;
        propHM = new HashMap<String, String>();
        setByteArrayStream(out);
    }

    BuildPropParser(File file, Context context) throws IOException {
    	mContext = context;
        propHM = new HashMap<String, String>();
        setFile(file);
    }

    public HashMap<String, String> getPropMap()         { return propHM;};
    public String getProp(String propname) { 
    	if (propHM != null)
    		return (String) propHM.get(propname); 
    	else 
    		return null;
    }

    private void setByteArrayStream(ByteArrayOutputStream out) {
        try {
        	File tmpDir = null;
        	if (mContext != null)
        		tmpDir = mContext.getFilesDir();
        	Log.d(TAG, "tmpDir:"  + tmpDir.toString() +  "\n");
            tmpFile = File.createTempFile("buildprop", "ss", tmpDir);
            
            tmpFile.deleteOnExit();
            FileOutputStream o2 = new FileOutputStream(tmpFile);
            out.writeTo(o2);
            o2.close();
            setFile(tmpFile);
            tmpFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setFile(File file) throws IOException {
        try {
            FileReader reader = new FileReader(file);
            BufferedReader in = new BufferedReader(reader);
            String string;
            while ((string = in.readLine()) != null) {
                Scanner scan = new Scanner(string);
                scan.useDelimiter("=");
                try {
                    propHM.put(scan.next(), scan.next());
                } catch (NoSuchElementException e) {
                    continue;
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    public String getRelease() { 
    	if (propHM != null) 
    		return propHM.get("ro.build.version.release"); 
    	else 
    		return null;
    }
    public String getNumRelease()  {
    	if (propHM != null) 
    		return propHM.get("ro.build.version.incremental");
    	else
    		return null;
    }

}

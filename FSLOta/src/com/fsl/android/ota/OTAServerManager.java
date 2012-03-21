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

import java.net.*;
import java.security.GeneralSecurityException;
import java.io.*;

import android.content.*;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RecoverySystem;
import android.util.Log;


public class OTAServerManager  {
	public interface OTAStateChangeListener {
		
		final int STATE_IN_IDLE = 0;
		final int STATE_IN_CHECKED = 1; // state in checking whether new available.
		final int STATE_IN_DOWNLOADING = 2; // state in download upgrade package
		final int STATE_IN_UPGRADING = 3;  // In upgrade state
		
		final int MESSAGE_DOWNLOAD_PROGRESS = 4;
		final int MESSAGE_VERIFY_PROGRESS = 5;
		final int MESSAGE_STATE_CHANGE = 6;
		final int MESSAGE_ERROR = 7;
		
		// should be raise exception ? but how to do exception in async mode ?
		final int NO_ERROR = 0;
		final int ERROR_WIFI_NOT_AVALIBLE = 1;  // require wifi network, for OTA app.
		final int ERROR_CANNOT_FIND_SERVER = 2;
		final int ERROR_PACKAGE_VERIFY_FALIED = 3;
		final int ERROR_WRITE_FILE_ERROR = 4;
		final int ERROR_NETWORK_ERROR = 5;
		final int ERROR_PACKAGE_INSTALL_FAILED = 6;
		final int ERROR_PACKAGE_VERIFY_FAILED = 7;
		
		// results
		final int RESULTS_ALREADY_LATEST = 1;

		public void onStateOrProgress(int message, int error, Object info);
		
	}

	private OTAStateChangeListener mListener;	
	private OTAServerConfig mConfig;
	private BuildPropParser parser = null;
	long mCacheProgress = -1;
	boolean mStop = false;
	Context mContext;
	String mUpdatePackageLocation = "/cache/update.zip";
	String TAG = "OTA";
	Handler mSelfHandler;
	WakeLock mWakelock;
	
	
	public OTAServerManager(Context context) throws MalformedURLException {
		mConfig = new OTAServerConfig(Build.PRODUCT);
		PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		mWakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "OTA Wakelock");
		mContext = context;
	}

	public OTAStateChangeListener getmListener() {
		return mListener;
	}

	public void setmListener(OTAStateChangeListener mListener) {
		this.mListener = mListener;
	}
	
	public boolean checkNetworkOnline() {
		ConnectivityManager conMgr =  (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if ((conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)).isConnectedOrConnecting()) {
			return true;
		} else {
			return false;
		}
	}
	
	public void startCheckingVersion() {
		
		Log.v(TAG, "startCheckingVersion");
		
		if (checkURLOK(mConfig.getBuildPropURL()) == false) {
			if (this.mListener != null) {
				if (this.checkNetworkOnline())
					reportCheckingError(OTAStateChangeListener.ERROR_CANNOT_FIND_SERVER);
				else
					reportCheckingError(OTAStateChangeListener.ERROR_WIFI_NOT_AVALIBLE);
			}
			
			return;
		}
		
		parser = getTargetPackagePropertyList(mConfig.getBuildPropURL());
		
		if (parser != null) {
			if (this.mListener != null)
				this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_CHECKED, 
						OTAStateChangeListener.NO_ERROR, parser);
		} else {
			reportCheckingError(OTAStateChangeListener.ERROR_WRITE_FILE_ERROR);
		}
	}
	
	// return true if needs to upgrade
	public boolean compareLocalVersionToServer() {
		if (parser == null) {
			Log.d(TAG, "compareLocalVersion Without fetch remote prop list.");
			return false;
		}
		String localNumVersion = Build.VERSION.INCREMENTAL;
		Long buildutc = Build.TIME;
		Long remoteBuildUTC = (Long.parseLong(parser.getProp("ro.build.date.utc"))) * 1000;
		// *1000 because Build.java also *1000, align with it.
		Log.d(TAG, "Local Version:" + Build.VERSION.INCREMENTAL + "server Version:" + parser.getNumRelease());
		boolean upgrade = false;
		upgrade = remoteBuildUTC > buildutc;
		// here only check build time, in your case, you may also check build id, etc.
		Log.d(TAG, "remote BUILD TIME: " + remoteBuildUTC + " local build rtc:" + buildutc);
		return upgrade;
	}
	
	void publishDownloadProgress(long total, long downloaded) {
		//Log.v(TAG, "download Progress: total: " + total + "download:" + downloaded);
		Long progress = new Long((downloaded*100)/total);
		if (this.mListener != null && progress.longValue() != mCacheProgress) {
			this.mListener.onStateOrProgress(OTAStateChangeListener.MESSAGE_DOWNLOAD_PROGRESS,
					0, progress);
			mCacheProgress = progress.longValue();
		}
	}
	
	void reportCheckingError(int error) {
		if (this.mListener != null)
			this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_CHECKED, error, null);
	}
	
	void reportDownloadError(int error) {
		if (this.mListener != null)
			this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_DOWNLOADING, error, null);
	}
	
	void reportInstallError(int error) {
		if (this.mListener != null)
			this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_UPGRADING, error, null);
	}
	
	public long getUpgradePackageSize() {
		if (checkURLOK(mConfig.getPackageURL()) == false) {
			Log.e(TAG, "getUpgradePckageSize Failed");
			return -1;
		}
		
		URL url = mConfig.getPackageURL();
		URLConnection con;
		try {
			con = url.openConnection();
			return con.getContentLength();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public void onStop() {
		mStop = true;
	}
	
	public void startDownloadUpgradePackage() {
		
		Log.v(TAG, "startDownloadUpgradePackage()");

		if (checkURLOK(mConfig.getPackageURL()) == false) {
			if (this.mListener != null)
				reportDownloadError(OTAStateChangeListener.ERROR_CANNOT_FIND_SERVER);
			return;
		}


		File targetFile = new File(mUpdatePackageLocation);
		try {
			targetFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			reportDownloadError(OTAStateChangeListener.ERROR_WRITE_FILE_ERROR);
			return;
		}

		try {
			mWakelock.acquire();
			
			URL url = mConfig.getPackageURL();
			Log.d(TAG, "start downoading package:" + url.toString());
			URLConnection conexion = url.openConnection();
			conexion.setReadTimeout(10000);
			// this will be useful so that you can show a topical 0-100% progress bar

			int lengthOfFile = 96038693;
			lengthOfFile = conexion.getContentLength();			
			// download the file
			InputStream input = new BufferedInputStream(url.openStream());
			OutputStream output = new FileOutputStream(targetFile);
			
			Log.d(TAG, "file size:" + lengthOfFile);
			byte data[] = new byte[100 * 1024];
			long total = 0, count;
			while ((count = input.read(data)) >= 0 && !mStop) {
				total += count;
				
				// publishing the progress....
				publishDownloadProgress(lengthOfFile, total);
				output.write(data, 0, (int)count);
			}
			
			output.flush();
			output.close();
			input.close();
			if (this.mListener != null && !mStop)
				this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_DOWNLOADING, 0, null);
		} catch (IOException e) {
			e.printStackTrace();
			reportDownloadError(OTAStateChangeListener.ERROR_WRITE_FILE_ERROR);
		} finally {
			mWakelock.release();
			mWakelock.acquire(2);
		}
	}
	
	RecoverySystem.ProgressListener recoveryVerifyListener = new RecoverySystem.ProgressListener() {
		public void onProgress(int progress) {
			Log.d(TAG, "verify progress" + progress);
			if (mListener != null)
				mListener.onStateOrProgress(OTAStateChangeListener.MESSAGE_VERIFY_PROGRESS, 
						0, new Long(progress));
		}
	};
	
	public void startInstallUpgradePackage() {
		File recoveryFile = new File(mUpdatePackageLocation);
		
		// first verify package
         try {
        	 mWakelock.acquire();
        	 RecoverySystem.verifyPackage(recoveryFile, recoveryVerifyListener, null);
         } catch (IOException e1) {
        	 reportInstallError(OTAStateChangeListener.ERROR_PACKAGE_VERIFY_FALIED);
        	 e1.printStackTrace();
        	 return;
         } catch (GeneralSecurityException e1) {
        	 reportInstallError(OTAStateChangeListener.ERROR_PACKAGE_VERIFY_FALIED);
        	 e1.printStackTrace();
        	 return;
         } finally {
        	 mWakelock.release();
         }

         // then install package
         try {
        	 mWakelock.acquire();
      	   RecoverySystem.installPackage(mContext, recoveryFile);
         } catch (IOException e) {
      	   // TODO Auto-generated catch block
        	 reportInstallError(OTAStateChangeListener.ERROR_PACKAGE_INSTALL_FAILED);
        	 e.printStackTrace();
        	 return;
         } catch (SecurityException e){
        	 e.printStackTrace();
        	 reportInstallError(OTAStateChangeListener.ERROR_PACKAGE_INSTALL_FAILED);
        	 return;
         } finally {
        	 mWakelock.release();
         }
         // cannot reach here...

	}

	boolean checkURLOK(URL url) {
		try {
			HttpURLConnection.setFollowRedirects(false);
			
			HttpURLConnection con =  (HttpURLConnection) url.openConnection();
			
			con.setRequestMethod("HEAD");
			
			return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	// function: 
	// download the property list from remote site, and parse it to peroerty list.
	// the caller can parser this list and get information.
	BuildPropParser getTargetPackagePropertyList(URL configURL) {
		
		// first try to download the property list file. the build.prop of target image.
		try {
			URL url =  configURL;
			url.openConnection();
			InputStream reader = url.openStream();
			ByteArrayOutputStream writer = new ByteArrayOutputStream();
			byte[] buffer = new byte[153600];
			int totalBufRead = 0;
			int bytesRead;
			
			Log.d(TAG, "start download: " + url.toString() + "to buffer");
		
			while ((bytesRead = reader.read(buffer)) > 0) {
				writer.write(buffer, 0, bytesRead);
				buffer = new byte[153600];
				totalBufRead += bytesRead;
			}
			
		
		Log.d(TAG, "download finish:" + (new Integer(totalBufRead).toString()) + "bytes download");
		reader.close();
		
		BuildPropParser parser = new BuildPropParser(writer, mContext);
		
		return parser;
		
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean handleMessage(Message arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}

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

import java.net.*;
import java.security.GeneralSecurityException;
import java.io.*;
import java.util.List;
import java.util.ArrayList;

import android.os.SystemProperties;
import android.content.*;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RecoverySystem;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.util.Log;

public class OTAServerManager {
	public interface OTAStateChangeListener {
		
		final int STATE_IN_IDLE = 0;
		final int STATE_IN_CHECKED = 1; // state in checking whether new available.
		final int STATE_IN_DOWNLOADING = 2; // state in download upgrade package
		final int STATE_IN_UPGRADING = 3;  // In upgrade state
		
		final int MESSAGE_DOWNLOAD_PROGRESS = 4;
		final int MESSAGE_VERIFY_PROGRESS = 5;
		final int MESSAGE_STATE_CHANGE = 6;
		final int MESSAGE_ERROR = 7;
		final int MESSAGE_WAIT_REBOOT = 8;
		
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

	public enum OtaTypeSelect {
		NONE, FULL_OTA, DIFF_OTA, BOTH_OTA;
	}

	public class OTAUpdateEngineCallback extends UpdateEngineCallback {
		private OTAStateChangeListener mListener;

		public OTAUpdateEngineCallback(OTAStateChangeListener listener){
			mListener = listener;
		}

		public void onStatusUpdate(int status, float percent) {
			if (status == UpdateEngine.UpdateStatusConstants.DOWNLOADING) {
				publishDownloadProgress(100, (long)(percent*100));
			}
		}

		public void onPayloadApplicationComplete(int errorCode) {
			Log.d(TAG, "onPayloadApplicationComplete: errorCode: " + errorCode);
			if (errorCode == UpdateEngine.ErrorCodeConstants.SUCCESS) {
				if (this.mListener != null && !mStop)
					this.mListener.onStateOrProgress(OTAStateChangeListener.MESSAGE_WAIT_REBOOT, 0, null);
			}
		}
	}

	private OTAStateChangeListener mListener;	
	private OTAServerConfig mConfig;
	private BuildPropParser parser = null;
	private BuildPropParser parser_diff = null;
	long mCacheProgress = -1;
	boolean mStop = false;
	Context mContext;
	String mUpdatePackageLocation = "/cache/update.zip";
	String TAG = "OTA";
	Handler mSelfHandler;
	WakeLock mWakelock;
	UpdateEngine mUpdateEngine;
	String[] mUpdateHeader;
	
	public OTAServerManager(Context context) throws MalformedURLException {
		mConfig = new OTAServerConfig(Build.PRODUCT);
		PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		mWakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "OTA Wakelock");
		mContext = context;
		mUpdateEngine = new UpdateEngine();
	}

	public OTAStateChangeListener getmListener() {
		return mListener;
	}

	public void setmListener(OTAStateChangeListener mListener) {
		this.mListener = mListener;
	}
	
	public boolean checkNetworkOnline() {
		ConnectivityManager conMgr =  (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if (conMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET).isConnectedOrConnecting()||
			conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting()) {
			return true;
		} else {
			return false;
		}
	}	
	
	public void startCheckingVersion() {
		
		Log.v(TAG, "startCheckingVersion");
		if (checkURLOK(mConfig.getBuildPropURL()) == false) {
			if (this.mListener != null) {
				if (this.checkNetworkOnline()) {
					reportCheckingError(OTAStateChangeListener.ERROR_CANNOT_FIND_SERVER);
                                        Log.v(TAG, "error cannot find server!");
                                } 
				else {
					reportCheckingError(OTAStateChangeListener.ERROR_WIFI_NOT_AVALIBLE);
                                        Log.v(TAG, "error wifi or ethernet not avalible");
                                }  
			}
			
			return;
		}
		
		parser = getTargetPackagePropertyList(mConfig.getBuildPropURL());
		if (ab_slot()) {
			parser_diff = getTargetPackagePropertyList(mConfig.getBuildPropDiffURL());
		}
		
		if (parser != null) {
			if (this.mListener != null)
				this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_CHECKED, 
						OTAStateChangeListener.NO_ERROR, parser);
		} else {
			reportCheckingError(OTAStateChangeListener.ERROR_WRITE_FILE_ERROR);
		}
	}

	public boolean getUpdateHeader(URL propertiesURL)
	{
		try {
			URL url = propertiesURL;
			url.openConnection();
			InputStream is = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			List<String> lines = new ArrayList<String>();
			String line = null;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
			br.close();
			is.close();
			mUpdateHeader = lines.toArray(new String[lines.size()]);
			mUpdateEngine.bind(new OTAUpdateEngineCallback(this.mListener));

			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// return true if needs to upgrade
	public OtaTypeSelect compareLocalVersionToServer() {
		boolean diff_ota = false;
		boolean full_ota = false;
		Long remoteDiffBuildUTC = 0L;
		Long remoteDiffBuildBaseUTC = 0L;

		if (parser == null) {
			Log.d(TAG, "compareLocalVersion Without fetch remote prop list.");
			return OtaTypeSelect.NONE;
		}
		OtaTypeSelect upgrade = OtaTypeSelect.NONE;
		Long buildutc = Build.TIME;
		// *1000 because Build.java also *1000, align with it.
		Long remoteBuildUTC = (Long.parseLong(parser.getProp("ro.build.date.utc"))) * 1000;
		if (buildutc < remoteBuildUTC)
			full_ota = true;
		if (ab_slot()) {
			remoteDiffBuildUTC = (Long.parseLong(parser_diff.getProp("ro.build.date.utc"))) * 1000;
			remoteDiffBuildBaseUTC = (Long.parseLong(parser_diff.getProp("base.ro.build.date.utc"))) * 1000;
			if (buildutc < remoteDiffBuildUTC && buildutc.equals(remoteDiffBuildBaseUTC))
				diff_ota = true;
		}
		if (full_ota && diff_ota)
			upgrade = OtaTypeSelect.BOTH_OTA;
		else if (full_ota)
			upgrade = OtaTypeSelect.FULL_OTA;
		else if (diff_ota)
			upgrade = OtaTypeSelect.DIFF_OTA;
		// here only check build time, in your case, you may also check build id, etc.
		Log.d(TAG, "remote BUILD TIME: " + remoteBuildUTC + " remote DIFF BUILD TIME: " + remoteDiffBuildUTC + " remote DIFF BUILD BASE TIME: " + remoteDiffBuildBaseUTC + " local BUILD TIME:" + buildutc);
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
		if (this.mListener != null ) {
			this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_CHECKED, error, null);
	                Log.v(TAG, "---------state in checked----------- ");
                }
        }
	
	void reportDownloadError(int error) {
		if (this.mListener != null)
			this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_DOWNLOADING, error, null);
	}
	
	void reportInstallError(int error) {
		if (this.mListener != null) {
			this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_UPGRADING, error, null);
                        Log.v(TAG, "---------state in upgrading----------- "); 
                }   
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

		if (ab_slot()) {
			if (getUpdateHeader(mConfig.getPayloadPropertiesURL()) == false) {
				reportCheckingError(OTAStateChangeListener.ERROR_CANNOT_FIND_SERVER);
									Log.v(TAG, "error cannot find server!");
				return;
			}

			mWakelock.acquire();
			mUpdateEngine.applyPayload(mConfig.getPackageURL().toString(), 0l, 0l, mUpdateHeader);
			mWakelock.release();
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
		if (ab_slot()) {
			if (mListener != null) {
				mListener.onStateOrProgress(OTAStateChangeListener.MESSAGE_WAIT_REBOOT, 0, null);
			}
			return;
		}

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

	public boolean ab_slot() {
		return mConfig.ab_slot();
	}

	public void setDiffUpgrade() {
		mConfig.setDiffUpgrade();
	}
}

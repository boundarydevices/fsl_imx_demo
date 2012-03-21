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
import java.net.MalformedURLException;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import com.fsl.android.ota.R;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;


// Controller of OTA Activity
public class OtaAppActivity extends Activity implements OTAServerManager.OTAStateChangeListener {
	
	Button mUpgradeButton;
	TextView mMessageTextView;
	TextView mVersionTextView;
	ProgressBar mSpinner;
	ProgressBar mDownloadProgress;
	
	Context mContext; 
	
	OTAServerManager mOTAManager;
	int mState = 0;
	
	/* state change will be 0 -> Checked -> Downloading -> upgrading.  */
	
	final String TAG = "OTA";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "OTAAppActivity : onCreate");
        setContentView(R.layout.main);
        (mUpgradeButton = (Button) findViewById(R.id.upgrade_button)) 
        		.setOnClickListener(mUpgradeListener);
        mMessageTextView = (TextView) findViewById(R.id.message_text_view);
        mVersionTextView = ((TextView) findViewById(R.id.version_text_view)); 
        mSpinner = (ProgressBar) findViewById(R.id.spinner);
        mDownloadProgress = (ProgressBar) findViewById(R.id.download_progress_bar);
        mContext = getBaseContext();
        
        try {
			mOTAManager = new OTAServerManager(mContext);
		} catch (MalformedURLException e) {
			mOTAManager = null;
			Log.e(TAG, "meet not a mailformat URL... should not happens.");
			e.printStackTrace();
		}
        mOTAManager.setmListener(this);

    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
    	Log.d(TAG, "OTAAppActivity : onStart");
    	// default state is checking, if resume from any pervious state,
    	// resume the state
    	onStateChangeUI(mState);

    	if (mState == 0) {
        	new Thread(new Runnable() {
        		public void run() {
        			mOTAManager.startCheckingVersion();		
        		}
        	}).start();
        }
    }
    
    @Override
    public void onRestart() {
    	super.onRestart();
    	Log.d(TAG, "OTAAppActivity : onRestart");
    }
    
    
    @Override
    public void onPause() 
    {
    	super.onPause();
    	Log.d(TAG, "OTAAppActivity : onPause");
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	mOTAManager.onStop();
    	Log.d(TAG, "OTAAppActivity : onStop");
    }

    OnClickListener mUpgradeListener = new OnClickListener() {
		public void onClick(View v) {
			Log.v(TAG, "upgrade button clicked.");
			new Thread(new Runnable() {
				public void run() {
					mOTAManager.startDownloadUpgradePackage();
				}
			}).start();
			onStateChangeUI(STATE_IN_DOWNLOADING);
		}
		
	};
	
	public void onStateOrProgress(int message, int error, Object info)
	{
		Log.v(TAG, "onStateOrProgress: " + "message: " + message + " error:" + error + " info: " + info );
		switch (message) {
		case STATE_IN_CHECKED:
			onStateChangeUI(message);
			onStateInChecked(error, info);
			break;
		case STATE_IN_DOWNLOADING:
			onStateChangeUI(message);
			mState = STATE_IN_DOWNLOADING;
			onStateDownload(error, info);
			break;
		case STATE_IN_UPGRADING:
			onStateChangeUI(message);
			mState = STATE_IN_UPGRADING;
			onStateUpgrade(error, info);
			break;
		case MESSAGE_DOWNLOAD_PROGRESS:
		case MESSAGE_VERIFY_PROGRESS:
			onProgress(message, error, info);
			break;
		}
	}

	// this state change function only change
	// Attributes of UI elements
	// other will control the model(download). 
	void onStateChangeUI(int newstate)
	{
		mState = newstate;
		if (newstate == STATE_IN_IDLE) {
			   mVersionTextView.setVisibility(View.INVISIBLE);
			   mDownloadProgress.setVisibility(View.INVISIBLE);
			   mUpgradeButton.setVisibility(View.INVISIBLE);
		}
		else if (newstate == STATE_IN_CHECKED) {
			mVersionTextView.post(new Runnable() {
				public void run() {
					mVersionTextView.setVisibility(View.VISIBLE);
				}
			});
		} else if (newstate == STATE_IN_DOWNLOADING) {
			// from start download, it start hide the version again.
			mVersionTextView.post(new Runnable() {
				public void run() {
					mVersionTextView.setVisibility(View.INVISIBLE);
					mUpgradeButton.setVisibility(View.INVISIBLE);
					mSpinner.setVisibility(View.INVISIBLE);
					mDownloadProgress.setVisibility(View.VISIBLE);
				}
			});
		} 
	}
	
	void onStateUpgrade(int error, Object info) {
		
		if (error == ERROR_PACKAGE_VERIFY_FAILED) {
			Log.v(TAG, "package verify failed, signaure not match");
			mMessageTextView.post(new Runnable() {
				public void run() {
					mMessageTextView.setText(getText(R.string.error_package_verify_failed));
				}
			});
			// meet error in Verify, fall back to check.
			// TODO which state should ?
		} else if (error == ERROR_PACKAGE_INSTALL_FAILED) {
			mMessageTextView.post(new Runnable() {
				public void run() {
					mMessageTextView.setText(getText(R.string.error_package_install_failed));
				}
			});
		}
	}
	
	void onProgress(int message, int error, Object info) {
		final Long progress = new Long((Long)info); 
		mDownloadProgress.post(new Runnable() {
			public void run() {
				mDownloadProgress.setProgress(progress.intValue());
			}
		});
		
		Log.v(TAG, "progress : " + progress);
		if (message == MESSAGE_DOWNLOAD_PROGRESS) {
			onStateChangeUI(STATE_IN_DOWNLOADING);
			mMessageTextView.post(new Runnable() {
				
				public void run() {
					mMessageTextView.setText(getText(R.string.download_upgrade_package));
				}
			});
		} else if (message == MESSAGE_VERIFY_PROGRESS) {
			onStateChangeUI(STATE_IN_UPGRADING);
			mMessageTextView.post(new Runnable() {
				public void run() {
					mMessageTextView.setText(getText(R.string.verify_package));
				}
			});
		}
	}
	
	void onStateDownload(int error, Object info) {
		if (error == ERROR_CANNOT_FIND_SERVER) {
			// in this case, the build.prop already found but the server don't have upgrade package
			// report as "Server Error: Not have upgrade package";
			mMessageTextView.post(new Runnable() {
				
				public void run() {
					mMessageTextView.setText(getText(R.string.error_server_no_package));
				}
			});
		} else if (error == ERROR_WRITE_FILE_ERROR) {
			mMessageTextView.post(new Runnable() {
				
				public void run() {
					mMessageTextView.setText(getText(R.string.error_write_file));
					mUpgradeButton.setVisibility(View.VISIBLE);
				}
			});
			onStateChangeUI(STATE_IN_CHECKED);
		}
		
		if (error == 0) {
			// success download, let try to start with install package...
			// we should already in another thread, no needs to create a thread.
			mOTAManager.startInstallUpgradePackage();
		}
	}
	
	public static String byteCountToDisplaySize(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "KMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	void onStateInChecked(int error, Object info) {
		
		mSpinner.post(new Runnable() {	
			public void run() {
				mSpinner.setVisibility(View.INVISIBLE);
			}
		});
		
		if (error == 0) {
			// return no error, usually means have a version info from remote server, release name is in @info
			// needs check here whether the local version is newer then remote version
			if (mOTAManager.compareLocalVersionToServer() == false) {
				// we are already latest...
				mMessageTextView.post(new Runnable() {
					public void run() {
						mMessageTextView.setText(Build.VERSION.RELEASE + ", " + Build.ID + "\n" +  getText(R.string.already_up_to_date));
						mVersionTextView.setVisibility(View.INVISIBLE);
					}
				});
				
			} else if (mOTAManager.compareLocalVersionToServer() == true ) {
				final BuildPropParser parser = (BuildPropParser) info;
				final long bytes = mOTAManager.getUpgradePackageSize();
				mMessageTextView.post(new Runnable() {
					public void run() {
						onStateChangeUI(STATE_IN_CHECKED);
						mMessageTextView.setText(getText(R.string.have_new));

						String length = (String) getText(R.string.length_unknown);
						
						if (bytes > 0)
							length = byteCountToDisplaySize(bytes, false);
						mVersionTextView.setText(getText(R.string.version) +  ":" +
								parser.getProp("ro.build.id") + "\n" +
								getText(R.string.full_version) + ":" +
								parser.getProp("ro.build.description") + "\n" +
								getText(R.string.size) + " " + length);
						mUpgradeButton.setVisibility(View.VISIBLE);
					}
				});
			}
		} else if (error == ERROR_WIFI_NOT_AVALIBLE) {
			mMessageTextView.post(new Runnable() {
				
				public void run() {
					mMessageTextView.setText(getText(R.string.error_needs_wifi));
				}
			});
		} else if (error == ERROR_CANNOT_FIND_SERVER) {
			mMessageTextView.post(new Runnable() {
				public void run() {
					mMessageTextView.setText(getText(R.string.error_cannot_connect_server));
				}
			});
		} else if (error == ERROR_WRITE_FILE_ERROR ) {
			mMessageTextView.post(new Runnable() {
				public void run() {
					mMessageTextView.setText(getText(R.string.error_write_file));
				}
			});
		}
	}
	
}
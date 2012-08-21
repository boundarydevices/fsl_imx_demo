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



package fsl.power.manager;


import fsl.power.manager.PowerServiceDB.Profiles;
import android.app.ListActivity;

import android.content.ContentResolver;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class powermanager extends ListActivity {
	private static final String TAG = "FSL_POWER";

	public static final String ACTION_POWER_OPTION = "fsl.power.service.action.START_SERVICE";

	private static final String KEY_PROFILE_ACTIVED = "activeid";

	private ContentResolver cr = null;
	private boolean DEBUG = false;

	//below define should align with profile service, if changed, please also
	// update them in profile service, otherwise, it may not work.
    private int PERFORMANCE = 1;
    private int POWERSAVING =2;
    private int NORMAL = 3;

    private int mCheckid = NORMAL -1; //normal mode


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
		this, R.array.profile_name, android.R.layout.simple_list_item_single_choice);
	final ListView listView = getListView();
	listView.setAdapter(adapter);

	/*
	 * acquire the sql to get the previous settings
	 */
	cr = getContentResolver();
	InitialPowerOption();

	listView.setItemsCanFocus(false);
	listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

	listView.setEnabled(true);
	if(DEBUG) Log.i(TAG,"select mode "+ mCheckid);
	listView.setSelection(mCheckid);
	listView.getCheckedItemIds();
	listView.isItemChecked(mCheckid);
	listView.setItemChecked(mCheckid, true);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
	super.onRestoreInstanceState(savedInstanceState);
	mCheckid = savedInstanceState.getInt(KEY_PROFILE_ACTIVED);
	Log.i(TAG, "onRestoreInstanceState() mCheckid: " + mCheckid);
	final ListView list = getListView();
	list.setSelection(mCheckid);


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
	super.onSaveInstanceState(outState);
	outState.putInt(KEY_PROFILE_ACTIVED, mCheckid);
    }


    protected void onListItemClick(ListView l, View v, int position, long id) {

	int check_pos = l.getCheckedItemPosition();
	mCheckid = check_pos;
	int profileid = NORMAL;
	switch (mCheckid){
		case 0:
			profileid = PERFORMANCE;
			break;
		case 1:
			profileid = POWERSAVING;
			break;
		default:
			profileid = NORMAL;
	}

	l.setEnabled(true);
	l.setSelection(mCheckid);

	Intent startservice = new Intent();
	startservice.putExtra("profile", profileid);
	startservice.setAction(ACTION_POWER_OPTION);
	sendBroadcast(startservice);

	if(DEBUG) Log.i(TAG,"position:  "+position+"   id:  "+id+"  check_pos: " + check_pos);

    }

    /*
     * acquire the sql to get the previous settings
     */
    private void  InitialPowerOption(){
	Cursor cur;
	cur = cr.query(Profiles.CONTENT_URI, null, Profiles.PfofileStatus + "="+"1", null, null);
	if (cur !=null){
		if (cur.getCount() > 0) {
			cur.moveToFirst();
			mCheckid = cur.getInt(cur.getColumnIndex(Profiles.ProfileID))-1;
		}
	}
	cur.close();
	cur = null;

    }

    public static void updatePowerOptions(int id) {

    }



}

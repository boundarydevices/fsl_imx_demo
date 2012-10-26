/*
 * Copyright (C) 2012 Freescale Semiconductor, Inc.
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



package fsl.power.service;

import java.util.HashMap;

import fsl.power.service.PowerServiceDB.Profiles;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * @author B33531
 *
 */
public class FSLPpowerProvider extends ContentProvider{

	private static final String TAG = "FSL_Provider";
	private static final boolean DEBUG = false;
	private static final String DATABASE_NAME = "profiles.db";
    private static final int DATABASE_VERSION = 1;
    /**
     * Table Name Define
     */
    private static final String PROFILE_TABLE_NAME = "profiles";

    /**
     * ProjectctionMap
     */

    private static HashMap<String, String> sProfilesProjectionMap;
    /**
     * Operation Code
     */
    private static final int PROFILES     = 1;
    private static final int PROFILES_ID  = 2;
    /**
     * Define the URI Matcher
     */
    private static final UriMatcher sUriMatcher;
    private DatabaseHelper mOpenHelper;

	 static {
	 /**
	  * URI Matcher
	  */
		 sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		 sUriMatcher.addURI(PowerServiceDB.AUTHORITY, "profiles", PROFILES);
		 sUriMatcher.addURI(PowerServiceDB.AUTHORITY, "profiles/#", PROFILES_ID);

		 /**
	  * ProjectMap List
	  * _ID	Profile_ID	ProfileName	PfofileStatus	TempHot TempActive	CurFreq	CurCPUGov	CPUHotPlug	CPUNM
	  * 1	1			Performance		1			80		60			996		interactive		true	4
	  */
       sProfilesProjectionMap = new HashMap<String, String>();
       sProfilesProjectionMap.put(Profiles._ID, Profiles._ID);
       sProfilesProjectionMap.put(Profiles.ProfileID, Profiles.ProfileID);
       sProfilesProjectionMap.put(Profiles.ProfileName, Profiles.ProfileName);
       sProfilesProjectionMap.put(Profiles.PfofileStatus, Profiles.PfofileStatus);
       sProfilesProjectionMap.put(Profiles.TempHot, Profiles.TempHot);
       sProfilesProjectionMap.put(Profiles.TempActive, Profiles.TempActive);
       sProfilesProjectionMap.put(Profiles.MaxFreq, Profiles.MaxFreq);
       sProfilesProjectionMap.put(Profiles.MinFreq, Profiles.MinFreq);
       sProfilesProjectionMap.put(Profiles.CurCPUGov, Profiles.CurCPUGov);
       sProfilesProjectionMap.put(Profiles.CPUHotPlug, Profiles.CPUHotPlug);
       sProfilesProjectionMap.put(Profiles.CPUNM, Profiles.CPUNM);
	 }

	public FSLPpowerProvider() {
		// TODO Auto-generated constructor stub
	}

	static class DatabaseHelper extends SQLiteOpenHelper {
		 DatabaseHelper(Context context) {
		    super(context, DATABASE_NAME, null, DATABASE_VERSION);
		    if(DEBUG) Log.i(TAG,"create profile DB");
		}
		 @Override
		 public void onCreate(SQLiteDatabase db) {
				/**
				 * Create the Profile Table
				 */
			 db.execSQL("CREATE TABLE " + PROFILE_TABLE_NAME + " ("
			    + Profiles._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			    + Profiles.ProfileID + " INTEGER  ,"
			    + Profiles.ProfileName + " TEXT,"
			    + Profiles.PfofileStatus + " INTEGER,"
			    + Profiles.TempHot+ " INTEGER,"
			    + Profiles.TempActive + " INTEGER,"
			    + Profiles.CurCPUGov + " TEXT,"
			    + Profiles.MaxFreq + " INTEGGER,"
			    + Profiles.MinFreq + " INTEGGER,"
			    + Profiles.CPUHotPlug + " INTERGER,"
			    + Profiles.CPUNM +" INTEGER"
			    + ");");
				if(DEBUG) Log.i(TAG,"ceate the profile table");
		 }

		 @Override
		 public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		    Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
			    + newVersion + ", which will destroy all old data");
		    db.execSQL("DROP TABLE IF EXISTS profiles");
		    onCreate(db);
		    if(DEBUG)Log.i(TAG,"onupgrade firewall DB");
	       }
	 }


	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		 SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		ContentValues values = new ContentValues();
		if(DEBUG) Log.i(TAG,"=========>delete()");
		switch (sUriMatcher.match(uri)) {
		case PROFILES:
		    if(where == null){
			//clear the relations table
			//db.delete(Relations_TABLE_NAME, where, null);
			//clear the reject table
			//db.delete(RejectMethods_TABLE_NAME, where, null);

		    }else{
			String[]keyOpcode =where.split("=");
			long profileId =0;
			if(keyOpcode[0].equals(Profiles._ID)){//SUPPORT FOR Profile._ID = profileId operation
				profileId = Long.parseLong(keyOpcode[1]);
				//rmRelationRowsAndRejectsToProfile(profileId);

			}else if (keyOpcode[0].equals(Profiles.ProfileName)) {//SUPPORT FOR Profile.ProfileName = profileName operation
						Cursor cur = db.query(PROFILE_TABLE_NAME, new String[]{Profiles._ID}, Profiles.ProfileName+"="+keyOpcode[1], null, null, null, null);
						cur.moveToFirst();
						profileId = cur.getLong(cur.getColumnIndex(Profiles._ID));
						cur.close();
						cur = null;
						//rmRelationRowsAndRejectsToProfile(profileId);
					}
		    }


			//clear the profile table
		    count = db.delete(PROFILE_TABLE_NAME, where, whereArgs);

		    break;
		case PROFILES_ID:
		    String profileId = uri.getPathSegments().get(1);
		    //rmRelationRowsAndRejectsToProfile(Long.parseLong(profileId));
		    //delete a profile which ID is profileId
		    count = db.delete(PROFILE_TABLE_NAME, Profiles._ID + "=" + profileId
			    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
		    break;

		default:
		    throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		Log.i(TAG,"------------>get Type");
		  switch (sUriMatcher.match(uri)) {
		case PROFILES:
			return Profiles.CONTENT_TYPE;
		case PROFILES_ID:
			return Profiles.CONTENT_ITEM_TYPE;
		default:
		    throw new IllegalArgumentException("Unknown URI " + uri);

		  }
	}

	@Override
	public Uri insert(Uri uri, ContentValues value) {
		// TODO Auto-generated method stub
		if (DEBUG) Log.i(TAG, "insert a cloum");
		ContentValues values;
		if (value != null)
			values = new ContentValues(value);
		else
			values = new ContentValues();

	    String strTable="";
	    String strColumnName="";
	    Uri CommonUri=null;
		switch (sUriMatcher.match(uri)){
		case PROFILES:
			strTable=PROFILE_TABLE_NAME;
		strColumnName=Profiles.ProfileID;
		CommonUri =PowerServiceDB.Profiles.CONTENT_URI;
		if (values.containsKey(Profiles.ProfileName)== false)
			values.put(Profiles.ProfileName, "");
		if (values.containsKey(Profiles.CPUHotPlug)== false)
			values.put(Profiles.CPUHotPlug, 0);
		if (values.containsKey(Profiles.CPUNM)== false)
			values.put(Profiles.CPUNM, 3);
		if (values.containsKey(Profiles.CurCPUGov)== false)
			values.put(Profiles.CurCPUGov, "interactive");
		if (values.containsKey(Profiles.MaxFreq)== false)
			values.put(Profiles.MaxFreq, 996);
		if (values.containsKey(Profiles.MinFreq)== false)
			values.put(Profiles.MinFreq, 198);
		if (values.containsKey(Profiles.PfofileStatus)== false)
			values.put(Profiles.PfofileStatus, "0");
		if (values.containsKey(Profiles.TempActive)== false)
			values.put(Profiles.TempActive, "60");
		if (values.containsKey(Profiles.TempHot)== false)
			values.put(Profiles.TempHot, 80);

		break;
		default:
			throw new IllegalArgumentException("unknow URI "+uri);


		}
		 SQLiteDatabase db = mOpenHelper.getWritableDatabase();

	 long rowId = db.insert(strTable, strColumnName, values);

	 if (rowId > 0) {
		Uri insertUri = ContentUris.withAppendedId(CommonUri, rowId);
	     getContext().getContentResolver().notifyChange(insertUri, null);
	     if (DEBUG) Log.i(TAG, "insert a cloum done");
	     return insertUri;
	 }
	 throw new SQLException("Failed to insert row into " + uri);

	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
	mOpenHelper = new DatabaseHelper(getContext());
	if (mOpenHelper==null) {
		if (DEBUG) Log.i(TAG,">faild in onCreate");
		return false;
	}
	if (DEBUG) Log.i(TAG,"Create scucess");
	return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
		   String sortOrder) {

		Log.i("TAG", "query");
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String defaultOrder="";
		switch (sUriMatcher.match(uri)) {
	case PROFILES:
	    qb.setTables(PROFILE_TABLE_NAME);
	    qb.setProjectionMap(sProfilesProjectionMap);
	    defaultOrder=PowerServiceDB.Profiles.DEFAULT_SORT_ORDER;
	    if(DEBUG) Log.i(TAG,"=========>query profile table");
	    break;
	case PROFILES_ID:
	    qb.setTables(PROFILE_TABLE_NAME);
	    qb.setProjectionMap(sProfilesProjectionMap);
	    qb.appendWhere(Profiles._ID + "=" + uri.getPathSegments().get(1));
	    //qb.appendWhere(Profiles.ProfileID + "=" + uri.getPathSegments().get(1));
	    defaultOrder=PowerServiceDB.Profiles.DEFAULT_SORT_ORDER;
	    if(DEBUG) Log.i(TAG,"=========>query profile table item");
	    break;
	default:
	    throw new IllegalArgumentException("Unknown URI " + uri);
	}
		// If no sort order is specified use the default
	String orderBy;
	if (TextUtils.isEmpty(sortOrder)) {
	    orderBy = defaultOrder;
	} else {
	    orderBy = sortOrder;
	}
	// Get the database and run the query
	SQLiteDatabase db = mOpenHelper.getReadableDatabase();
	Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

	// Tell the cursor what uri to watch, so it knows when its source data changes
	c.setNotificationUri(getContext().getContentResolver(), uri);
	return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		 SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		if (DEBUG) Log.i(TAG,"=========>updata()");
		switch (sUriMatcher.match(uri)) {
		case PROFILES:
		    count = db.update(PROFILE_TABLE_NAME, values, where, whereArgs);
		    break;
		case PROFILES_ID:
		    String profileId = uri.getPathSegments().get(1);
		    int status = 0;
		    if(values.getAsInteger(Profiles.PfofileStatus)!=null){
			status = values.getAsInteger(Profiles.PfofileStatus);
		    }
		    if(status == 1){
			//update the other profile of which status =1 and set the status =0
				ContentValues valuestmp = new ContentValues();
				valuestmp.clear();
				valuestmp.put(Profiles.PfofileStatus,0);
				count = db.update(PROFILE_TABLE_NAME, valuestmp, Profiles.PfofileStatus+"="+"1", whereArgs);
				valuestmp.clear();
				valuestmp = null;
		    }

		    count = db.update(PROFILE_TABLE_NAME, values, Profiles._ID + "=" + profileId
			    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);

		    break;
		default:
		    throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;

	}




}

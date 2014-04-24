/*
 *  Copyright 2014 Calq.io
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is 
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing permissions and limitations under the 
 *  License.
 *  
 */

package io.calq.android.analytics;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Data store for saving API calls which haven't been sent yet. This allows us to queue failed API
 * calls and replay calls later if there is an issue (the most common being no network signal to
 * actually send the data.
 */
public class ApiDataStore extends SQLiteOpenHelper {
	
    /**
     * Database version for current code base.
     */
    private static final int DATABASE_VERSION = 1;
 
    /**
     * Name of the DB.
     */
    private static final String DATABASE_NAME = "api_queue";
 
    /**
     * Table name for actual queue.
     */
    private static final String TABLE_QUEUE = "api_queue";
    
    /**
     * Creates a new instance of the handler. This handler is thread safe.
     * 
     * @param context 		The context to use.
     */
	public ApiDataStore(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	// Database schema

	/**
	 * Handles when the DB is created for the first time.
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		String create = 
			 "CREATE TABLE " + TABLE_QUEUE + "(" +
				 "id INTEGER PRIMARY KEY," +
				 "write_key VARCHAR(32)," +
				 "endpoint VARCHAR(64)," +
				 "payload TEXT" +
			 ")";
		db.execSQL(create);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// No upgrades at present
	}
	
	// CRUD operations
	
	/**
	 * Adds the given API call to the queue.
	 * 
	 * @param apiCall		The call to add to the queue.
	 */
	public boolean addToQueue(AbstractAnalyticsApiCall apiCall) {
	    ContentValues values = new ContentValues();
	    values.put("write_key", apiCall.getWriteKey());
	    values.put("endpoint", apiCall.getApiEndpoint());
	    values.put("payload", apiCall.getPayload());
	 
	    SQLiteDatabase db = getWritableDatabase();
	    boolean success = db.insert(TABLE_QUEUE, null, values) != -1;
	    db.close();
	    
	    return success;
	}
	
	/**
	 * Gets the next API message from the queue (doesn't remove from queue).
	 * 
	 * @param writeKey		The writeKey to peek for queued calls for.
	 */
	public QueuedApiCall peekQueue(String writeKey) {
		SQLiteDatabase db = getReadableDatabase();
	    Cursor cursor = db.query(TABLE_QUEUE, /* All columns */ null,
	    		"write_key = ?", new String[] { writeKey }, null, null, null, /* LIMIT */ "1");
	    
	    QueuedApiCall result = null;
	    if (cursor != null && cursor.moveToFirst()) {
	    	result = new QueuedApiCall(
	    			cursor.getLong(cursor.getColumnIndex("id")),
	    			cursor.getString(cursor.getColumnIndex("endpoint")),
	    			cursor.getString(cursor.getColumnIndex("payload")),
	    			cursor.getString(cursor.getColumnIndex("write_key")));
	    }
	    return result;
	}
	
	/**
	 * Removes the given QueuedApiCall from the queue.
	 * 
	 * @param apiCall		The previously queued API call to remove.
	 */
	public boolean deleteFromQueue(QueuedApiCall apiCall) {
		SQLiteDatabase db = getReadableDatabase();
		boolean success = db.delete(TABLE_QUEUE, "id = ?", new String[] { String.valueOf(apiCall.getId()) }) == 1;
	    db.close();
	    return success;
	}

}

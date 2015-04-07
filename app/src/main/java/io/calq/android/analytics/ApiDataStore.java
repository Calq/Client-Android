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
import android.database.sqlite.SQLiteStatement;

import java.util.Vector;

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
     * Reference to DB once opened.
     */
    protected SQLiteDatabase db;

    /**
     * Compiled insert statement.
     */
    protected SQLiteStatement insert;

    /**
     * Creates a new instance of the handler. This handler is thread safe.
     * 
     * @param context 		The context to use.
     */
	public ApiDataStore(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

    @Override
    public SQLiteDatabase getWritableDatabase () {
        if(db == null || !db.isOpen()) {
            db = super.getWritableDatabase();
            insert = null;
        }
        return db;
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
        SQLiteDatabase db = getWritableDatabase();
        if(insert == null) {
            insert = db.compileStatement("INSERT INTO " + TABLE_QUEUE + " (write_key, endpoint, payload) VALUES (?, ?, ?)");
        }

        // Params are 1 indexed, not 0
        insert.bindString(1, apiCall.getWriteKey());
        insert.bindString(2, apiCall.getApiEndpoint());
        insert.bindString(3, apiCall.getPayload());

	    boolean success = insert.executeInsert() == 1;
	    
	    return success;
	}
	
	/**
	 * Gets the next API message from the queue (doesn't remove from queue).
	 * 
	 * @param writeKey		The writeKey to peek for queued calls for.
	 */
	public QueuedApiCall peekQueue(String writeKey) {
	    Cursor cursor = getWritableDatabase().query(TABLE_QUEUE, /* All columns */ null,
                "write_key = ?", new String[]{writeKey}, null, null,
	    		/* ORDER BY */ "id ASC", /* LIMIT */ "1");
	    
	    QueuedApiCall result = null;
	    if (cursor != null && cursor.moveToFirst()) {
	    	result = new QueuedApiCall(
	    			cursor.getLong(cursor.getColumnIndex("id")),
	    			cursor.getString(cursor.getColumnIndex("endpoint")),
	    			cursor.getString(cursor.getColumnIndex("payload")),
	    			cursor.getString(cursor.getColumnIndex("write_key")));
	    }
        cursor.close();
	    return result;
	}

    /**
     * Gets a sequence of API calls from the queue for the given endpoint. Does
     * not remove items from queue.
     *
     * @param writeKey		The writeKey to peek for queued calls for.
     * @param endPoint		The type of api call to get a batch for (based on end point).
     */
    public Vector<QueuedApiCall> getBatch(String writeKey, String endPoint) {
        Cursor cursor = getWritableDatabase().query(TABLE_QUEUE, /* All columns */ null,
                "write_key = ?", new String[]{writeKey}, null, null,
                /* ORDER BY */ "id ASC", /* LIMIT */ "100");

        Vector<QueuedApiCall> batch = new Vector<QueuedApiCall>();
        if (cursor != null && cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String callEndPoint = cursor.getString(cursor.getColumnIndex("endpoint"));
                if (!callEndPoint.equalsIgnoreCase(endPoint)) {
                    break;  // Different type, stop batch
                }
                batch.add(new QueuedApiCall(
                        cursor.getLong(cursor.getColumnIndex("id")),
                        cursor.getString(cursor.getColumnIndex("endpoint")),
                        cursor.getString(cursor.getColumnIndex("payload")),
                        cursor.getString(cursor.getColumnIndex("write_key"))));
                cursor.moveToNext();
            }
        }
        cursor.close();
        return batch;
    }
	
	/**
	 * Removes the given QueuedApiCall from the queue.
	 * 
	 * @param apiCall		The previously queued API call to remove.
	 */
	public boolean deleteFromQueue(QueuedApiCall apiCall) {
		boolean success = getWritableDatabase().delete(TABLE_QUEUE, "id = ?", new String[]{String.valueOf(apiCall.getId())}) == 1;
	    return success;
	}

    /**
     * Removes the given batch of calls from the queue.
     *
     * @param batch		The batch of API calls to remove.
     */
    public boolean deleteFromQueue(Vector<QueuedApiCall> batch) {
        // Build params for in clause
        StringBuilder builder = new StringBuilder();
        builder.append("id IN (?");
        for(int n = 1; n < batch.size(); n++) {
            builder.append(",?");
        }
        builder.append(")");

        // Build array of IN values
        String[] params = new String[batch.size()];
        for(int n = 0; n < batch.size(); n++) {
            params[n] = String.valueOf(batch.get(n).getId());
        }

        boolean success = getWritableDatabase().delete(TABLE_QUEUE, builder.toString(), params) == batch.size();
        return success;
    }

}

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

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Base class of all API calls.
 *
 */
public abstract class AbstractAnalyticsApiCall {
	
	private static final String TAG = "AbstractAnalyticsApiCal";    // Truncate for 23 letters
	
	/**
	 * The write key to use for this API call.
	 */
	protected String writeKey;
	
	/**
	 * The unique Id of the actor referenced by this API call.
	 */
	protected String actor;
	
	/**
	 * @param actor			The actor referenced by this API call.
	 * @param writeKey		The write key to use for this API call.
	 */
	public AbstractAnalyticsApiCall(String actor, String writeKey) {
		this.actor = actor;
		this.writeKey = writeKey;
	}
	
	/**
	 * Gets the name of the API endpoint that should be called (such as Track).
	 * @return the name of the endpoint to call.
	 */
	public abstract String getApiEndpoint();
	
	/**
	 * Builds a JSON payload describing this API call. This is what will ultimately be passed
	 * to the API server for this call.
	 * 
	 * @return a JSON payload to pass to the API server.
	 */
	protected JSONObject buildJSONPayload() {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put(ReservedApiProperties.ACTOR, actor);
			jsonObject.put(ReservedApiProperties.WRITE_KEY, writeKey);
		} catch (JSONException e) {
			// This shouldn't be happening! Should be sanitized already
			Log.e(TAG, "Error setting JSON values", e);
		}
		return jsonObject;
	}
	
	/**
	 * Returns a JSON payload in string form to be sent to the API server for this call.
	 */
	public String getPayload() {
		return buildJSONPayload().toString();
	}
	
	/**
	 * Returns the write key used by this call.
	 */
	public String getWriteKey() {
		return writeKey;
	}

}

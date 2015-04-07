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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;


public class ActionApiCall extends AbstractAnalyticsApiCall {
	
	private static final String TAG = "ActionApiCall";

    public static final String ENDPOINT = "Track";
	
	/**
	 * The date time of when this call was created (wrapped as UTC unix epoch offset).
	 */
	protected Date createdAt;
	
	/**
	 * The action being performed.
	 */
	protected String action;
	
	/**
	 * The custom properties sent with this action.
	 */
	protected  Map<String, Object> properties;

	/**
	 * Creates a new ActionApiCall describing an action. This will be passed to the
	 * /track/ API endpoint.
	 * 
	 * @param actor				The actor performing this action.
	 * @param action			The action being performed.
	 * @param properties		Any custom properties related to this action. Can be empty, but not null.
	 * @param writeKey			The write key to use for this API call.
	 */
	public ActionApiCall(String actor, String action, Map<String, Object> properties, String writeKey) {
		super(actor, writeKey);
		
		if(properties == null) {
			throw(new IllegalArgumentException("A properties value must be passed to the ActionApiCall ctor. Can be empty."));
		}
		this.properties = properties;
		this.action = action;
		
		createdAt = new Date();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getApiEndpoint() {
		return ENDPOINT;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JSONObject buildJSONPayload() {
		JSONObject jsonObject = super.buildJSONPayload();
		try {
			// Get date formatted as UTC
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);
			df.setTimeZone(TimeZone.getTimeZone("gmt"));
			String gmtTime = df.format(createdAt);
			
			// Build properties into object
			JSONObject jsonPropeties = new JSONObject();
			for(Entry<String, Object> entry : properties.entrySet()) {
				jsonPropeties.put(entry.getKey(), entry.getValue());
			}
			
			jsonObject.put(ReservedApiProperties.TIMESTAMP, gmtTime);
			jsonObject.put(ReservedApiProperties.ACTION_NAME, action);
			jsonObject.put(ReservedApiProperties.USER_PROPERTIES, jsonPropeties);
		} catch (JSONException e) {
			// This shouldn't be happening! Should be sanitized already
			Log.e(TAG, "Error setting JSON values", e);
		}
		return jsonObject;
	}

	

}

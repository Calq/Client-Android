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


public class TransferApiCall extends AbstractAnalyticsApiCall {
	
	private static final String TAG = "TransferApiCall";

	/**
	 * The new actor name.
	 */
	protected String newActor;
	
	/**
	 * Creates a new ActionApiCall describing an action. This will be passed to the
	 * /transfer/ API endpoint.
	 * 
	 * @param oldActor			The former actor name.
	 * @param newActor			The new actor name
	 * @param writeKey			The write key to use for this API call.
	 */
	public TransferApiCall(String oldActor, String newActor, String writeKey) {
		super(oldActor, writeKey);
		this.newActor = newActor; 
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getApiEndpoint() {
		return "Transfer";
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JSONObject buildJSONPayload() {
		JSONObject jsonObject = super.buildJSONPayload();
		try {
			
			jsonObject.put(ReservedApiProperties.OLD_ACTOR, actor);
			jsonObject.put(ReservedApiProperties.NEW_ACTOR, newActor);
		} catch (JSONException e) {
			// This shouldn't be happening! Should be sanitized already
			Log.e(TAG, "Error setting JSON values", e);
		}
		return jsonObject;
	}

}

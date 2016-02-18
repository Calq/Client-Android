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
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * This is a special type of API call used for persisting calls to storage. At this
 * point we no longer care what kind of call it is. We just care about the endpoint
 * and payload - enough to actually send the call to the API server.
 */
public class QueuedApiCall extends AbstractAnalyticsApiCall {
	
	/**
	 * The endpoint this api call should use.
	 */
	protected String endpoint;
	
	/**
	 * The payload this call will issue against the API server.
	 */
	protected String payload;
	
	/**
	 * The id of this queued call (in local storage).
	 */
	protected long id;
	
	/**
	 * Creates a instance of an API call which has been previously queued.
	 * 
	 * @param id			The id of this queued call (in local storage).
	 * @param endpoint		The endpoint this api call should use.
	 * @param payload		The JSON payload of the call.
	 * @param writeKey		The write key used.
	 */
	public QueuedApiCall(long id, String endpoint, String payload, String writeKey) {
		super(null, writeKey);
		
		this.id = id;
		this.endpoint = endpoint;
		this.payload = payload;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getApiEndpoint() {
		return endpoint;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPayload() {
        // This was stored and now is going to be sent. Need to get the time stamp for now
        try {
            JSONObject json = new JSONObject(payload);

            // Get date formatted as UTC
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            String utcTime = df.format(new Date());

            // Inject time into payload
            json.put(ReservedApiProperties.UTC_NOW, utcTime);
            return json.toString();
        } catch(JSONException e) {
            // This shouldn't be happening. Just fall back and pass on to API server as is
            return payload;
        }
	}
	
	/**
	 * Gets the local storage Id for this queued call.
	 */
	public long getId() {
		return id;
	}

}

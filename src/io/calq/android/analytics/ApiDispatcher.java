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

import io.calq.android.ApiException;
import io.calq.android.LocalConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is the class that actually dispatches API calls to the Calq API server.
 */
public class ApiDispatcher {
	
	/**
	 * Config settings being used.
	 */
	protected LocalConfig config;

	/**
	 * Creates a new ApiDispatcher for sending QueuedApiCalls to the Calq server.
	 * 
	 * @param config	The local config settings to use.
	 */
	public ApiDispatcher(LocalConfig config) {
		this.config = config;
	}
	
	/**
	 * Dispatches the given API call to the remote Calq server.
	 * @param apiCall			The api call to dispatch.
	 * @throws ApiException 
	 * @returns if this was successful.
	 */
	public boolean dispatch(QueuedApiCall apiCall) throws ApiException {
		
		// At some point we should switch from the Apache client to HttpURLConnection on clients that support it.
		//	See: http://android-developers.blogspot.co.uk/2011/09/androids-http-clients.html
		
		try {
			HttpClient httpclient = new DefaultHttpClient();
			
			HttpPost post = new HttpPost(getEndpointUrl(apiCall));
			post.setHeader("Content-type", "application/json");
			post.setEntity(new StringEntity(apiCall.getPayload()));
			HttpResponse response = httpclient.execute(post);
			
		    int statusCode = response.getStatusLine().getStatusCode();
		    if(statusCode >= 500) { 
		    	// 500s we want to retry later
	        	return false;
	        } else if(statusCode != HttpStatus.SC_OK) {
			
			    // Try get response for other codes, might have API error in it
			    HttpEntity entity = response.getEntity();
			    if(entity != null) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					out.close();
					String responseString = out.toString();
					
					JSONObject json = new JSONObject(responseString);
					if(json.has("error")) {
						throw(new ApiException(json.getString("error")));
					} else {
						throw(new ApiException("Unknown error occured during API call."));
					}
			    }
	        }
			
		} catch (ClientProtocolException e) {
			// Failed, but don't know why. Signal failed for re-queue
    		return false;
		} catch (IOException e) {
			// Failed, but don't know why. Signal failed for re-queue
    		return false;
		} catch (JSONException e) {
			// Failed, but don't know why. Signal failed for re-queue
			return false;
		}
	    
	    // All OK. This call can be removed from local queue
	    return true;
	}
	
	/**
	 * Gets the endpoint URL to use for the given API call.
	 */
	private String getEndpointUrl(QueuedApiCall apiCall) {
		String apiServerUrl = config.getRemoteApiServerUrl();
		return apiServerUrl + (apiServerUrl.endsWith("/") ? "" : "/") + apiCall.endpoint;
	}

}

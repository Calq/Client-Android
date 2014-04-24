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

package io.calq.android;

import io.calq.android.analytics.AbstractAnalyticsApiCall;
import io.calq.android.analytics.ActionApiCall;
import io.calq.android.analytics.ApiHandler;
import io.calq.android.analytics.ProfileApiCall;
import io.calq.android.analytics.ReservedActionProperties;
import io.calq.android.analytics.TransferApiCall;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Message;
import android.view.Display;
import android.view.WindowManager;

/**
 * Main class for interacting with the Calq API.
 * 
 * <p>Full guide and reference documentation is available on the Calq website at:
 * https://www.calq.io/docs/client/android
 * 
 * <p>Call {@link #getOrCreateClient(String)} to initialize a new CalqClient for the current
 * session. This will automatically load previous user properties if any exist.
 * 
 * <p>Once you have a client you can send actions to Calq using the {@link #track(String, Map)}
 * and {@link #trackSale(String, Map, String, BigDecimal)} methods.
 * 
 * <p>In order to communicate with the Calq API servers your application will need to have
 * been granted the <tt>android.permission.INTERNET</tt> permission.
 * 
 * <p>This client is designed to be thread safe. You will normally have a single instance of
 * a CalqClient for the current user and share it throughout your application.
 * 
 * @author Calq.io
 */
public class CalqClient {
	
	/**
	 * State store used to save client state for this client.
	 */
	protected ClientStateStore stateStore;
	
	// Note: Remember that any fields here that should be persisted between sessions will
	//	need to be loaded and persisted by appropriate code in ClientStateStore.

	/**
	 * The unique Id of the actor used by this instance.
	 */
	String actor;
	
	/**
	 * If this client is anonymous or not.
	 */
	Boolean isAnon;
	
	/**
	 * If this client has sent an action before.
	 */
	Boolean hasTracked;
	
	/**
	 * Map of global properties for this session.
	 */
	Map<String, Object> globalProperties;
	
	/**
	 * The ApiHandler we use to process API calls.
	 */
	protected ApiHandler apiHandler;
		
	/**
	 * The write key in use by this client.
	 */
	protected String writeKey;
	
	/**
	 * Lock used for synchronization.
	 */
	private static Object lock = new Object();
	
	/**
	 * Singletons per write key.
	 */
	protected static Map<String, CalqClient> clients;	// Global state, but makes it much easier for our users to log events
	
	// Creating clients
	
	/**
	 * Creates a new CalqClient instance directly by specifying a unique actor Id and the
	 * write key to communicate with the API server. Typically you would not call this directly
	 * but use the {@link #getOrCreateClient(Context String)} method instead. If you call this 
	 * directly then you can use {@link #loadState()} to load any previous client state.
	 * 
	 * @param applicationContext	An application context to use with this client.
	 * @param actor			The unique Id of the actor represented by this client.
	 * @param writeKey		The write key to use when communicating with the API.
	 * @param config		The configuration to use for this client (will fetch from context if null)
	 */
	public CalqClient(Context applicationContext, String actor, String writeKey, LocalConfig config) {
		if(actor == null || actor.length() == 0) {
			throw(new IllegalArgumentException("The 'actor' parameter can not be null or empty"));
		}
		if(writeKey == null || writeKey.length() == 0) {
			throw(new IllegalArgumentException("The 'writeKey' parameter can not be null or empty"));
		}
		
		if(applicationContext.checkCallingOrSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
			throw(new IllegalStateException("CalqClient requires INTERNET permission"));
		}
		
		this.actor = actor;
		this.writeKey = writeKey;
		
		stateStore = new ClientStateStore(applicationContext);
		globalProperties = new Hashtable<String, Object>();
		isAnon = true;
		hasTracked = false;
		
		if(config == null) {
			config = LocalConfig.getInstance(applicationContext);
		}
		apiHandler = ApiHandler.getHandlerForKey(applicationContext, writeKey, config);
	}
		
	/**
	 * Attempts to create a CalqClient from previously saved data (includes identity and
	 * super properties). If no previous data is found then a new client will be created.
	 * 
	 * <p>This method stores the client as a singleton and can be used to fetch clients
	 * around the app without re-creating them. Alternatively you can pass your CalqClient
	 * instance around.
	 * 
	 * @param applicationContext	The application context to use.
	 * @param writeKey		The write key to use when communicating with the API.
	 * @param config		The configuration to use for this client (can be null).
	 * @return a new CalqClient instance either populated with the previous session data
	 * 		 or with a new anonymous user Id (a blank session).
	 */
	public static CalqClient getOrCreateClient(Context applicationContext, String writeKey, LocalConfig config) {
		synchronized (lock) {
			if(clients == null) {
				clients = new Hashtable<String, CalqClient>();
			}
			CalqClient client = clients.get(writeKey);
			if(client == null) {
				// Creating new client
				String actor = CalqClient.generateAnonymousId();	// Id will get overwritten if we have previous state
				
				client = new CalqClient(applicationContext, actor, writeKey, config);
				client.loadState();
				client.populateDeviceInfo(applicationContext); // Overwrite device data every session (could have updated)
				client.persistState();
				
				clients.put(writeKey, client);
			}
			return client;
		}
	}
	
	/**
	 * Attempts to create a new CalqClient. with the given write key.
	 * @see CalqClient#getOrCreateClient(Context, String, LocalConfig)
	 * 
	 * @param applicationContext	The application context to use.
	 * @param writeKey		The write key to use when communicating with the API.
	 */
	public static CalqClient getOrCreateClient(Context applicationContext, String writeKey) {
		return CalqClient.getOrCreateClient(applicationContext, writeKey, null);
	}
		
	/**
	 * Attempts to create a new CalqClient. Will read write_key from manifest.
	 * @see #getOrCreateClient(Context, String)
	 * 
	 * <p>This is the standard way of creating CalqClient instances.
	 * 
	 * @param applicationContext	The application context to use.
	 */
	public static CalqClient getOrCreateClient(Context applicationContext) {
		LocalConfig config = LocalConfig.getInstance(applicationContext);
		String writeKey = config.getWriteKey();
		if(writeKey == null || writeKey.length() == 0) {
			throw(new RuntimeException("Unable to find Calq write_key in manifest (io.calq.android.config.writeKey)"));
		}
		return CalqClient.getOrCreateClient(applicationContext, writeKey, config);
	}
		
	// Client state persistence
	
	/**
	 * Populates the CalqClient with data saved from a previous session. Normally called
	 * automatically as part of {@link #getOrCreateClient(String)}
	 */
	public boolean loadState() {
		synchronized(lock) {
			return stateStore.loadState(this);
		}
	}
	
	/**
	 * Persists the current client state to storage. Called internally by methods that
	 * update client state.
	 */
	protected void persistState() {
		synchronized(lock) {
			stateStore.persistState(this);
		}
	}
	
	// API methods
	
	/**
	 * Tracks the given action.
	 * 
	 * <p>Calq performs analytics based on actions that you send it, and any custom data
	 * associated with that action. This call is the core of the analytics platform.
	 * 
	 * <p>All actions have an action name, and some optional data to send along with it.
	 * 
	 * <p>This method will pass data to a background worker and continue ASAP. It will not
	 * block whilst API calls to Calq servers are made.
	 * 
	 * @param action		The name of the action to track.
	 * @param properties	Any optional properties to include along with this action.
	 */
	public void track(String action, Map<String, Object> properties) {
		if(action == null || action.length() == 0) {
			throw(new IllegalArgumentException("The 'action' parameter can not be null or empty"));
		}
		if(properties == null) {
			properties = new Hashtable<String, Object>();
		}
		
		Map<String, Object> mergedProperties = new Hashtable<String, Object>();
		mergedProperties.putAll(globalProperties);
		mergedProperties.putAll(properties);
		
		callAnalyticsApi(new ActionApiCall(actor, action, mergedProperties, writeKey));
		
		if(!hasTracked) {
			hasTracked = true;
			persistState();
		}
	}
	
	/**
	 * Tracks the given action which has associated revenue.
	 * 
	 * @param action		The name of the action to track.
	 * @param properties	Any optional properties to include along with this action.
	 * @param currency		The 3 letter currency code for this sale (can be fictional).
	 * @param amount		The amount this sale is worth (can be negative for refunds).
	 */
	public void trackSale(String action, Map<String, Object> properties, String currency, BigDecimal amount) {
		if(properties == null) {
			properties = new Hashtable<String, Object>();
		}
		properties.put(ReservedActionProperties.SALE_CURRENCY, currency);
		properties.put(ReservedActionProperties.SALE_VALUE, amount);
		track(action, properties);
	}
	
	/**
	 * Sets a global property to be sent with all future actions when using 
	 * {@link #track(String, Map)}. Will be persisted to client for future. If a value
	 * has been already set then it will be overwritten.
	 * 
	 * @param property		The name of the property to set.
	 * @param value			The value of the new global property.
	 */
	public void setGlobalProperty(String property, Object value) {
		if(property == null || property.length() == 0) {
			throw(new IllegalArgumentException("The 'property' parameter can not be null or empty"));
		}
		if(value == null) {
			throw(new IllegalArgumentException("The 'value' parameter can not be null"));
		}
		
		globalProperties.put(property, value);
		persistState();
	}
	
	/**
	 * Sets the ID of this client to something else. This should be called if you register or
	 * sign-in a user and want to associate previously anonymous actions with this new identity.
	 * 
	 * <p>This should only be called once for a given user. Calling identify(...) again with a 
	 * different Id for the same user will result in an IllegalStateException being thrown.
	 * 
	 * @param actor			The new unique actor Id. 
	 */
	public void identify(String actor) {
		synchronized(lock) {
			if(!this.actor.equals(actor)) {
				if(!isAnon) {
					throw(new IllegalStateException("Identify has already been called for this actor."));
				}
				
				String oldActor = this.actor;
				this.actor = actor;
				
				if(hasTracked) {
					callAnalyticsApi(new TransferApiCall(oldActor, actor, writeKey));
				}
				
				isAnon = false;
				hasTracked = false;
				persistState();
			}
		}
	}
	
	/**
	 * Sets profile properties for the current user. These are not the same as global properties.
     * A user MUST be identified before calling profile else an IllegalStateException will be thrown.
     * 
	 * @param properties	The custom properties to set for this user. If a property with the
	 * 		same name already exists then it will be overwritten.
	 * @throws ApiException 
	 */
	public void profile(Map<String, Object> properties) {
		if (properties == null || properties.isEmpty()) {
            throw (new IllegalArgumentException("You must pass some information to Profile(...) (or else there isn't much point)"));
        }
        if(isAnon) {
            throw (new IllegalStateException("A client must be identified (call identify(...)) before calling profile(...)"));
        }

        callAnalyticsApi(new ProfileApiCall(actor, properties, writeKey));
	}
	
	/**
	 * Clears the current session and resets to being an anonymous user.
	 * You should generally call this if a user logs out of your application.
	 */
	public void clear() {
		synchronized (lock) {
			hasTracked = false;
			isAnon = true;
			actor = CalqClient.generateAnonymousId();
			globalProperties = new Hashtable<String, Object>();
			
			persistState();
		}
	}
	
	/**
	 * Passes the given API call information to the ApiHandler to process.
	 * @param call			The API call to process.
	 */
	protected void callAnalyticsApi(AbstractAnalyticsApiCall call) {
		Message msg = Message.obtain();
		msg.obj = call;
		msg.what = ApiHandler.MESSAGE_API;
		apiHandler.sendMessage(msg);
	}
	
	
	// Util methods
	
	/**
	 * Asks the CalqClient to flush any API calls which are currently queued. Normally this
	 * is done in the background for you (calls are grouped together to save battery).
	 * 
	 * <p>It is recommended you call this manually in your applications onDestroy handler.
	 * This will ensure that outstanding calls are sent before the app closes. If there is
	 * no network signal, they will be skipped and saved until the app is re-opened.
	 */
	public void flushQueue() { 
		apiHandler.forceFlush();
	}
	
	/**
	 * Populates the global properties for this session with default device information  that we generally
	 * want each time. This is called automatically as part of {@link #getOrCreateClient(Context, String)}.
	 * 
	 * @param context		Context used to get device info from.
	 */
	public void populateDeviceInfo(Context context) {
		// Resolution values
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		@SuppressWarnings("deprecation")
		int width = display.getWidth();  // Deprecated now, but we want 2.1 support
		@SuppressWarnings("deprecation")
		int height = display.getHeight();  // Deprecated now, but we want 2.1 support
		
		// Build suitable agent string. These should all be here, but we test just in case
		String osName = System.getProperty("os.name");
		String osVer = System.getProperty("os.version");
		String osArch = System.getProperty("os.arch");
		StringBuilder agent = new StringBuilder();
		if(osName != null) {
			agent.append(osName);
		}
		if(osVer != null) {
			if(agent.length() > 0) {
				agent.append(" ");	
			}
			agent.append(osVer);
		}
		if(osArch != null) {
			if(agent.length() > 0) {
				agent.append(" ");	
			}
			agent.append(osArch);
		}
		
		if(agent.length() > 0) {
			globalProperties.put(ReservedActionProperties.DEVICE_AGENT, agent.toString());
		}
		globalProperties.put(ReservedActionProperties.DEVICE_OS, "Android");
		globalProperties.put(ReservedActionProperties.DEVICE_RESOLUTION, Integer.toString(width) + "x" + Integer.toString(height));
		globalProperties.put(ReservedActionProperties.DEVICE_MOBILE, true);		
	}
	
	/**
	 * Generates a new anonymous Id to identify a user.
	 * 
	 * @return a new anonymous user Id.
	 */
	private static String generateAnonymousId() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}
	
}

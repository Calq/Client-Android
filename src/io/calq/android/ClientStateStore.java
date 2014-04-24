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

import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Class handling the persistence of client state to the local device. This
 * is used to store CalqClient properties between sessions (such as the user
 * id and any global properties).
 */
class ClientStateStore {
	
	/**
	 * The shared properties representing client state (actor, isAnon etc).
	 */
	private SharedPreferences statePreferences;
	
	/**
	 * The shared properties representing global properties.
	 */
	private SharedPreferences globalPropertyPreferences;
	
	/**
	 * Creates a new ClientStateStore from the given Context.
	 * 
	 * <p>ClientStateStore are used by CalqClient instances to persist state.
	 * This way global properties and other such data can persist between app
	 * sessions.
	 * 
	 * @param appContext		The application context used to access app storage.
	 * 		This should always be application context and not an activity context.
	 */
	public ClientStateStore(Context appContext) {
		statePreferences = appContext.getSharedPreferences(ClientStateStore.SHAREDPREFERENCES_STATE, Context.MODE_PRIVATE);
		globalPropertyPreferences = appContext.getSharedPreferences(ClientStateStore.SHAREDPREFERENCES_GLOBALPROPERTIES, Context.MODE_PRIVATE);
	}
	
	/**
	 * Loads client state for the given CalqClient instance.
	 * 
	 * @param client	The instance to load local state into.
	 */
	@SuppressWarnings("unchecked")
	public boolean loadState(CalqClient client) {
		if(!statePreferences.contains(ClientStateStore.STATEKEY_ACTOR)) {
			return false;	// No previous state
		}
		
		client.actor = statePreferences.getString(ClientStateStore.STATEKEY_ACTOR, client.actor);
		client.isAnon = statePreferences.getBoolean(ClientStateStore.STATEKEY_IS_ANON, client.isAnon);
		client.hasTracked = statePreferences.getBoolean(ClientStateStore.STATEKEY_HAS_TRACKED, client.hasTracked);
		
		client.globalProperties = (Map<String, Object>) globalPropertyPreferences.getAll();
		
		return true;
	}
	
	/**
	 * Persists the given client state to local storage.
	 * 
	 * @param client	The client to save state from.
	 */
	public void persistState(CalqClient client) {
		SharedPreferences.Editor editor = statePreferences.edit();
		editor.putString(ClientStateStore.STATEKEY_ACTOR, client.actor);
		editor.putBoolean(ClientStateStore.STATEKEY_IS_ANON, client.isAnon);
		editor.putBoolean(ClientStateStore.STATEKEY_HAS_TRACKED, client.hasTracked);
		editor.commit();
		
		// Store all global properties
		editor = globalPropertyPreferences.edit();
		for(Entry<String, Object> entry : client.globalProperties.entrySet()) {
		    editor.putString(entry.getKey(), entry.getValue().toString());
		}
		editor.commit();
	}
	
	/**
	 * Name of shared preferences holding general client state.
	 */
	private static final String SHAREDPREFERENCES_STATE = "calq_state";
	
	/**
	 * Name of shared preferences holding global properties.
	 */
	private static final String SHAREDPREFERENCES_GLOBALPROPERTIES = "calq_global_props";
	
	private static final String STATEKEY_ACTOR = "actor";
	private static final String STATEKEY_IS_ANON = "isAnon";
	private static final String STATEKEY_HAS_TRACKED = "hasTracked";

}
